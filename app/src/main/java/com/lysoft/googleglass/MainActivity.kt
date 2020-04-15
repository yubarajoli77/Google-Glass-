package com.lysoft.googleglass

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import edu.cmu.pocketsphinx.*
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


class MainActivity : AppCompatActivity(), GlassGestureDetector.OnGestureListener,
    RecognitionListener {
    private lateinit var glassGestureDetector: GlassGestureDetector
    private lateinit var tvTouchResult: TextView

    /* Named searches allow to quickly reconfigure the decoder */
    private val KWS_SEARCH = "wakeup"
    private val FORECAST_SEARCH = "forecast"
    private val DIGITS_SEARCH = "digits"
    private val PHONE_SEARCH = "phones"
    private val MENU_SEARCH = "menu"
    /* Keyword we are looking for to activate menu */
    private val KEYPHRASE = "OK Glass"

    /* Used to handle permission request */
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

    private var recognizer: SpeechRecognizer? = null
    private lateinit var captions: HashMap<String, Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prepare the data for UI
        captions = HashMap()
        captions[KWS_SEARCH] = R.string.kws_caption
        captions[MENU_SEARCH] = R.string.menu_caption
        captions[DIGITS_SEARCH] = R.string.digits_caption
        captions[PHONE_SEARCH] = R.string.phone_caption
        captions[FORECAST_SEARCH] = R.string.forecast_caption
        setContentView(R.layout.activity_main)

        glassGestureDetector = GlassGestureDetector(this, this)
        tvTouchResult = findViewById(R.id.tv_response)
        tvTouchResult.setText(R.string.preparing_recognizer)

        // Check if user has given permission to record audio
        // Check if user has given permission to record audio
        val permissionCheck: Int = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO)
            return
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        SetupTask(this).execute()
    }

    @SuppressLint("StaticFieldLeak")
    private inner class SetupTask internal constructor(activity: MainActivity) :
        AsyncTask<Void?, Void?, java.lang.Exception?>() {
        var activityReference: WeakReference<MainActivity> = WeakReference(activity)

        override fun onPostExecute(result: java.lang.Exception?) {
            if (result != null) {
                (activityReference.get()!!.findViewById(R.id.tv_response) as TextView).text =
                    "Failed to init recognizer $result"
            } else {
                activityReference.get()!!.switchSearch(KWS_SEARCH)
            }
        }

        override fun doInBackground(vararg params: Void?): java.lang.Exception? {
            try {
                val assets = Assets(activityReference.get())
                val assetDir = assets.syncAssets()
                activityReference.get()!!.setupRecognizer(assetDir)
            } catch (e: IOException) {
                return e
            }
            return null
        }
    }

    override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean {
        when (gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                tvTouchResult.text = "Gesture detects Tap"
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.type = "image/*"
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent);
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                tvTouchResult.text = "Gesture detects Swipe Forward"
                Toast.makeText(this, "Swipe Forward", Toast.LENGTH_SHORT).show()
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                tvTouchResult.text = "Gesture detects Swipe Backward"
                Toast.makeText(this, "Swipe Backward", Toast.LENGTH_SHORT).show()
                return true
            }
            GlassGestureDetector.Gesture.SWIPE_UP -> {
                tvTouchResult.text = "Gesture detects Swipe Up"
                Toast.makeText(this, "Swipe Up", Toast.LENGTH_SHORT).show()
                return true
            }

//            GlassGestureDetector.Gesture.SWIPE_DOWN -> {
//                tvTouchResult.text = "Gesture detects Swipe Down"
//                Toast.makeText(this, "Swipe Down", Toast.LENGTH_SHORT).show()
//                return true
//            }
            else -> return false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return if (glassGestureDetector.onTouchEvent(ev)) {
            true
        } else super.dispatchTouchEvent(ev)
    }

    override fun onResult(hypothesis: Hypothesis?) {
       tvTouchResult.text = ""
        if (hypothesis != null) {
            val text: String = hypothesis.hypstr
            Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPartialResult(hypothesis: Hypothesis?) {
        if (hypothesis == null) return

        when (val text: String = hypothesis.hypstr) {
            KEYPHRASE -> switchSearch(MENU_SEARCH)
            DIGITS_SEARCH -> switchSearch(DIGITS_SEARCH)
            PHONE_SEARCH -> switchSearch(PHONE_SEARCH)
            FORECAST_SEARCH -> switchSearch(FORECAST_SEARCH)
            else -> tvTouchResult.text = text
        }
    }

    override fun onTimeout() {
        switchSearch(KWS_SEARCH)
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onEndOfSpeech() {
        if (recognizer!!.searchName != KWS_SEARCH)
            switchSearch(KWS_SEARCH)
    }

    override fun onError(error: Exception?) {
        tvTouchResult.text = error!!.message
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                SetupTask(this).execute()
            } else {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recognizer != null) {
            recognizer!!.cancel()
            recognizer!!.shutdown()
        }
    }

    private fun switchSearch(searchName: String) {
        recognizer!!.stop()
        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName == KWS_SEARCH)
            recognizer!!.startListening(searchName)
        else
            recognizer!!.startListening(searchName, 10000)

        val caption = resources.getString(captions[searchName]!!)
        tvTouchResult.text = caption
    }

    @Throws(IOException::class)
    private fun setupRecognizer(assetsDir: File) { // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        recognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assetsDir, "en-us-ptm"))
            .setDictionary(File(assetsDir, "cmudict-en-us.dict"))
            .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
            .recognizer
        recognizer!!.addListener(this)
        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */
        // Create keyword-activation search.
        recognizer!!.addKeyphraseSearch(KWS_SEARCH,KEYPHRASE)
        // Create grammar-based search for selection between demos
        val menuGrammar = File(assetsDir, "menu.gram")
        recognizer!!.addGrammarSearch(MENU_SEARCH, menuGrammar)
        // Create grammar-based search for digit recognition
        val digitsGrammar = File(assetsDir, "digits.gram")
        recognizer!!.addGrammarSearch(DIGITS_SEARCH,digitsGrammar)
        // Create language model search
        val languageModel = File(assetsDir, "weather.dmp")
        recognizer!!.addNgramSearch(FORECAST_SEARCH,languageModel)
        // Phonetic search
        val phoneticModel = File(assetsDir, "en-phone.dmp")
        recognizer!!.addAllphoneSearch(PHONE_SEARCH,phoneticModel)
    }

}
