#!/usr/bin/env python3
"""Rhythm package relocation tool — per-phase mechanical rehoming.

Moves one module's packages from the shared root `chromahub.rhythm.app.*` to its
unique root `com.qlzhj.rhythm.<gradle-path>.*`, rewriting every reference repo-wide
by resolving actual class ownership from the file tree (packages are duplicated
across modules today, so prefix-only rewriting is ambiguous).

Per-phase usage:
    relocate.py <module-dir> --check     # index + collision + ref census only
    relocate.py <module-dir> --dry-run   # rewrite report, no writes/moves
    relocate.py <module-dir>             # rewrite + git mv + empty-dir cleanup

Reference resolution rules:
  * package lines: only in the module's own files, via its strip rule
  * R / BuildConfig (generated, no source file): in module's own files,
    resolve to the module's own namespace
  * any other `chromahub.rhythm.app.<...>`: longest package prefix owned by the
    module; if the package is multi-owner, the next segment must be a file stem
    in the module's copy (stem collisions were verified to be zero repo-wide)
  * protocol literals (`.action.` / `.broadcast.` / `.extra.` segments or bare
    package const) never contain relocated-package segments, so they never match
"""
import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path("/home/qlzhj/项目/AndroidStudioProjects/Rhythm")
OLD_PREFIX = "chromahub.rhythm.app"
NEW_BASE = "com.qlzhj.rhythm"
SCAN_DIRS = ["app", "core", "feature", "macrobenchmark"]
GUARD_SEGMENTS = {"action", "broadcast", "extra"}  # protocol literal segments, never relocated packages

# module dir -> (new location segments under com/qlzhj/rhythm, strip pattern segments)
CONFIG = {
    "app":               (["app"],                  ["app"]),
    "core/common":       (["core", "common"],       ["core", "common"]),
    "core/model":        (["core", "model"],        ["core", "model"]),
    "core/designsystem": (["core", "designsystem"], ["core", "designsystem"]),
    "core/network":      (["core", "network"],      ["core", "network"]),
    "core/data":         (["core", "data"],         ["core", "data"]),
    "core/media":        (["core", "media"],        ["core", "media"]),
    "core/ui":           (["core", "ui"],           ["core", "ui"]),
    "feature/local":     (["feature", "local"],     ["features", "local"]),
    "feature/streaming": (["feature", "streaming"], ["features", "streaming"]),
    "feature/settings":  (["feature", "settings"],  ["features", "settings"]),
    "feature/widget":    (["feature", "widget"],    ["features", "widget"]),
}

OCC_RE = re.compile(r"chromahub\.rhythm\.app(?:\.[A-Za-z_][A-Za-z0-9_]*)+")
PKG_LINE_RE = re.compile(r"^(?:\ufeff)?package\s+chromahub\.rhythm\.app((?:\.[A-Za-z_][A-Za-z0-9_]*)*)\s*$")
R_RE = re.compile(r"chromahub\.rhythm\.app\.R\b")
BUILD_CONFIG_RE = re.compile(r"chromahub\.rhythm\.app\.BuildConfig\b")

# top-level declaration names (column-0 anchored): Kotlin top-level members are
# referenced as package.member without any file stem, so the multi-owner gate
# must know them, not just file stems
MODS = r"(?:(?:public|internal|private|protected|sealed|abstract|data|enum|open|value|inline|suspend|const|external|operator|infix|lateinit|override|final|expect|actual)\s+)*"
DECL_RE = re.compile(
    rf"^{MODS}(?:class|interface|object|typealias)\s+`?([A-Za-z_][A-Za-z0-9_]*)`?"
    rf"|^{MODS}(?:val|var)\s+`?([A-Za-z_][A-Za-z0-9_]*)`?"
    rf"|^{MODS}fun\s+(?:<[^>]*>\s*)?(?:[A-Za-z_][A-Za-z0-9_]*(?:<[^>]*>)?\s*\.\s*)?`?([A-Za-z_][A-Za-z0-9_]*)`?\s*\(",
    re.MULTILINE,
)


def decl_names(path: Path, include_private: bool = True):
    try:
        text = path.read_bytes().decode("utf-8")
    except UnicodeDecodeError:
        return set()
    out = set()
    for m in DECL_RE.finditer(text):
        if not include_private and re.match(r"\s*(?:private|protected)\b", m.group(0)):
            continue
        name = next(g for g in m.groups() if g)
        out.add(name)
    return out


