package com.infintykey.taskwake

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val database by lazy { TaskDatabase.getInstance(applicationContext) }
    private lateinit var taskContainer: LinearLayout
    private lateinit var permissionMessage: TextView
    private lateinit var permissionButton: Button

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { updatePermissionStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
            setBackgroundColor(Color.rgb(248, 249, 252))
        }
        val baseLeft = dp(20)
        val baseTop = dp(12)
        val baseRight = dp(20)
        val baseBottom = dp(16)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(baseLeft + bars.left, baseTop + bars.top, baseRight + bars.right, baseBottom + bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, root)?.isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, root)?.isAppearanceLightNavigationBars = true

        val heading = text("TaskWake", 28f, bold = true).apply { setTextColor(Color.rgb(28, 45, 76)) }
        root.addView(heading, matchWrap())
        root.addView(text("مهامك وتذكيراتك في مكان واحد", 15f, color = Color.DKGRAY), matchWrap())

        val permissionPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = rounded(Color.rgb(235, 241, 250), 14)
        }
        permissionMessage = text("", 14f)
        permissionPanel.addView(permissionMessage, matchWrap())
        permissionButton = Button(this).apply {
            text = "تفعيل الإشعارات"
            setOnClickListener { requestNotificationPermission() }
        }
        permissionPanel.addView(permissionButton, matchWrap())
        val panelParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(18); bottomMargin = dp(12) }
        root.addView(permissionPanel, panelParams)

        taskContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        scroll.addView(taskContainer, ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val addButton = Button(this).apply {
            text = "+  إضافة مهمة"
            textSize = 16f
            setOnClickListener { showAddTaskDialog() }
        }
        root.addView(addButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)).apply { topMargin = dp(10) })
        setContentView(root)
        updatePermissionStatus()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                database.taskDao().observeAll().collect { renderTasks(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionMessage.isInitialized) updatePermissionStatus()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(this, "تحقق من إعدادات إشعارات التطبيق في الجهاز", Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePermissionStatus() {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val enabled = runtimeGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
        permissionMessage.text = if (enabled) {
            "الإشعارات مفعّلة. قد تختلف طريقة ظهورها أو تنبيه الشاشة حسب إعدادات الجهاز."
        } else {
            "الإشعارات غير مفعّلة؛ لن تظهر التذكيرات حتى تمنح التطبيق الصلاحية وتسمح بها من إعدادات الجهاز."
        }
        permissionButton.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun showAddTaskDialog() {
        var reminderAt: Long? = null
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }
        val titleInput = EditText(this).apply {
            hint = "عنوان المهمة"
            singleLine = true
            textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        }
        val descriptionInput = EditText(this).apply {
            hint = "وصف اختياري"
            minLines = 2
            maxLines = 4
            gravity = Gravity.TOP or Gravity.START
        }
        val reminderButton = Button(this).apply { text = "اختيار موعد تذكير (اختياري)" }
        val clearReminderButton = Button(this).apply {
            text = "إزالة موعد التذكير"
            visibility = View.GONE
            setOnClickListener {
                reminderAt = null
                reminderButton.text = "اختيار موعد تذكير (اختياري)"
                visibility = View.GONE
            }
        }
        reminderButton.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val defaultHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
                val defaultMinute = now.get(Calendar.MINUTE)
                TimePickerDialog(this, { _, hour, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    reminderAt = selected
                    reminderButton.text = "التذكير: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(selected)}"
                    clearReminderButton.visibility = View.VISIBLE
                }, defaultHour, defaultMinute, true).show()
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        form.addView(titleInput, matchWrap())
        form.addView(descriptionInput, matchWrap())
        form.addView(reminderButton, matchWrap())
        form.addView(clearReminderButton, matchWrap())

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("مهمة جديدة")
            .setView(form)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text.toString().trim()
                if (title.isBlank()) {
                    titleInput.error = "أدخل عنوان المهمة"
                    return@setOnClickListener
                }
                if (reminderAt != null && !TaskTimeRules.isFutureReminder(reminderAt, System.currentTimeMillis())) {
                    Toast.makeText(this, "اختر وقتًا مستقبليًا للتذكير", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val task = TaskEntity(
                    title = title,
                    description = descriptionInput.text.toString().trim(),
                    reminderAtMillis = reminderAt,
                )
                lifecycleScope.launch {
                    val id = database.taskDao().upsert(task)
                    if (reminderAt != null) ReminderScheduler.schedule(this@MainActivity, task.copy(id = id))
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun renderTasks(tasks: List<TaskEntity>) {
        taskContainer.removeAllViews()
        val upcoming = tasks.filterNot { it.isCompleted }
        val completed = tasks.filter { it.isCompleted }
        if (upcoming.isEmpty() && completed.isEmpty()) {
            taskContainer.addView(text("لا توجد مهام بعد. أضف مهمتك الأولى للبدء.", 16f, color = Color.GRAY).apply {
                gravity = Gravity.CENTER
                setPadding(dp(18), dp(48), dp(18), dp(48))
            }, matchWrap())
            return
        }
        taskContainer.addView(text("المهام القادمة (${upcoming.size})", 19f, bold = true).apply {
            setPadding(0, dp(8), 0, dp(8))
            setTextColor(Color.rgb(28, 45, 76))
        }, matchWrap())
        if (upcoming.isEmpty()) {
            taskContainer.addView(text("لا توجد مهام قادمة.", 14f, color = Color.GRAY).apply { setPadding(0, 0, 0, dp(10)) }, matchWrap())
        }
        upcoming.forEach { taskContainer.addView(taskCard(it), matchWrap()) }
        if (completed.isNotEmpty()) {
            taskContainer.addView(text("مكتملة (${completed.size})", 18f, bold = true).apply {
                setPadding(0, dp(16), 0, dp(8))
                setTextColor(Color.rgb(80, 91, 108))
            }, matchWrap())
            completed.forEach { taskContainer.addView(taskCard(it), matchWrap()) }
        }
    }

    private fun taskCard(task: TaskEntity): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(10))
            background = rounded(if (task.isCompleted) Color.rgb(240, 242, 245) else Color.WHITE, 14)
            elevation = dp(2).toFloat()
        }
        val title = text(task.title, 17f, bold = true).apply {
            setTextColor(if (task.isCompleted) Color.GRAY else Color.rgb(34, 46, 67))
        }
        card.addView(title, matchWrap())
        if (task.description.isNotBlank()) card.addView(text(task.description, 14f, color = Color.DKGRAY), matchWrap())
        val reminder = task.reminderAtMillis?.let {
            "التذكير: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(it)}"
        } ?: "بلا تذكير"
        card.addView(text(reminder, 13f, color = Color.rgb(86, 102, 125)).apply { setPadding(0, dp(5), 0, dp(3)) }, matchWrap())

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val delete = Button(this).apply {
            text = "حذف"
            setOnClickListener {
                lifecycleScope.launch {
                    ReminderScheduler.cancel(this@MainActivity, task.id)
                    database.taskDao().deleteById(task.id)
                }
            }
        }
        actions.addView(delete, LinearLayout.LayoutParams(0, dp(46), 1f))
        val complete = Button(this).apply {
            text = if (task.isCompleted) "مكتملة" else "إكمال"
            isEnabled = !task.isCompleted
            setOnClickListener {
                lifecycleScope.launch {
                    ReminderScheduler.cancel(this@MainActivity, task.id)
                    database.taskDao().update(task.copy(isCompleted = true))
                }
            }
        }
        actions.addView(complete, LinearLayout.LayoutParams(0, dp(46), 1f))
        card.addView(actions, matchWrap())
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { bottomMargin = dp(10) }
        card.layoutParams = params
        return card
    }

    private fun text(value: String, size: Float, color: Int = Color.BLACK, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(3), 0, dp(3))
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
