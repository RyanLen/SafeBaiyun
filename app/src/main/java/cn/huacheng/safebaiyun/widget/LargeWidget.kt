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
import androidx.glance.Image
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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import cn.huacheng.safebaiyun.R
import cn.huacheng.safebaiyun.ShortcutActivity

/**
 *
 *@description:
 *@author: guangzhou
 *@create: 2024-05-06
 */

class LargeReceiver : DoorWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = LargeWidget
}

object LargeWidget : GlanceAppWidget() {


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
    private fun WidgetContent(context: Context, widgetId: Int, door: Door?) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(170.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.surface)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                Image(
                    modifier = GlanceModifier.size(68.dp, 96.dp),
                    provider = ImageProvider(R.drawable.guangzhou),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = null
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Box(modifier = GlanceModifier.padding(top = 8.dp)) {
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
            Spacer(modifier = GlanceModifier.defaultWeight())

            Text(
                text = door?.name ?: "请选择门禁",
                maxLines = 1,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
            )
            Text(text = if (door == null) "点击选择门禁" else "更换门禁",
                modifier = GlanceModifier.clickable(actionStartActivity(WidgetBindingStore.configureIntent(context, widgetId))),
                style = TextStyle(fontSize = 14.sp))
        }

    }

}

