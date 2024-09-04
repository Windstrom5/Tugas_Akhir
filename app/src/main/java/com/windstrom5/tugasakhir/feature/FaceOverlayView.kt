package com.windstrom5.tugasakhir.feature

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val faceBounds = mutableListOf<RectF>()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = 0xFFFF0000.toInt()
        strokeWidth = 5f
    }

    fun setFaces(faces: List<Face>) {
        faceBounds.clear()
        faces.forEach { face ->
            faceBounds.add(RectF(face.boundingBox))
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faceBounds.forEach { rect ->
            canvas.drawRect(rect, paint)
        }
    }
}