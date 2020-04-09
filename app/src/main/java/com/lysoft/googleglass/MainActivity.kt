package com.lysoft.googleglass

import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), GlassGestureDetector.OnGestureListener {
    private lateinit var glassGestureDetector: GlassGestureDetector
    private  lateinit var tvTouchResult : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        glassGestureDetector = GlassGestureDetector(this, this)
        tvTouchResult = findViewById(R.id.tv_response)
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean {
        when (gesture) {
            GlassGestureDetector.Gesture.TAP ->{
                tvTouchResult.setText("Gesture detects Tap")
                Toast.makeText(this,"Tap", Toast.LENGTH_SHORT).show()
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_FORWARD ->{
                tvTouchResult.setText("Gesture detects Swipe Forward")
                Toast.makeText(this,"Swipe Forward", Toast.LENGTH_SHORT).show()
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD ->{
                tvTouchResult.setText("Gesture detects Swipe Backward")
                Toast.makeText(this,"Swipe Backward", Toast.LENGTH_SHORT).show()
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_UP ->{
                tvTouchResult.setText("Gesture detects Swipe Up")
                Toast.makeText(this,"Swipe Up", Toast.LENGTH_SHORT).show()
                return true
            }

            GlassGestureDetector.Gesture.SWIPE_DOWN ->{
                tvTouchResult.setText("Gesture detects Swipe Down")
                Toast.makeText(this,"Swipe Down", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> return false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return if (glassGestureDetector.onTouchEvent(ev)) {
            true
        } else super.dispatchTouchEvent(ev)
    }


}
