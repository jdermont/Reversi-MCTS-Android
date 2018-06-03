package pl.derjack.reversimcts.gfx

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import pl.derjack.reversimcts.objects.Game

class BoardView : View {

    private lateinit var paint: Paint
    private var lineWidth: Float = 0f
    private var lastWidth: Int = 0
    private var marginX: Float = 0f
    private var marginY: Float = 0f
    private var cellSize: Float = 0f

    private var lastCoords: Point? = null

    private var game: Game? = null
    private var listener: BoardListener? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        initLineWidth()
        initPaint()
    }

    private fun initLineWidth() {
        val dpi = context.resources.displayMetrics.densityDpi
        lineWidth = when {
            dpi <= DisplayMetrics.DENSITY_LOW -> 1f
            dpi <= DisplayMetrics.DENSITY_MEDIUM -> 2f
            dpi <= DisplayMetrics.DENSITY_HIGH -> 3f
            dpi <= DisplayMetrics.DENSITY_XHIGH -> 4f
            dpi <= DisplayMetrics.DENSITY_XXHIGH -> 6f
            else -> 8f
        }
    }

    private fun initPaint() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.strokeWidth = lineWidth
    }

    private fun calculateDimensions() {
        val smallerSize = Math.min(width, height)
        cellSize = smallerSize / 9f
        if (width > height) {
            marginX = (cellSize + width - height) / 2f
            marginY = cellSize / 2f
        } else {
            marginX = cellSize / 2f
            marginY = (cellSize + height - width) / 2f
        }
        lastWidth = width
    }

    fun setGame(game: Game?) {
        this.game = game
    }

    fun setListener(listener: BoardListener) {
        this.listener = listener
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (lastWidth != width) {
            calculateDimensions()
        }

        // background
        paint.color = DARK_GREEN
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // lines
        paint.color = Color.BLACK
        for (i in 0..8) {
            canvas.drawLine(marginX, marginY + i * cellSize, marginX + 8 * cellSize, marginY + i * cellSize, paint)
            canvas.drawLine(marginX + i * cellSize, marginY, marginX + i * cellSize, marginY + 8 * cellSize, paint)
        }

        if (game == null) {
            return
        }

        drawBoardState(canvas)
        drawPossibleMoves(canvas)

        game?.lastMove?.apply {
            drawHalfStone(canvas, TRANSPARENT_RED, x, y)
        }
    }

    private fun drawBoardState(canvas: Canvas) {
        var currentBoard = game!!.currentBoard
        var opponentBoard = game!!.opponentBoard
        var i = 0
        var x = 0
        var y = 0
        while (i < 64) {
            if (currentBoard and java.lang.Long.MIN_VALUE != 0L) {
                drawStone(canvas, Color.BLACK, x, y)
            } else if (opponentBoard and java.lang.Long.MIN_VALUE != 0L) {
                drawStone(canvas, Color.WHITE, x, y)
            }
            currentBoard = currentBoard shl 1
            opponentBoard = opponentBoard shl 1
            x++
            if (x % Game.SIZE == 0) {
                x = 0
                y++
            }
            i++
        }
    }

    private fun drawPossibleMoves(canvas: Canvas) {
        val availableMoves = game!!.availableMoves
        val color = if (game!!.currentPlayer == Game.FIRST) Color.BLACK else Color.WHITE
        for (move in availableMoves) {
            drawHalfStone(canvas, color, move.x, move.y)
        }
    }

    private fun drawStone(canvas: Canvas, @ColorInt color: Int, x: Int, y: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawOval(marginX + x * cellSize + lineWidth, marginY + y * cellSize + lineWidth, marginX + (x + 1) * cellSize - lineWidth, marginY + (y + 1) * cellSize - lineWidth, paint!!)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawOval(marginX + x * cellSize + lineWidth, marginY + y * cellSize + lineWidth, marginX + (x + 1) * cellSize - lineWidth, marginY + (y + 1) * cellSize - lineWidth, paint!!)
    }

    private fun drawHalfStone(canvas: Canvas, @ColorInt color: Int, x: Int, y: Int) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawOval(marginX + x * cellSize + cellSize / 4, marginY + y * cellSize + cellSize / 4, marginX + (x + 1) * cellSize - cellSize / 4, marginY + (y + 1) * cellSize - cellSize / 4, paint!!)
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawOval(marginX + x * cellSize + cellSize / 4, marginY + y * cellSize + cellSize / 4, marginX + (x + 1) * cellSize - cellSize / 4, marginY + (y + 1) * cellSize - cellSize / 4, paint!!)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            val x = ((e.x - marginX) / cellSize).toInt()
            val y = ((e.y - marginY) / cellSize).toInt()
            if (x >= 0 && x < Game.SIZE && y >= 0 && y < Game.SIZE) {
                lastCoords = Point(x, y)
            }

        } else if (e.action == MotionEvent.ACTION_UP) {
            val x = ((e.x - marginX) / cellSize).toInt()
            val y = ((e.y - marginY) / cellSize).toInt()
            if (x >= 0 && x < Game.SIZE && y >= 0 && y < Game.SIZE) {
                val coords = Point(x, y)
                if (coords == lastCoords) {
                    listener?.onTappedCell(coords)
                }
            }
            lastCoords = null
        }
        return true
    }

    interface BoardListener {
        fun onTappedCell(coords: Point)
    }

    companion object {
        @ColorInt
        private val DARK_GREEN = -0xff3400
        @ColorInt
        private val TRANSPARENT_RED = 0x40ff0000
    }
}
