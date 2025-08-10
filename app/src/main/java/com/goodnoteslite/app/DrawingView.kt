package com.goodnoteslite.app

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class DrawingView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null): View(ctx, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    data class Point(val x: Float, val y: Float, val t: Long, val pressure: Float)
    data class Stroke(var points: MutableList<Point>, var path: Path = Path(), var paint: Paint = Paint())

    private val strokes = mutableListOf<Stroke>()
    private val undone = mutableListOf<Stroke>()
    private var current: Stroke? = null

    private var bitmap: Bitmap? = null
    private var canvasBmp: Canvas? = null

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var longPressTriggered = false

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvasBmp = Canvas(bitmap!!)
            clearBitmap()
            redrawAllToBitmap()
        }
    }

    private fun clearBitmap() { canvasBmp?.drawColor(Color.WHITE) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        current?.let { canvas.drawPath(it.path, paint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val p = if (event.pointerCount > 0) event.getPressure(0) else 1f

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressTriggered = false
                longPressRunnable = Runnable {
                    longPressTriggered = true
                    current?.let { trySnapToShape(it) }
                }
                handler.postDelayed(longPressRunnable!!, 500)
                undone.clear()
                current = Stroke(mutableListOf(), Path(), Paint(paint))
                current!!.points.add(Point(x,y,System.currentTimeMillis(),p))
                current!!.path.moveTo(x,y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                handler.removeCallbacks(longPressRunnable!!)
                current?.let {
                    it.points.add(Point(x,y,System.currentTimeMillis(),p))
                    if (it.points.size >= 3) {
                        val p1 = it.points[it.points.size - 3]
                        val p2 = it.points[it.points.size - 2]
                        val p3 = it.points[it.points.size - 1]
                        it.path.quadTo(p2.x, p2.y, (p2.x + p3.x)/2, (p2.y + p3.y)/2)
                    } else {
                        it.path.lineTo(x,y)
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable!!)
                current?.let {
                    beautifyStroke(it)
                    strokes.add(it)
                    drawStrokeToBitmap(it)
                    current = null
                    invalidate()
                }
            }
        }
        return true
    }

    private fun beautifyStroke(s: Stroke) {
        if (s.points.size < 3) return
        val resampled = resamplePoints(s.points, 64)
        s.path.reset()
        s.path.moveTo(resampled[0].x, resampled[0].y)
        for (i in 1 until resampled.size - 2) {
            val p0 = resampled[i]
            val p1 = resampled[i+1]
            val midx = (p0.x + p1.x)/2
            val midy = (p0.y + p1.y)/2
            s.path.quadTo(p0.x, p0.y, midx, midy)
        }
        val last = resampled.last()
        s.path.lineTo(last.x, last.y)
    }

    private fun resamplePoints(points: List<Point>, n: Int): List<Point> {
        if (points.size <= n) return points
        val dists = mutableListOf(0.0)
        for (i in 1 until points.size) {
            val dx = (points[i].x - points[i-1].x).toDouble()
            val dy = (points[i].y - points[i-1].y).toDouble()
            dists.add(dists.last() + sqrt(dx*dx + dy*dy))
        }
        val total = dists.last()
        val interval = total / (n - 1)
        val newPoints = mutableListOf<Point>()
        var j = 0
        newPoints.add(points[0])
        for (i in 1 until n-1) {
            val target = i * interval
            while (j < dists.size - 1 && dists[j+1] < target) j++
            val ratio = if (dists[j+1] - dists[j] == 0.0) 0.0 else (target - dists[j]) / (dists[j+1] - dists[j])
            val x = (points[j].x + ratio * (points[j+1].x - points[j].x)).toFloat()
            val y = (points[j].y + ratio * (points[j+1].y - points[j].y)).toFloat()
            newPoints.add(Point(x, y, System.currentTimeMillis(), 1f))
        }
        newPoints.add(points.last())
        return newPoints
    }

    private fun drawStrokeToBitmap(s: Stroke) {
        s.paint = Paint(paint)
        canvasBmp?.drawPath(s.path, s.paint)
    }

    private fun redrawAllToBitmap() {
        clearBitmap()
        for (s in strokes) canvasBmp?.drawPath(s.path, s.paint)
    }

    fun undo(){ if (strokes.isNotEmpty()){ undone.add(strokes.removeAt(strokes.size-1)); redrawAllToBitmap(); invalidate() } }
    fun redo(){ if (undone.isNotEmpty()){ strokes.add(undone.removeAt(undone.size-1)); redrawAllToBitmap(); invalidate() } }
    fun clearAll(){ strokes.clear(); undone.clear(); clearBitmap(); invalidate() }

    private fun trySnapToShape(s: Stroke) {
        val pts = s.points
        if (pts.size < 6) return
        val start = pts.first()
        val end = pts.last()
        val distStartEnd = hypot((start.x-end.x).toDouble(), (start.y-end.y).toDouble())
        val bbox = boundingBox(pts)
        val w = bbox[2] - bbox[0]
        val h = bbox[3] - bbox[1]
        if (distStartEnd < max(20f, min(w,h)*0.2)) {
            val aspect = w/h
            if (aspect > 0.7 && aspect < 1.3) {
                val cx = (bbox[0]+bbox[2])/2
                val cy = (bbox[1]+bbox[3])/2
                val r = (max(w,h))/2
                val path = Path()
                path.addCircle(cx, cy, r, Path.Direction.CW)
                s.path = path
            } else {
                val path = Path()
                path.addOval(RectF(bbox[0], bbox[1], bbox[2], bbox[3]), Path.Direction.CW)
                s.path = path
            }
        } else {
            val lineError = computeLineError(pts)
            if (lineError < 8f) {
                val path = Path()
                path.moveTo(pts.first().x, pts.first().y)
                path.lineTo(pts.last().x, pts.last().y)
                s.path = path
            }
        }
        drawStrokeToBitmap(s)
        invalidate()
    }

    private fun boundingBox(pts: List<Point>): FloatArray {
        var minx = Float.MAX_VALUE; var miny = Float.MAX_VALUE; var maxx = -Float.MAX_VALUE; var maxy = -Float.MAX_VALUE
        for (p in pts) {
            if (p.x < minx) minx = p.x
            if (p.y < miny) miny = p.y
            if (p.x > maxx) maxx = p.x
            if (p.y > maxy) maxy = p.y
        }
        return floatArrayOf(minx, miny, maxx, maxy)
    }

    private fun computeLineError(pts: List<Point>): Float {
        val x0 = pts.first().x; val y0 = pts.first().y
        val x1 = pts.last().x; val y1 = pts.last().y
        val dx = x1 - x0; val dy = y1 - y0
        val denom = sqrt(dx*dx + dy*dy)
        if (denom == 0f) return Float.MAX_VALUE
        var sum = 0f
        for (p in pts) {
            val num = abs(dy*p.x - dx*p.y + x1*y0 - y1*x0)
            sum += num / denom
        }
        return sum / pts.size
    }
}
