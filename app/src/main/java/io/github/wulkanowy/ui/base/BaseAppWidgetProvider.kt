package io.github.wulkanowy.ui.base

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * @see [android.appwidget.AppWidgetProvider]
 */
abstract class BaseAppWidgetProvider : BroadcastReceiver(), CoroutineScope {

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    private var job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        const val WIDGET_ID_KEY = AppWidgetManager.EXTRA_APPWIDGET_IDS
    }

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras

        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIds = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    onUpdate(context, appWidgetIds, extras)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_DELETED -> {
                if (extras != null && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
                    val appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)
                    onDeleted(context, appWidgetId, extras)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                if (extras != null && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_ID)
                    && extras.containsKey(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS)
                ) {
                    val appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID)
                    val widgetExtras = extras.getBundle(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS)
                    onAppWidgetOptionsChanged(context, appWidgetId, widgetExtras)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_ENABLED -> onEnabled(context)
            AppWidgetManager.ACTION_APPWIDGET_DISABLED -> onDisabled(context)
            AppWidgetManager.ACTION_APPWIDGET_RESTORED -> {
                val oldIds = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS)
                val newIds = extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (oldIds != null && oldIds.isNotEmpty() && newIds != null && newIds.isNotEmpty()) {
                    onRestored(context, oldIds, newIds)
                    onUpdate(context, newIds, extras)
                }
            }
        }
    }

    open fun onUpdate(context: Context, appWidgetIds: IntArray, extras: Bundle?) {}
    open fun onAppWidgetOptionsChanged(context: Context, appWidgetId: Int, newOptions: Bundle?) {}
    open fun onEnabled(context: Context) {}
    open fun onDisabled(context: Context) {}
    open fun onDeleted(context: Context, appWidgetId: Int, extras: Bundle) {}
    open fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray?) {}
}
