package com.qlzhj.rhythm.feature.widget.infrastructure.widget.glance

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Glance Widget Receiver for Rhythm Music Widget
 * 
 * This receiver handles widget lifecycle events and updates
 */
class RhythmWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RhythmMusicWidget()
}
