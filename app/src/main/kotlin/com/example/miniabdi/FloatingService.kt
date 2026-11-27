package com.example.miniabdi

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var containerView: LinearLayout
    private lateinit var charImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var questionText: TextView
    private lateinit var optionsRow: LinearLayout
    private lateinit var params: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var swayAnimator: ObjectAnimator? = null
    private var correctAnswer: Int = 0
    private var isAsking = false

    private val questionRunnable = object : Runnable {
        override fun run() {
            if (!isAsking) {
                showMathQuestion()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingOverlay()
        startIdleSway()
        scheduleNextQuestion()
    }

    private fun startForegroundNotification() {
        val channelId = "mini_abdi_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "سرویس شناور MINI ABDI",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("MINI ABDI فعاله")
                .setContentText("برای بستن آدمک، اون رو به پایین صفحه بکش.")
                .setSmallIcon(R.drawable.char_idle)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("MINI ABDI فعاله")
                .setContentText("برای بستن آدمک، اون رو به پایین صفحه بکش.")
                .setSmallIcon(R.drawable.char_idle)
                .setContentIntent(pendingIntent)
                .build()
        }

        startForeground(1, notification)
    }

    private fun createFloatingOverlay() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 300
        }

        containerView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC1E1E1E"))
                cornerRadius = 24f
            }
            setPadding(20, 8, 20, 8)
            visibility = View.GONE
        }

        charImage = ImageView(this).apply {
            setImageResource(R.drawable.char_idle)
            layoutParams = LinearLayout.LayoutParams(240, 240)
        }

        questionText = TextView(this).apply {
            text = ""
            textSize = 18f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6222222"))
                cornerRadius = 24f
            }
            setPadding(24, 12, 24, 12)
            visibility = View.GONE
        }

        optionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 0)
            visibility = View.GONE
        }

        containerView.addView(statusText)
        containerView.addView(charImage)
        containerView.addView(questionText)
        containerView.addView(optionsRow)

        setupDragAndDismiss()
        windowManager.addView(containerView, params)
    }

    private fun setupDragAndDismiss() {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        charImage.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(containerView, params)

                    val screenHeight = resources.displayMetrics.heightPixels
                    if (params.y > screenHeight - 360) {
                        statusText.text = "رها کن تا حذف بشه"
                        statusText.visibility = View.VISIBLE
                    } else if (!isAsking) {
                        statusText.visibility = View.GONE
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val screenHeight = resources.displayMetrics.heightPixels
                    if (params.y > screenHeight - 360) {
                        stopSelf()
                    } else if (!isAsking) {
                        statusText.visibility = View.GONE
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun startIdleSway() {
        isAsking = false
        charImage.setImageResource(R.drawable.char_idle)
        questionText.visibility = View.GONE
        optionsRow.visibility = View.GONE
        statusText.visibility = View.GONE

        swayAnimator?.cancel()
        swayAnimator = ObjectAnimator.ofFloat(charImage, "rotation", -12f, 12f).apply {
            duration = 1100
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun scheduleNextQuestion() {
        handler.removeCallbacks(questionRunnable)
        handler.postDelayed(questionRunnable, 60_000L)
    }

    private fun showMathQuestion() {
        isAsking = true
        swayAnimator?.cancel()
        charImage.rotation = 0f
        charImage.setImageResource(R.drawable.char_think)

        val isAddition = Random.nextBoolean()
        val a: Int
        val b: Int
        val sign: String

        if (isAddition) {
            a = Random.nextInt(2, 50)
            b = Random.nextInt(1, 49)
            correctAnswer = a + b
            sign = "+"
        } else {
            a = Random.nextInt(10, 99)
            b = Random.nextInt(1, a)
            correctAnswer = a - b
            sign = "-"
        }

        statusText.text = "دارم فکر می‌کنم..."
        statusText.visibility = View.VISIBLE

        questionText.text = "$a $sign $b = ؟"
        questionText.visibility = View.VISIBLE

        optionsRow.removeAllViews()
        val choices = generateThreeChoices(correctAnswer)
        for (choice in choices) {
            val btn = Button(this).apply {
                text = choice.toString()
                textSize = 16f
                setPadding(24, 8, 24, 8)
                setOnClickListener { handleAnswer(choice) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 0, 8, 0)
            }
            optionsRow.addView(btn, lp)
        }
        optionsRow.visibility = View.VISIBLE
    }

    private fun generateThreeChoices(answer: Int): List<Int> {
        val set = mutableSetOf(answer)
        while (set.size < 3) {
            val delta = Random.nextInt(-9, 10)
            val candidate = answer + delta
            if (candidate >= 0 && candidate != answer) {
                set.add(candidate)
            }
        }
        return set.shuffled()
    }

    private fun handleAnswer(selected: Int) {
        questionText.visibility = View.GONE
        optionsRow.visibility = View.GONE

        if (selected == correctAnswer) {
            charImage.setImageResource(R.drawable.char_happy)
            statusText.text = "آفرین! درست بود 🎉"
        } else {
            charImage.setImageResource(R.drawable.char_think)
            statusText.text = "این بار نشد، دوباره تلاش کن!"
        }
        statusText.visibility = View.VISIBLE

        handler.postDelayed({
            startIdleSway()
            scheduleNextQuestion()
        }, 2000L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(questionRunnable)
        swayAnimator?.cancel()
        if (::containerView.isInitialized && containerView.windowToken != null) {
            windowManager.removeView(containerView)
        }
    }
}
