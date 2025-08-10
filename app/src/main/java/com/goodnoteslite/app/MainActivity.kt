package com.goodnoteslite.app

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Button

class MainActivity : AppCompatActivity() {
    lateinit var drawingView: DrawingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawingView = DrawingView(this)
        drawingView.setBackgroundColor(Color.WHITE)

        val root = FrameLayout(this)
        root.addView(drawingView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val toolbar = LinearLayout(this)
        toolbar.orientation = LinearLayout.HORIZONTAL
        toolbar.setPadding(12,12,12,12)
        toolbar.setBackgroundColor(Color.argb(30,0,0,0))

        val undo = Button(this).apply { text = "Undo"; setOnClickListener{ drawingView.undo() } }
        val redo = Button(this).apply { text = "Redo"; setOnClickListener{ drawingView.redo() } }
        val clear = Button(this).apply { text = "Clear"; setOnClickListener{ drawingView.clearAll() } }

        toolbar.addView(undo)
        toolbar.addView(redo)
        toolbar.addView(clear)

        val toolbarParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        toolbarParams.topMargin = 20
        toolbarParams.leftMargin = 20
        root.addView(toolbar, toolbarParams)

        setContentView(root)
    }
}
