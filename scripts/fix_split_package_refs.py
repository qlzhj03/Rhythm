#!/usr/bin/env python3
"""Detect bare (import-less) references from unmoved files to symbols of an
already-moved module, where the two used to share a package name. Kotlin
resolves same-package references without imports, so splitting a package name
across modules breaks them silently. Inserts the needed import line."""
import re
import sys
from pathlib import Path

sys.path.insert(0, "/tmp/rhythm-relocate")
import relocate as R  # noqa: E402


def main():
    module = sys.argv[1]
    apply = "--apply" in sys.argv
    R.CURRENT_MODULE = module
    all_kt = R.collect_kt_files()
    m_pkgs, m_stems, m_decls, _, _, _ = R.build_maps_residual(module, all_kt)

    # decl name -> new FQCN (package that declared it)
    decl_fqcn = {}
    for p in all_kt:
        if not R.is_in_module(p, module):
            continue
        parts = p.parts
        try:
            ri = parts.index("rhythm")
        except ValueError:
            continue
        mpath = R.CONFIG[module][0]
        base_len = ri + 1 + len(mpath)
        if len(parts) < base_len + 1 or parts[ri - 2] != "com" or parts[ri - 1] != "qlzhj":
            continue
        nr = list(parts[base_len:-1])
        pkg_fq = R.NEW_BASE + "." + ".".join(mpath + nr)
        for d in R.decl_names(p, include_private=False):
            decl_fqcn.setdefault(d, pkg_fq + "." + d)

    old_pkgs = set(m_pkgs.keys()) - {R.OLD_PREFIX}
    fixes = {}
    for p in all_kt:
        if R.is_in_module(p, module) or R.is_in_module(p, "macrobenchmark"):
            continue
        seg = R.old_rest_segments(p)
        if seg is None:
            continue
        pkg = R.OLD_PREFIX + ("." + ".".join(seg) if seg else "")
        if pkg not in old_pkgs:
            continue
        text = p.read_bytes().decode("utf-8", errors="replace")
        body_lines = text.splitlines()
        for d, fq in decl_fqcn.items():
            # the file may declare its own local val/fun/class with the same
            # name (legacy files have col-0 function bodies) — bare refs then
            # bind locally and must not get an import
            if re.search(r"\b(?:val|var|fun|class|object)\s+" + re.escape(d) + r"\b", text):
                continue
            pat = re.compile(r"(?<![\w.])" + re.escape(d) + r"\b")
            for i, ln in enumerate(body_lines):
                if ln.strip().startswith(("import", "package")):
                    continue
                if pat.search(ln):
                    fixes.setdefault(str(p.relative_to(R.ROOT)), set()).add(fq)
                    break
    for f, fqs in sorted(fixes.items()):
        path = R.ROOT / f
        print(f"{f}: needs {sorted(fqs)}")
        if apply:
            text = path.read_bytes().decode("utf-8")
            lines = text.splitlines(keepends=True)
            last_import = -1
            pkg_line = 0
            for i, ln in enumerate(lines):
                if ln.strip().startswith("import "):
                    last_import = i
                elif ln.strip().startswith("package "):
                    pkg_line = i
            ins = max(last_import, pkg_line) + 1
            for fq in sorted(fqs):
                imp = f"import {fq}\n"
                if imp not in text:
                    lines.insert(ins, imp)
                    ins += 1
            path.write_bytes("".join(lines).encode("utf-8"))
    if not fixes:
        print("no split-package bare references found")


if __name__ == "__main__":
    main()