def is_in_module(path: Path, module_dir: str) -> bool:
    try:
        return path.relative_to(ROOT / module_dir) is not None
    except ValueError:
        return False


def collect_kt_files():
    out = []
    for d in SCAN_DIRS:
        base = ROOT / d
        if not base.exists():
            continue
        for p in base.rglob("*.kt"):
            s = str(p)
            if "/build/" in s or "/.git/" in s:
                continue
            out.append(p)
    return out


def old_rest_segments(path: Path):
    """Return segment list after chromahub/rhythm/app, or None if file is not under the shared root."""
    parts = path.parts
    try:
        i = parts.index("chromahub")
        if len(parts) < i + 4 or parts[i + 1] != "rhythm" or parts[i + 2] != "app":
            return None
        return list(parts[i + 3 : -1])  # exclude filename
    except ValueError:
        return None


def strip_segments(segs, strip):
    """Strip a leading sequence equal to `strip`."""
    if len(segs) >= len(strip) and segs[: len(strip)] == strip:
        return segs[len(strip):]
    return list(segs)


def build_maps(module_dir, all_kt):
    mpath, strip = CONFIG[module_dir]
    new_mpath = NEW_BASE.split(".") + mpath

    # global package ownership across all configured modules + macrobenchmark
    owners = {}
    for p in all_kt:
        seg = old_rest_segments(p)
        if seg is None:
            continue
        # owner module: first configured module dir that is an ancestor;
        # macrobenchmark is tracked as an unconfigured sentinel so its packages
        # (benchmark.*) are never mistaken for the target module's
        owner = None
        for m in CONFIG:
            if is_in_module(p, m):
                owner = m
                break
        if owner is None and is_in_module(p, "macrobenchmark"):
            owner = "macrobenchmark"
        if owner is None:
            continue
        for i in range(len(seg) + 1):
            pkg = OLD_PREFIX if i == 0 else OLD_PREFIX + "." + ".".join(seg[:i])
            owners.setdefault(pkg, set()).add(owner)

    # target module maps: old pkg -> new pkg, and (pkg -> stems/decls) for its files
    m_pkgs = {}
    m_stems = {}
    m_decls = {}
    moves = []  # (old file Path, new Path)
    dest_seen = {}
    for p in all_kt:
        if not is_in_module(p, module_dir):
            continue
        seg = old_rest_segments(p)
        if seg is None:
            continue
        stripped = strip_segments(seg, strip)
        for i in range(len(seg) + 1):
            old_pkg = OLD_PREFIX if i == 0 else OLD_PREFIX + "." + ".".join(seg[:i])
            new_pkg = NEW_BASE + "." + ".".join(mpath)
            stripped_i = strip_segments(seg[:i], strip)
            if stripped_i:
                new_pkg += "." + ".".join(stripped_i)
            m_pkgs[old_pkg] = new_pkg
        m_stems.setdefault(OLD_PREFIX + ("." + ".".join(seg) if seg else ""), set()).add(p.stem)
        m_decls.setdefault(OLD_PREFIX + ("." + ".".join(seg) if seg else ""), set()).update(decl_names(p))
        src_parts = list(p.parts)
        ci = src_parts.index("chromahub")
        rel_tail = src_parts[ci + 3:]  # <rest...>/<Stem>.kt
        new_tail = new_mpath + strip_segments(rel_tail[:-1], strip) + [rel_tail[-1]]
        # module root = the directory that contained 'chromahub'
        mod_root = Path(*src_parts[:ci])
        dest = mod_root.joinpath(*new_tail)
        moves.append((p, dest))
        if dest in dest_seen:
            print(f"FATAL: destination collision: {dest}\n  from {dest_seen[dest]}\n  and  {p}")
            sys.exit(1)
        dest_seen[dest] = p
        if dest.exists():
            print(f"FATAL: destination exists on disk: {dest} (would overwrite {p})")
            sys.exit(1)
    return m_pkgs, m_stems, m_decls, owners, moves, new_mpath


