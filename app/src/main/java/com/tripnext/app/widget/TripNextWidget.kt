package com.tripnext.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.tripnext.app.MainActivity

class TripNextWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent(context) }
    }
}

@Composable private fun WidgetContent(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply { data = android.net.Uri.parse("tripnext://quick-expense") }
    Column(GlanceModifier.fillMaxSize().background(ColorProvider(android.graphics.Color.rgb(15, 23, 42))).padding(16.dp)) {
        Text("TripNext", style = TextStyle(color = ColorProvider(android.graphics.Color.WHITE), fontWeight = FontWeight.Bold, fontSize = 18.sp))
        Spacer(GlanceModifier.height(8.dp))
        Button("+ Nova despesa", onClick = actionStartActivity(intent))
    }
}

class TripNextWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = TripNextWidget() }
