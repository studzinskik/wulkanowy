package io.github.wulkanowy.ui.modules.luckynumberwidget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.Resource
import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.exceptions.NoCurrentStudentException
import io.github.wulkanowy.data.repositories.LuckyNumberRepository
import io.github.wulkanowy.data.repositories.StudentRepository
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.flowWithResourceIn
import io.github.wulkanowy.utils.toFirstResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class LuckyNumberWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var sharedPref: SharedPrefProvider

    companion object {

        fun getStudentWidgetKey(appWidgetId: Int) = "lucky_number_widget_student_$appWidgetId"

        fun getThemeWidgetKey(appWidgetId: Int) = "lucky_number_widget_theme_$appWidgetId"

        fun getHeightWidgetKey(appWidgetId: Int) = "lucky_number_widget_height_$appWidgetId"

        fun getWidthWidgetKey(appWidgetId: Int) = "lucky_number_widget_width_$appWidgetId"

        const val APP_WIDGET_IDS_KEY = "app_widget_ids"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray?) {
        Timber.d("LuckyNumberWidgetProvider.onUpdate(appWidgetIds: $appWidgetIds)")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        context.startService(Intent(context, UpdateService::class.java).putExtra(APP_WIDGET_IDS_KEY, appWidgetIds))
    }

    @AndroidEntryPoint
    class UpdateService : Service(), CoroutineScope {

        @Inject
        lateinit var studentRepository: StudentRepository

        @Inject
        lateinit var luckyNumberRepository: LuckyNumberRepository

        @Inject
        lateinit var sharedPref: SharedPrefProvider

        private var job: Job = Job()

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main + job

        override fun onStart(intent: Intent?, startId: Int) {
            job = Job()
            Timber.d("Start LuckyNumberWidgetProvider.UpdateService: $startId")
            launch {
                intent?.getIntArrayExtra(APP_WIDGET_IDS_KEY)?.forEach { appWidgetId ->
                    Timber.d("LuckyNumberWidgetProvider($appWidgetId) data load: start")
                    updateWidget(appWidgetId, "...")
                    getLuckyNumber(sharedPref.getLong(getStudentWidgetKey(appWidgetId), 0), appWidgetId).let {
                        if (it.status == Status.SUCCESS) {
                            updateWidget(appWidgetId, it.data?.luckyNumber?.toString() ?: "No")
                            Timber.d("LuckyNumberWidgetProvider($appWidgetId) data load: success")
                        } else if (it.status == Status.ERROR) {
                            if (it.error is NoCurrentStudentException) return@let
                            updateWidget(appWidgetId, "Err")
                            Timber.e(it.error, "LuckyNumberWidgetProvider($appWidgetId) load error")
                        }
                    }
                }
            }
        }

        private fun updateWidget(appWidgetId: Int, number: String) {
            val remoteView = getRemoteView(appWidgetId, number)
            setStyles(remoteView, appWidgetId, null, sharedPref)

            AppWidgetManager.getInstance(this).updateAppWidget(appWidgetId, remoteView)
            Timber.d("LuckyNumberWidgetProvider($appWidgetId) updated: success")
        }

        private fun getRemoteView(appWidgetId: Int, number: String): RemoteViews {
            val appIntent = PendingIntent.getActivity(this, MainView.Section.LUCKY_NUMBER.id,
                MainActivity.getStartIntent(this, MainView.Section.LUCKY_NUMBER, true), FLAG_UPDATE_CURRENT)
            return RemoteViews(packageName, getCorrectLayoutId(appWidgetId, this, sharedPref)).apply {
                setTextViewText(R.id.luckyNumberWidgetNumber, number)
                setOnClickPendingIntent(R.id.luckyNumberWidgetContainer, appIntent)
            }
        }

        private suspend fun getLuckyNumber(studentId: Long, appWidgetId: Int) = try {
            flowWithResourceIn {
                val students = studentRepository.getSavedStudents()
                val student = students.singleOrNull { it.student.id == studentId }?.student
                val currentStudent = when {
                    student != null -> student
                    studentId != 0L && studentRepository.isCurrentStudentSet() -> {
                        studentRepository.getCurrentStudent(false).also {
                            sharedPref.putLong(getStudentWidgetKey(appWidgetId), it.id)
                        }
                    }
                    else -> throw NoCurrentStudentException()
                }

                luckyNumberRepository.getLuckyNumber(currentStudent, false)
            }.toFirstResult()
        } catch (e: Throwable) {
            Resource.error(e)
        }

        override fun onDestroy() {
            super.onDestroy()
            job.cancel()
        }

        override fun onBind(intent: Intent?): IBinder? = null
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds?.forEach { appWidgetId ->
            with(sharedPref) {
                delete(getHeightWidgetKey(appWidgetId))
                delete(getStudentWidgetKey(appWidgetId))
                delete(getThemeWidgetKey(appWidgetId))
                delete(getWidthWidgetKey(appWidgetId))
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        Timber.d("onAppWidgetOptionsChanged($appWidgetId)")

        val remoteView = RemoteViews(context.packageName, getCorrectLayoutId(appWidgetId, context, sharedPref))

        setStyles(remoteView, appWidgetId, newOptions, sharedPref)
        appWidgetManager.updateAppWidget(appWidgetId, remoteView)
    }
}

private fun setStyles(views: RemoteViews, appWidgetId: Int, options: Bundle? = null, sharedPref: SharedPrefProvider) {
    val width = options?.getInt(OPTION_APPWIDGET_MIN_WIDTH) ?: sharedPref.getLong(LuckyNumberWidgetProvider.getWidthWidgetKey(appWidgetId), 74).toInt()
    val height = options?.getInt(OPTION_APPWIDGET_MAX_HEIGHT) ?: sharedPref.getLong(LuckyNumberWidgetProvider.getHeightWidgetKey(appWidgetId), 74).toInt()

    with(sharedPref) {
        putLong(LuckyNumberWidgetProvider.getWidthWidgetKey(appWidgetId), width.toLong())
        putLong(LuckyNumberWidgetProvider.getHeightWidgetKey(appWidgetId), height.toLong())
    }

    val rows = getCellsForSize(height)
    val cols = getCellsForSize(width)

    Timber.d("New lucky number widget measurement: %dx%d", width, height)
    Timber.d("Widget size: $cols x $rows")

    when {
        1 == cols && 1 == rows -> views.setVisibility(imageTop = false, imageLeft = false)
        1 == cols && 1 < rows -> views.setVisibility(imageTop = true, imageLeft = false)
        1 < cols && 1 == rows -> views.setVisibility(imageTop = false, imageLeft = true)
        1 == cols && 1 == rows -> views.setVisibility(imageTop = true, imageLeft = false)
        2 == cols && 1 == rows -> views.setVisibility(imageTop = false, imageLeft = true)
        else -> views.setVisibility(imageTop = false, imageLeft = false, title = true)
    }
}

private fun RemoteViews.setVisibility(imageTop: Boolean, imageLeft: Boolean, title: Boolean = false) {
    setViewVisibility(R.id.luckyNumberWidgetImageTop, if (imageTop) VISIBLE else GONE)
    setViewVisibility(R.id.luckyNumberWidgetImageLeft, if (imageLeft) VISIBLE else GONE)
    setViewVisibility(R.id.luckyNumberWidgetTitle, if (title) VISIBLE else GONE)
    setViewVisibility(R.id.luckyNumberWidgetNumber, VISIBLE)
}

private fun getCellsForSize(size: Int): Int {
    var n = 2
    while (74 * n - 30 < size) ++n
    return n - 1
}

private fun getCorrectLayoutId(appWidgetId: Int, context: Context, sharedPref: SharedPrefProvider): Int {
    val savedTheme = sharedPref.getLong(LuckyNumberWidgetProvider.getThemeWidgetKey(appWidgetId), 0)
    val isSystemDarkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    return if (savedTheme == 1L || (savedTheme == 2L && isSystemDarkMode)) {
        R.layout.widget_luckynumber_dark
    } else {
        R.layout.widget_luckynumber
    }
}