def build_maps_residual(module_dir, all_kt):
    """Maps for an already-moved module: reverse the strip rule to reconstruct
    plausible old packages (strip applied / not applied), then register the
    module's current decls under each candidate. Ownership of a candidate =
    whoever still owns it in the current tree, plus this module (wildcard
    imports of single-owner candidates must resolve)."""
    mpath, strip = CONFIG[module_dir]
    m_pkgs, m_stems, m_decls = {}, {}, {}
    # current owners of old-tree packages (unmoved modules + macrobenchmark)
    owners = {}
    for p in all_kt:
        seg = old_rest_segments(p)
        if seg is None:
            continue
        owner = None
        for m in CONFIG:
            if is_in_module(p, m):
                owner = m
                break
        if owner is None and is_in_module(p, "macrobenchmark"):
            owner = "macrobenchmark"
        if owner is None:
            continue
        for i in range(len(seg) + 1):
            pkg = OLD_PREFIX if i == 0 else OLD_PREFIX + "." + ".".join(seg[:i])
            owners.setdefault(pkg, set()).add(owner)
    for p in all_kt:
        if not is_in_module(p, module_dir):
            continue
        parts = p.parts
        try:
            ri = parts.index("rhythm")
        except ValueError:
            continue
        base_len = ri + 1 + len(mpath)
        if len(parts) < base_len + 1 or parts[ri - 2] != "com" or parts[ri - 1] != "qlzhj":
            continue
        nr = list(parts[base_len:-1])
        new_pkg = NEW_BASE + "." + ".".join(mpath + nr)
        d = decl_names(p)
        stem = p.stem
        for cand in (list(strip) + nr, nr):
            old_pkg = OLD_PREFIX + ("." + ".".join(cand) if cand else "")
            m_pkgs[old_pkg] = new_pkg
            m_stems.setdefault(old_pkg, set()).add(stem)
            m_decls.setdefault(old_pkg, set()).update(d)
            owners.setdefault(old_pkg, set()).add(module_dir)
    return m_pkgs, m_stems, m_decls, owners, [], NEW_BASE.split(".") + mpath


def resolve(match_text, m_pkgs, m_stems, m_decls, owners):
    """Return (old_pkg_key, pkg_segments_j, stem_or_None) or None."""
    segs = match_text.split(".")[3:]
    n = len(segs)
    for j in range(n, -1, -1):
        pkg = OLD_PREFIX if j == 0 else OLD_PREFIX + "." + ".".join(segs[:j])
        if pkg not in m_pkgs:
            continue
        if owners.get(pkg, set()) == {CURRENT_MODULE}:
            return pkg, j, None
        # multi-owner: next segment must be a stem or top-level declaration of ours
        if j < n and segs[j] in (m_stems.get(pkg, set()) | m_decls.get(pkg, set())):
            return pkg, j, segs[j]
        return None  # ambiguous/foreign: do not fall back to shorter prefixes
    return None


CURRENT_MODULE = None


def rewrite_file(path, m_pkgs, m_stems, m_decls, owners, new_mpath, write=True):
    raw = path.read_bytes()
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        return None, ["<undecodable>"]
    lines = text.splitlines(keepends=True)
    out = []
    changed = 0
    unresolved = []
    own = is_in_module(path, CURRENT_MODULE)
    for line in lines:
        orig = line
        m = PKG_LINE_RE.match(line.rstrip("\r\n"))
        if m and own:
            segs = [s for s in m.group(1).split(".") if s]
            stripped = strip_segments(segs, CONFIG[CURRENT_MODULE][1])
            new_pkg = NEW_BASE + "." + ".".join(CONFIG[CURRENT_MODULE][0])
            if stripped:
                new_pkg += "." + ".".join(stripped)
            eol = line[len(line.rstrip("\r\n")):]
            bom = "\ufeff" if line.startswith("\ufeff") else ""
            line = f"{bom}package {new_pkg}" + eol
        else:
            # R / BuildConfig special rules for the module's own files
            if own:
                mroot = NEW_BASE + "." + ".".join(CONFIG[CURRENT_MODULE][0])
                line = R_RE.sub(mroot + ".R", line)
                line = BUILD_CONFIG_RE.sub(mroot + ".BuildConfig", line)
                # wildcard import of the module's OWN package: target is
                # unambiguous (reverse dependencies are impossible)
                wm = re.match(r"^import\s+(chromahub\.rhythm\.app(?:\.[A-Za-z_][\w.]*)?)\.\*\s*$", line.rstrip("\r\n"))
                if wm and wm.group(1) in m_pkgs:
                    eol = line[len(line.rstrip("\r\n")):]
                    line = f"import {m_pkgs[wm.group(1)]}.*" + eol
            # generic references
            def repl(mm):
                segs = mm.group(0).split(".")[3:]
                if any(s in GUARD_SEGMENTS for s in segs):
                    return mm.group(0)
                hit = resolve(mm.group(0), m_pkgs, m_stems, m_decls, owners)
                if hit is None:
                    return mm.group(0)
                old_pkg, j, stem = hit
                parts = [m_pkgs[old_pkg]]
                if stem is not None:
                    parts.append(stem)
                parts.extend(segs[(j + 1) if stem is not None else j:])
                return ".".join(parts)
            line = OCC_RE.sub(repl, line)
        if line != orig:
            changed += 1
        out.append(line)
    # unresolved census (post-rewrite): for eyeball only
    for ln in out:
        for mm in OCC_RE.finditer(ln):
            unresolved.append(mm.group(0))
    if changed == 0 and not any(u for u in unresolved):
        return False, unresolved
    new_text = "".join(out)
    if new_text != text and write:
        path.write_bytes(new_text.encode("utf-8"))
    return changed > 0 and new_text != text, unresolved


