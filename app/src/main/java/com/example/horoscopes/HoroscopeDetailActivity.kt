package com.example.horoscopes

import android.os.Bundle
import android.util.Log // For logging
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch // Import for launch

class HoroscopeDetailActivity : AppCompatActivity() {

    // Declare views that will be initialized in onCreate
    private lateinit var zodiacSignNameTextView: TextView
    private lateinit var horoscopeTextView: TextView
    private lateinit var progressBar: ProgressBar

    // Declare the GenerativeModel variable
    private lateinit var generativeModel: GenerativeModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horoscope_detail)

        // Initialize the views
        zodiacSignNameTextView = findViewById(R.id.zodiacSignNameTextView)
        horoscopeTextView = findViewById(R.id.horoscopeTextView)
        progressBar = findViewById(R.id.progressBar)

        // Retrieve the Zodiac sign name passed from MainActivity
        val zodiacSignName = intent.getStringExtra("ZODIAC_SIGN_NAME")

        if (zodiacSignName != null) {
            zodiacSignNameTextView.text = zodiacSignName
            // Now that we have the name, let's fetch the horoscope
            initializeGenerativeModel() // Initialize the model first
            fetchHoroscope(zodiacSignName)
        } else {
            // Handle the case where the name is not passed (should not happen if MainActivity is correct)
            zodiacSignNameTextView.text = "Error"
            horoscopeTextView.text = "Could not load horoscope. Zodiac sign name missing."
            Toast.makeText(this, "Error: Zodiac sign name not provided.", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeGenerativeModel() {
        // Retrieve the API key from BuildConfig
        // Make sure you've added the API key to your local.properties
        // and the buildConfigField in your app/build.gradle.kts
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "YOUR_DEFAULT_API_KEY_IF_NOT_FOUND") {
            Log.e("HoroscopeDetail", "API Key is missing or default. Please check local.properties and build.gradle.kts.")
            horoscopeTextView.text = "API Key is not configured. Please check app setup."
            // You might want to disable further API calls if the key is missing
            return
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash", // Or "gemini-pro" or other suitable models
            apiKey = apiKey
        )
    }

    private fun fetchHoroscope(signName: String) {
        if (!::generativeModel.isInitialized) { // Check if model was initialized (API key might be missing)
            Log.e("HoroscopeDetail", "GenerativeModel not initialized. API key might be missing.")
            horoscopeTextView.text = "Horoscope service not available (API key issue)."
            return
        }

        // Show progress bar and clear previous horoscope text
        progressBar.visibility = View.VISIBLE
        horoscopeTextView.text = "Fetching your horoscope..." // Or clear it: ""

        // Get today's date for the prompt
        // The current date is May 10, 2025.
        val todayDate = "May 10, 2025" // We'll use the current date

        val prompt = "What is the daily horoscope for the zodiac sign $signName for today, $todayDate? Focus on general themes like love, career, and health. Keep it concise, around 2-3 paragraphs."

        // Use lifecycleScope to launch a coroutine for the API call
        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                progressBar.visibility = View.GONE // Hide progress bar
                horoscopeTextView.text = response.text ?: "No horoscope text received."
            } catch (e: Exception) {
                progressBar.visibility = View.GONE // Hide progress bar
                Log.e("HoroscopeDetail", "Error fetching horoscope: ${e.message}", e)
                horoscopeTextView.text = "Sorry, couldn't fetch the horoscope. Error: ${e.localizedMessage}"
                Toast.makeText(this@HoroscopeDetailActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}