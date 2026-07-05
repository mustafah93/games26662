package com.example.balldodge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var running = false

    private val paintBall = Paint().apply { color = Color.CYAN; isAntiAlias = true }
    private val paintObstacle = Paint().apply { color = Color.rgb(230, 60, 60); isAntiAlias = true }
    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
    }
    private val paintBg = Paint().apply { color = Color.parseColor("#1B1B2F") }

    private var screenWidth = 0
    private var screenHeight = 0

    // الكرة
    private var ballX = 0f
    private var ballY = 0f
    private val ballRadius = 40f
    private var stickX = 0f // قيمة من -1 إلى 1 قادمة من عصا يد PS4
    private val ballSpeed = 25f

    // العوائق
    data class Obstacle(var x: Float, var y: Float, val w: Float, val h: Float, var speed: Float)
    private val obstacles = mutableListOf<Obstacle>()
    private var spawnTimer = 0L
    private var spawnInterval = 1000L

    private var score = 0
    private var gameOver = false
    private var lastTime = System.currentTimeMillis()

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun setStickX(value: Float) {
        // منطقة ميتة (dead-zone) لتفادي انحراف العصا الذاتي
        stickX = if (abs(value) < 0.15f) 0f else value
    }

    fun restartIfGameOver() {
        if (gameOver) resetGame()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && gameOver) {
            resetGame()
        }
        return true
    }

    private fun resetGame() {
        obstacles.clear()
        ballX = screenWidth / 2f
        ballY = screenHeight * 0.8f
        score = 0
        spawnInterval = 1000L
        spawnTimer = 0L
        gameOver = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        resetGame()
        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        screenWidth = w
        screenHeight = h
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
        }
    }

    fun resumeGame() {
        if (!running) {
            running = true
            gameThread = Thread(this)
            gameThread?.start()
        }
    }

    fun pauseGame() {
        running = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
        }
    }

    override fun run() {
        while (running) {
            val now = System.currentTimeMillis()
            val delta = now - lastTime
            lastTime = now

            if (!gameOver) {
                update(delta)
            }
            draw()

            try {
                Thread.sleep(16)
            } catch (e: InterruptedException) {
            }
        }
    }

    private fun update(delta: Long) {
        // تحريك الكرة حسب عصا التحكم
        ballX += stickX * ballSpeed
        if (ballX < ballRadius) ballX = ballRadius
        if (ballX > screenWidth - ballRadius) ballX = screenWidth - ballRadius

        // توليد عوائق جديدة
        spawnTimer += delta
        if (spawnTimer > spawnInterval) {
            spawnTimer = 0
            val w = 100f + Random.nextFloat() * 100f
            val x = Random.nextFloat() * (screenWidth - w)
            obstacles.add(Obstacle(x, -100f, w, 60f, 8f + Random.nextFloat() * 4f))
            if (spawnInterval > 400L) spawnInterval -= 15L
        }

        // تحريك العوائق وفحص التصادم
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val o = iterator.next()
            o.y += o.speed
            if (o.y > screenHeight) {
                iterator.remove()
                score++
                continue
            }
            if (ballY + ballRadius > o.y && ballY - ballRadius < o.y + o.h &&
                ballX + ballRadius > o.x && ballX - ballRadius < o.x + o.w
            ) {
                gameOver = true
            }
        }
    }

    private fun draw() {
        if (!holder.surface.isValid) return
        val canvas: Canvas = holder.lockCanvas()
        try {
            canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paintBg)

            canvas.drawCircle(ballX, ballY, ballRadius, paintBall)

            for (o in obstacles) {
                canvas.drawRect(o.x, o.y, o.x + o.w, o.y + o.h, paintObstacle)
            }

            paintText.textSize = 60f
            canvas.drawText("النقاط: $score", 40f, 80f, paintText)

            if (gameOver) {
                paintText.textSize = 90f
                canvas.drawText("انتهت اللعبة", screenWidth / 2f - 260f, screenHeight / 2f, paintText)
                paintText.textSize = 45f
                canvas.drawText(
                    "اضغط X أو المس الشاشة لإعادة اللعب",
                    screenWidth / 2f - 400f,
                    screenHeight / 2f + 80f,
                    paintText
                )
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}