def git_mv(src: Path, dst: Path):
    dst.parent.mkdir(parents=True, exist_ok=True)
    r = subprocess.run(["git", "mv", str(src), str(dst)], cwd=ROOT, capture_output=True, text=True)
    if r.returncode != 0:
        # untracked fallback
        import shutil
        shutil.move(str(src), str(dst))
        return False
    return True


def prune_empty_dirs(module_dir):
    base = ROOT / module_dir
    leftovers = []
    for root_dir in sorted(base.rglob("chromahub"), reverse=True):
        if not root_dir.is_dir() or "/build/" in str(root_dir):
            continue
        # bottom-up rmdir of empty dirs only — never delete real content
        for p in sorted(root_dir.rglob("*"), reverse=True):
            if p.is_dir():
                try:
                    p.rmdir()
                except OSError:
                    leftovers.append(p)
        try:
            root_dir.rmdir()
        except OSError:
            leftovers.append(root_dir)
    if leftovers:
        print(f"WARNING: non-empty leftovers under {module_dir}:")
        for p in leftovers[:10]:
            print(f"  {p.relative_to(ROOT)}")


def main():
    global CURRENT_MODULE
    ap = argparse.ArgumentParser()
    ap.add_argument("module")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--residual", action="store_true",
                    help="re-run rewrite for an already-moved module (reverse-mapped old packages)")
    args = ap.parse_args()
    if args.module not in CONFIG:
        sys.exit(f"unknown module: {args.module}")
    CURRENT_MODULE = args.module

    all_kt = collect_kt_files()
    if args.residual:
        m_pkgs, m_stems, m_decls, owners, moves, new_mpath = build_maps_residual(args.module, all_kt)
        print(f"[{args.module} residual] old-pkg candidates: {len(m_pkgs)}")
    else:
        m_pkgs, m_stems, m_decls, owners, moves, new_mpath = build_maps(args.module, all_kt)
        print(f"[{args.module}] packages owned: {len(m_pkgs)}, files to move: {len(moves)}")

    if args.check:
        multi = {p: o for p, o in owners.items() if len(o) > 1 and p in m_pkgs}
        print(f"multi-owner packages shared by this module: {len(multi)}")
        for p in sorted(multi):
            print(f"  {p}  <- {sorted(o for o in owners[p])}")
        return

    rewritten_files = 0
    total_unresolved = {}
    do_write = not args.dry_run
    for p in all_kt:
        changed, unresolved = rewrite_file(p, m_pkgs, m_stems, m_decls, owners, new_mpath, write=do_write)
        if changed:
            rewritten_files += 1
        if unresolved and is_in_module(p, args.module):
            total_unresolved[str(p.relative_to(ROOT))] = unresolved[:8]
    print(f"rewritten files: {rewritten_files}")
    if total_unresolved:
        print("unresolved in module's own files (expected: refs to pending modules / literals):")
        for f, refs in list(total_unresolved.items())[:15]:
            print(f"  {f}: {refs}")

    if args.dry_run:
        print(f"would move {len(moves)} files; sample:")
        for src, dst in moves[:12]:
            print(f"  {src.relative_to(ROOT)}\n    -> {dst.relative_to(ROOT)}")
        return

    moved = 0
    for src, dst in moves:
        moved += 1 if git_mv(src, dst) else 0
    print(f"moved: {moved}/{len(moves)} (git mv; untracked fallback if any)")
    prune_empty_dirs(args.module)
    print("done")


if __name__ == "__main__":
    main()
