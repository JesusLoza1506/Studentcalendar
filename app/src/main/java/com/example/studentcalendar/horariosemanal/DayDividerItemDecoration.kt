package com.example.studentcalendar.horariosemanal

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class DayDividerItemDecoration(
    private val dividerWidth: Int, // Ancho de la línea divisoria en píxeles
    private val dividerColor: Int // Color de la línea divisoria
) : RecyclerView.ItemDecoration() {

    private val paint = Paint()

    init {
        paint.color = dividerColor
        paint.style = Paint.Style.FILL
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0

        if (position < itemCount - 1) {
            outRect.right = dividerWidth
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(canvas, parent, state)

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            val itemCount = parent.adapter?.itemCount ?: 0

            if (position < itemCount - 1) {
                val left = child.right // La línea comienza en el borde derecho del elemento
                val top = parent.paddingTop // La línea se extiende desde la parte superior del RecyclerView
                val right = left + dividerWidth // La línea termina después de su ancho
                val bottom = parent.height - parent.paddingBottom // La línea se extiende hasta la parte inferior del RecyclerView

                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
    }
}
