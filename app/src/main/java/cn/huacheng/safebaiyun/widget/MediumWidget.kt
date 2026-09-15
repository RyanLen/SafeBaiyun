package cn.huacheng.safebaiyun.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.action.clickable
import cn.huacheng.safebaiyun.unlock.DataRepo
import cn.huacheng.safebaiyun.unlock.Door
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import cn.huacheng.safebaiyun.ShortcutActivity
import cn.huacheng.safebaiyun.R

/**
 *
 *@description:
 *@author: guangzhou
 *@create: 2024-05-06
 */

class MediumReceiver : DoorWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = MediumWidget
}

object MediumWidget : GlanceAppWidget() {


    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            val doors by DataRepo.state.collectAsState()
            val bindings by WidgetBindingStore.state.collectAsState()
            val door = bindings.resolve(widgetId, doors)
            GlanceTheme {
                WidgetContent(context, widgetId, door)
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        Row(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.surface)
                .fillMaxWidth()
                .cornerRadius(50.dp)
                .padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = door?.name ?: "请选择门禁",
                    maxLines = 1,
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
                )
                Text(text = if (door == null) "点击选择" else "更换门禁",
                    modifier = GlanceModifier.clickable(actionStartActivity(WidgetBindingStore.configureIntent(context, widgetId))),
                    style = TextStyle(fontSize = 14.sp))
            }
            CircleIconButton(
                imageProvider = ImageProvider(R.drawable.unlock),
                contentDescription = if (door == null) "选择门禁" else "打开" + door.name,
                backgroundColor = GlanceTheme.colors.primary,
                contentColor = GlanceTheme.colors.onPrimary,
                onClick = actionStartActivity(if (door == null) WidgetBindingStore.configureIntent(context, widgetId)
                            else WidgetBindingStore.unlockIntent(context, widgetId))
            )

        }
    }

}

