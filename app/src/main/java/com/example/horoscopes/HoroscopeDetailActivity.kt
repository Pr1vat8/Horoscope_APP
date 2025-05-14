package com.example.horoscopes

import android.annotation.SuppressLint
import android.graphics.Color // Import Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class HoroscopeDetailActivity : AppCompatActivity() {

    private lateinit var zodiacSignNameTextView: TextView
    private lateinit var horoscopeTextView: TextView
    private lateinit var luckyNumberTextView: TextView
    private lateinit var luckyColorTextView: TextView
    private lateinit var luckyNumberSectionLayout: LinearLayout
    private lateinit var luckyColorSectionLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    // Color Swatch Views
    private lateinit var colorSwatch1: View
    private lateinit var colorSwatch2: View
    private lateinit var colorSwatch3: View
    private lateinit var colorSwatchesLayout: LinearLayout // Parent layout for swatches

    private val httpClient = OkHttpClient()

    companion object {
        private const val RAPIDAPI_HOST = "astropredict-daily-horoscopes-lucky-insights.p.rapidapi.com"
        private const val REQUEST_DELAY_MS = 1000L // Delay between API calls (1 second)
    }

    // Modified FetchResult to include colorHexCodes explicitly
    data class FetchResult(
        val textData: String?,
        val colorHexCodes: List<String>? = null, // List of hex codes for colors
        val success: Boolean,
        val type: String
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_horoscope_detail)

        zodiacSignNameTextView = findViewById(R.id.zodiacSignNameTextView)
        horoscopeTextView = findViewById(R.id.horoscopeTextView)
        progressBar = findViewById(R.id.progressBar)
        luckyNumberTextView = findViewById(R.id.luckyNumberTextView)
        luckyColorTextView = findViewById(R.id.luckyColorTextView)
        luckyNumberSectionLayout = findViewById(R.id.luckyNumberSectionLayout)
        luckyColorSectionLayout = findViewById(R.id.luckyColorSectionLayout)

        // Initialize color swatch Views
        colorSwatch1 = findViewById(R.id.colorSwatch1)
        colorSwatch2 = findViewById(R.id.colorSwatch2)
        colorSwatch3 = findViewById(R.id.colorSwatch3)
        colorSwatchesLayout = findViewById(R.id.colorSwatchesLayout)

        hideLuckyInfo()

        val zodiacSignName = intent.getStringExtra("ZODIAC_SIGN_NAME")

        if (zodiacSignName != null) {
            zodiacSignNameTextView.text = zodiacSignName
            fetchAllHoroscopeDataSequentially(zodiacSignName.lowercase(Locale.ROOT))
        } else {
            zodiacSignNameTextView.text = "Error"
            horoscopeTextView.text = "Could not load horoscope. Zodiac sign name missing."
            hideLuckyInfo()
            Toast.makeText(this, "Error: Zodiac sign name not provided.", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildUrl(sign: String, type: String): String {
        return when (type) {
            "horoscope" -> "https://${RAPIDAPI_HOST}/horoscope?lang=en&zodiac=$sign&type=daily"
            "number"    -> "https://${RAPIDAPI_HOST}/horoscope?zodiac=$sign&dailylucky=number"
            "color"     -> "https://${RAPIDAPI_HOST}/horoscope?zodiac=$sign&dailylucky=color"
            else        -> throw IllegalArgumentException("Unknown data type for URL: $type")
        }
    }

    private fun fetchAllHoroscopeDataSequentially(signNameLower: String) {
        val apiKey = BuildConfig.RAPIDAPI_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_DEFAULT_RAPIDAPI_KEY") {
            Log.e("HoroscopeDetail", "RapidAPI Key is missing.")
            horoscopeTextView.text = "API Key is not configured."
            progressBar.visibility = View.GONE
            luckyNumberTextView.text = "N/A"
            luckyColorTextView.text = "N/A"
            updateColorSwatches(null) // Clear swatches
            showLuckyInfo()
            return
        }

        progressBar.visibility = View.VISIBLE
        horoscopeTextView.text = "Fetching horoscope..."
        luckyNumberTextView.text = "" // Clear previous
        luckyColorTextView.text = ""  // Clear previous
        updateColorSwatches(null)     // Clear/hide swatches initially
        hideLuckyInfo()               // Hide sections until ready

        lifecycleScope.launch {
            var anyRequestFailedDueToQuota = false
            var finalLuckyColorHexCodes: List<String>? = null

            try {
                // 1. Fetch Horoscope Text
                Log.d("HoroscopeDetail", "Fetching horoscope text for $signNameLower...")
                val horoscopeResult = fetchAndParseData(buildUrl(signNameLower, "horoscope"), "horoscope", apiKey)
                horoscopeTextView.text = horoscopeResult.textData ?: "Horoscope data not available."
                if (!horoscopeResult.success && horoscopeResult.textData == "Quota Exceeded") {
                    anyRequestFailedDueToQuota = true
                }

                // 2. Fetch Lucky Number
                if (!anyRequestFailedDueToQuota) {
                    Log.d("HoroscopeDetail", "Delaying ($REQUEST_DELAY_MS ms) before fetching lucky number...")
                    luckyNumberSectionLayout.visibility = View.VISIBLE // Show section now
                    luckyNumberTextView.text = "Fetching..."
                    delay(REQUEST_DELAY_MS)

                    Log.d("HoroscopeDetail", "Fetching lucky number for $signNameLower...")
                    val luckyNumberResult = fetchAndParseData(buildUrl(signNameLower, "number"), "number", apiKey)
                    luckyNumberTextView.text = luckyNumberResult.textData ?: "N/A"
                    if (!luckyNumberResult.success && luckyNumberResult.textData == "Quota Exceeded") {
                        anyRequestFailedDueToQuota = true
                    }
                } else {
                    luckyNumberSectionLayout.visibility = View.VISIBLE // Still show section
                    luckyNumberTextView.text = "N/A (Quota)"
                }

                // 3. Fetch Lucky Color
                if (!anyRequestFailedDueToQuota) {
                    Log.d("HoroscopeDetail", "Delaying ($REQUEST_DELAY_MS ms) before fetching lucky color...")
                    luckyColorSectionLayout.visibility = View.VISIBLE // Show section now
                    luckyColorTextView.text = "Fetching..."
                    updateColorSwatches(emptyList()) // Clear swatches while fetching text
                    delay(REQUEST_DELAY_MS)

                    Log.d("HoroscopeDetail", "Fetching lucky color for $signNameLower...")
                    val luckyColorResult = fetchAndParseData(buildUrl(signNameLower, "color"), "color", apiKey)
                    luckyColorTextView.text = luckyColorResult.textData ?: "N/A"
                    finalLuckyColorHexCodes = luckyColorResult.colorHexCodes
                    if (!luckyColorResult.success && luckyColorResult.textData == "Quota Exceeded") {
                        anyRequestFailedDueToQuota = true
                    }
                } else {
                    luckyColorSectionLayout.visibility = View.VISIBLE // Still show section
                    luckyColorTextView.text = "N/A (Quota)"
                }

            } catch (e: Exception) {
                Log.e("HoroscopeDetail", "Error in sequential data fetching: ${e.message}", e)
                if (horoscopeTextView.text.toString().contains("Fetching")) horoscopeTextView.text = "Failed to load."
                if (luckyNumberTextView.text.toString().contains("Fetching")) luckyNumberTextView.text = "Error"
                if (luckyColorTextView.text.toString().contains("Fetching")) luckyColorTextView.text = "Error"
                Toast.makeText(this@HoroscopeDetailActivity, "Error fetching data.", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                updateColorSwatches(finalLuckyColorHexCodes) // Update swatches with final data or hide them
                showLuckyInfo() // Ensure parent sections are visible
                if (anyRequestFailedDueToQuota) {
                    Toast.makeText(this@HoroscopeDetailActivity, "API daily quota may have been exceeded.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun fetchAndParseData(url: String, dataType: String, apiKey: String): FetchResult {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("x-rapidapi-host", RAPIDAPI_HOST)
            .addHeader("x-rapidapi-key", apiKey)
            .build()

        var parsedTextData: String? = null
        var parsedColorHexCodes: List<String>? = null

        return try {
            val responseString = makeApiCall(request)
            val trimmedResponse = responseString.trim()

            if (trimmedResponse.startsWith("{")) {
                val jsonResponse = JSONObject(trimmedResponse)
                when (dataType) {
                    "horoscope" -> {
                        parsedTextData = if (jsonResponse.has("horoscope")) {
                            jsonResponse.getString("horoscope")
                        } else if (jsonResponse.has("prediction")) {
                            val predictionData = jsonResponse.get("prediction")
                            if (predictionData is JSONArray) {
                                val paragraphs = mutableListOf<String>()
                                for (i in 0 until predictionData.length()) {
                                    paragraphs.add(predictionData.getString(i))
                                }
                                if (paragraphs.isNotEmpty()) paragraphs.joinToString("\n\n") else null
                            } else {
                                predictionData.toString()
                            }
                        } else {
                            Log.w("HoroscopeDetail", "Horoscope/Prediction key not found for URL: $url")
                            null
                        }
                    }
                    "number" -> {
                        if (jsonResponse.has("daily_lucky_numbers")) {
                            val itemsArray = jsonResponse.optJSONArray("daily_lucky_numbers")
                            if (itemsArray != null && itemsArray.length() > 0) {
                                val numbersList = mutableListOf<String>()
                                for (i in 0 until itemsArray.length()) {
                                    val item = itemsArray.opt(i)
                                    if (item != null && !(item is JSONObject)) {
                                        numbersList.add(item.toString())
                                    }
                                }
                                if (numbersList.isNotEmpty()) {
                                    parsedTextData = numbersList.joinToString(", ")
                                } else {
                                    Log.w("HoroscopeDetail", "'daily_lucky_numbers' array for numbers did not contain valid number entries. URL: $url")
                                }
                            } else {
                                Log.w("HoroscopeDetail", "'daily_lucky_numbers' array is null or empty for dataType 'number'. URL: $url")
                            }
                        } else {
                            Log.w("HoroscopeDetail", "'daily_lucky_numbers' key not found for dataType 'number'. URL: $url")
                        }
                    }
                    "color" -> {
                        if (jsonResponse.has("daily_lucky_numbers")) {
                            val itemsArray = jsonResponse.optJSONArray("daily_lucky_numbers")
                            if (itemsArray != null && itemsArray.length() > 0) {
                                val colorNamesList = mutableListOf<String>()
                                val hexCodesList = mutableListOf<String>()
                                for (i in 0 until itemsArray.length()) {
                                    val colorObject = itemsArray.optJSONObject(i)
                                    if (colorObject != null) {
                                        val name = colorObject.optString("name")
                                        val code = colorObject.optString("code")

                                        if (!name.isNullOrEmpty() && !name.equals("null", ignoreCase = true)) {
                                            colorNamesList.add(name)
                                        }
                                        if (!code.isNullOrEmpty() && !code.equals("null", ignoreCase = true) && code.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) { // Allow 8 for alpha too
                                            hexCodesList.add(code)
                                        } else if (!code.isNullOrEmpty() && !code.equals("null", ignoreCase = true)){
                                            Log.w("HoroscopeDetail", "Invalid or missing hex code format: '$code' for color '$name'. URL: $url")
                                        }
                                    }
                                }
                                if (colorNamesList.isNotEmpty()) {
                                    parsedTextData = colorNamesList.joinToString(", ")
                                }
                                if (hexCodesList.isNotEmpty()) {
                                    parsedColorHexCodes = hexCodesList
                                }
                                if (colorNamesList.isEmpty() && (parsedTextData == null)) {
                                    Log.w("HoroscopeDetail", "'daily_lucky_numbers' array for colors did not yield valid names. URL: $url")
                                }
                            } else {
                                Log.w("HoroscopeDetail", "'daily_lucky_numbers' array is null or empty for dataType 'color'. URL: $url")
                            }
                        } else {
                            Log.w("HoroscopeDetail", "'daily_lucky_numbers' key not found for dataType 'color'. URL: $url")
                        }
                    }
                }
            } else if (trimmedResponse == "[]") {
                Log.w("HoroscopeDetail", "API for $dataType ($url) returned an empty array []. No data.")
            } else if (trimmedResponse.isEmpty()) {
                Log.w("HoroscopeDetail", "API for $dataType ($url) returned an empty response.")
            } else {
                Log.e("HoroscopeDetail", "Unexpected response format for $dataType ($url): ${trimmedResponse.take(100)}...")
            }

            val success = parsedTextData != null || (dataType == "color" && parsedColorHexCodes?.isNotEmpty() == true)
            if (!success) {
                Log.w("HoroscopeDetail", "No data ultimately parsed for $dataType. URL: $url. Response was: ${trimmedResponse.take(300)}")
            } else if (dataType != "color") {
                Log.i("HoroscopeDetail", "Successfully parsed $dataType text: $parsedTextData. URL: $url")
            }
            if (dataType == "color" && parsedColorHexCodes?.isNotEmpty() == true) {
                Log.i("HoroscopeDetail", "Successfully parsed $dataType color names: $parsedTextData, hex codes: $parsedColorHexCodes. URL: $url")
            }

            FetchResult(parsedTextData, parsedColorHexCodes, success, dataType)
        } catch (e: Exception) {
            Log.e("HoroscopeDetail", "Exception during fetch or parse for $dataType from $url: ${e.message}", e)
            var dataForFetchResult: String? = null // Renamed to avoid conflict
            if (e is IOException && e.message?.contains("code 429") == true) {
                dataForFetchResult = "Quota Exceeded"
            }
            FetchResult(dataForFetchResult, null, false, dataType)
        }
    }

    private suspend fun makeApiCall(request: Request): String {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("HoroscopeDetail", "API Call Failed: Code=${response.code}, Message=${response.message ?: ""}, URL=${request.url}")
                    Log.e("HoroscopeDetail", "Response Body: $errorBody")
                    throw IOException("Unexpected code ${response.code} - ${response.message ?: ""}. URL: ${request.url}")
                }
                response.body?.string() ?: throw IOException("Response body is null. URL: ${request.url}")
            }
        }
    }

    private fun updateColorSwatches(hexCodes: List<String>?) {
        val swatches = listOf(colorSwatch1, colorSwatch2, colorSwatch3)
        if (hexCodes.isNullOrEmpty()) {
            colorSwatchesLayout.visibility = View.GONE
            swatches.forEach { it.visibility = View.GONE }
            return
        }

        colorSwatchesLayout.visibility = View.VISIBLE
        for (i in swatches.indices) {
            if (i < hexCodes.size) {
                try {
                    swatches[i].setBackgroundColor(Color.parseColor(hexCodes[i]))
                    swatches[i].visibility = View.VISIBLE
                } catch (e: IllegalArgumentException) {
                    Log.e("HoroscopeDetail", "Invalid hex color code: ${hexCodes[i]}", e)
                    swatches[i].visibility = View.GONE
                }
            } else {
                swatches[i].visibility = View.GONE
            }
        }
    }

    private fun showLuckyInfo() {
        luckyNumberSectionLayout.visibility = View.VISIBLE
        luckyColorSectionLayout.visibility = View.VISIBLE
        // colorSwatchesLayout visibility is handled by updateColorSwatches and within luckyColorSectionLayout
    }

    private fun hideLuckyInfo() {
        luckyNumberSectionLayout.visibility = View.GONE
        luckyColorSectionLayout.visibility = View.GONE
        colorSwatchesLayout.visibility = View.GONE // Ensure this is also hidden
    }
}