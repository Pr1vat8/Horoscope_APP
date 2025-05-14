package com.example.horoscopes

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
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

// Define a constant for SharedPreferences (if not already defined/imported from MainActivity)
private const val PREFS_NAME_DETAIL = "horoscope_prefs" // Ensure this matches MainActivity
private const val API_KEY_PREF_DETAIL = "api_key"     // Ensure this matches MainActivity


class HoroscopeDetailActivity : AppCompatActivity() {

    private lateinit var zodiacSignNameTextView: TextView
    private lateinit var horoscopeTextView: TextView
    private lateinit var luckyNumberTextView: TextView
    private lateinit var luckyColorTextView: TextView
    private lateinit var luckyNumberSectionLayout: LinearLayout
    private lateinit var luckyColorSectionLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var colorSwatch1: View
    private lateinit var colorSwatch2: View
    private lateinit var colorSwatch3: View
    private lateinit var colorSwatchesLayout: LinearLayout

    private val httpClient = OkHttpClient()

    companion object {
        private const val RAPIDAPI_HOST = "astropredict-daily-horoscopes-lucky-insights.p.rapidapi.com"
        private const val REQUEST_DELAY_MS = 1000L // Keep delay to respect API rate limits
    }

    data class FetchResult(
        val textData: String?,
        val colorHexCodes: List<String>? = null,
        val success: Boolean,
        val type: String // "horoscope", "number", "color"
    )

    private fun getStoredApiKey(): String {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME_DETAIL, Context.MODE_PRIVATE)
        return prefs.getString(API_KEY_PREF_DETAIL, "") ?: ""
    }

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

        colorSwatch1 = findViewById(R.id.colorSwatch1)
        colorSwatch2 = findViewById(R.id.colorSwatch2)
        colorSwatch3 = findViewById(R.id.colorSwatch3)
        colorSwatchesLayout = findViewById(R.id.colorSwatchesLayout)

        hideLuckyInfo()

        val zodiacSignName = intent.getStringExtra("ZODIAC_SIGN_NAME")
        // Retrieve API key passed from MainActivity, or fallback to SharedPreferences, then BuildConfig
        val passedApiKey = intent.getStringExtra("API_KEY")

        val apiKeyToUse = if (!passedApiKey.isNullOrBlank()) {
            passedApiKey
        } else {
            val storedKey = getStoredApiKey()
            if (storedKey.isNotBlank()) storedKey else BuildConfig.RAPIDAPI_KEY
        }


        if (zodiacSignName != null) {
            zodiacSignNameTextView.text = zodiacSignName
            if (apiKeyToUse.isBlank() || apiKeyToUse == "YOUR_DEFAULT_RAPIDAPI_KEY") { // Check against placeholder too
                Log.e("HoroscopeDetail", "RapidAPI Key is missing or is the default placeholder.")
                horoscopeTextView.text = "API Key is not configured. Please set it in the main screen."
                progressBar.visibility = View.GONE
                luckyNumberTextView.text = "N/A"
                luckyColorTextView.text = "N/A"
                updateColorSwatches(null)
                showLuckyInfo() // Show sections with N/A
                Toast.makeText(this, "API Key is missing. Please set it via the main screen.", Toast.LENGTH_LONG).show()
            } else {
                fetchAllHoroscopeDataSequentially(zodiacSignName.lowercase(Locale.ROOT), apiKeyToUse)
            }
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
            "number" -> "https://${RAPIDAPI_HOST}/horoscope?zodiac=$sign&dailylucky=number"
            "color" -> "https://${RAPIDAPI_HOST}/horoscope?zodiac=$sign&dailylucky=color"
            else -> throw IllegalArgumentException("Unknown data type for URL: $type")
        }
    }

    private fun fetchAllHoroscopeDataSequentially(signNameLower: String, apiKey: String) {
        // API Key check is now done in onCreate before calling this function

        progressBar.visibility = View.VISIBLE
        horoscopeTextView.text = "Fetching horoscope..."
        luckyNumberTextView.text = "" // Clear previous
        luckyColorTextView.text = ""  // Clear previous
        updateColorSwatches(null)     // Clear previous swatches
        hideLuckyInfo()               // Hide until data is ready or confirmed N/A

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
                    luckyNumberSectionLayout.visibility = View.VISIBLE // Show section before fetching
                    luckyNumberTextView.text = "Fetching..."
                    delay(REQUEST_DELAY_MS)

                    Log.d("HoroscopeDetail", "Fetching lucky number for $signNameLower...")
                    val luckyNumberResult = fetchAndParseData(buildUrl(signNameLower, "number"), "number", apiKey)
                    luckyNumberTextView.text = luckyNumberResult.textData ?: "N/A"
                    if (!luckyNumberResult.success && luckyNumberResult.textData == "Quota Exceeded") {
                        anyRequestFailedDueToQuota = true
                    }
                } else {
                    luckyNumberSectionLayout.visibility = View.VISIBLE
                    luckyNumberTextView.text = "N/A (API Issue)"
                }

                // 3. Fetch Lucky Color
                if (!anyRequestFailedDueToQuota) {
                    Log.d("HoroscopeDetail", "Delaying ($REQUEST_DELAY_MS ms) before fetching lucky color...")
                    luckyColorSectionLayout.visibility = View.VISIBLE // Show section before fetching
                    luckyColorTextView.text = "Fetching..."
                    updateColorSwatches(emptyList()) // Show swatches area, possibly empty initially
                    delay(REQUEST_DELAY_MS)

                    Log.d("HoroscopeDetail", "Fetching lucky color for $signNameLower...")
                    val luckyColorResult = fetchAndParseData(buildUrl(signNameLower, "color"), "color", apiKey)
                    luckyColorTextView.text = luckyColorResult.textData ?: "N/A"
                    finalLuckyColorHexCodes = luckyColorResult.colorHexCodes
                    if (!luckyColorResult.success && luckyColorResult.textData == "Quota Exceeded") {
                        anyRequestFailedDueToQuota = true
                    }
                } else {
                    luckyColorSectionLayout.visibility = View.VISIBLE
                    luckyColorTextView.text = "N/A (API Issue)"
                }

            } catch (e: Exception) {
                Log.e("HoroscopeDetail", "Error in sequential data fetching: ${e.message}", e)
                if (horoscopeTextView.text.toString().contains("Fetching")) horoscopeTextView.text = "Failed to load."
                if (luckyNumberTextView.text.toString().contains("Fetching")) luckyNumberTextView.text = "Error"
                if (luckyColorTextView.text.toString().contains("Fetching")) luckyColorTextView.text = "Error"
                Toast.makeText(this@HoroscopeDetailActivity, "Error fetching data. Check network or API key.", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                updateColorSwatches(finalLuckyColorHexCodes) // Update with final colors or clear if null
                showLuckyInfo() // Ensure sections are visible to show "N/A" or data

                if (anyRequestFailedDueToQuota) {
                    Toast.makeText(this@HoroscopeDetailActivity, "API daily quota may have been exceeded for one or more requests.", Toast.LENGTH_LONG).show()
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

            // --- Same parsing logic as before ---
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
                                    if (item != null && !(item is JSONObject)) { // Ensure item is not another JSON object
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
                                        if (!code.isNullOrEmpty() && !code.equals("null", ignoreCase = true) && code.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
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
            } else if (trimmedResponse == "[]") { // Handle empty JSON array specifically
                Log.w("HoroscopeDetail", "API for $dataType ($url) returned an empty array []. No data.")
                // parsedTextData remains null
            } else if (trimmedResponse.isEmpty()) {
                Log.w("HoroscopeDetail", "API for $dataType ($url) returned an empty response.")
                // parsedTextData remains null
            }
            else { // Non-JSON, non-empty array, non-empty string response
                Log.e("HoroscopeDetail", "Unexpected response format for $dataType ($url): ${trimmedResponse.take(100)}...")
                // This could be an error message string from the API not in JSON format
                // For example, some APIs return plain text errors
                // Check if it's a known error message like "Quota Exceeded"
                if (trimmedResponse.contains("Invalid API key", ignoreCase = true)) {
                    parsedTextData = "Invalid API Key" // Specific feedback
                } else if (trimmedResponse.contains("Quota Exceeded", ignoreCase = true) || trimmedResponse.contains("limit", ignoreCase = true)) {
                    parsedTextData = "Quota Exceeded"
                }
                // else parsedTextData remains null if not a recognized plain text error
            }
            // --- End of original parsing logic ---

            val success = parsedTextData != null || (dataType == "color" && parsedColorHexCodes?.isNotEmpty() == true)

            // If success is false, but parsedTextData was set to "Quota Exceeded" or "Invalid API Key", keep that message
            if (!success && (parsedTextData == "Quota Exceeded" || parsedTextData == "Invalid API Key")) {
                // Do nothing, the error message is already set
            } else if (!success) {
                Log.w("HoroscopeDetail", "No data ultimately parsed for $dataType. URL: $url. Response was: ${trimmedResponse.take(300)}")
                // parsedTextData = null // Ensure it's null if not a specific error message
            } else if (dataType != "color") { // Successful parse for non-color
                Log.i("HoroscopeDetail", "Successfully parsed $dataType text: $parsedTextData. URL: $url")
            }

            if (dataType == "color" && parsedColorHexCodes?.isNotEmpty() == true) {
                Log.i("HoroscopeDetail", "Successfully parsed $dataType color names: $parsedTextData, hex codes: $parsedColorHexCodes. URL: $url")
            }


            return FetchResult(parsedTextData, parsedColorHexCodes, success, dataType)

        } catch (e: IOException) { // Catch network errors or errors from makeApiCall
            Log.e("HoroscopeDetail", "IOException during fetch or parse for $dataType from $url: ${e.message}", e)
            var dataForFetchResult: String? = null
            // Check for specific error codes if possible (e.g., 429 for quota from makeApiCall exception message)
            if (e.message?.contains("code 429") == true || e.message?.contains("Quota Exceeded", ignoreCase = true) == true) {
                dataForFetchResult = "Quota Exceeded"
            } else if (e.message?.contains("code 401") == true || e.message?.contains("code 403") == true || e.message?.contains("Unauthenticated", ignoreCase = true) == true || e.message?.contains("Invalid API key", ignoreCase = true) == true) {
                dataForFetchResult = "Invalid API Key"
            } else {
                dataForFetchResult = "Network Error" // Generic error for other IOExceptions
            }
            return FetchResult(dataForFetchResult, null, false, dataType)
        } catch (e: Exception) { // Catch other exceptions (e.g., JSONException)
            Log.e("HoroscopeDetail", "General Exception during fetch or parse for $dataType from $url: ${e.message}", e)
            return FetchResult("Parsing Error", null, false, dataType)
        }
    }


    private suspend fun makeApiCall(request: Request): String {
        return withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e("HoroscopeDetail", "API Call Failed: Code=${response.code}, Message=${response.message}, URL=${request.url}")
                    Log.e("HoroscopeDetail", "Response Body: $errorBody")
                    // Throw an IOException with details that can be caught by fetchAndParseData
                    throw IOException("Unexpected code ${response.code} - ${response.message}. Body: $errorBody. URL: ${request.url}")
                }
                response.body?.string() ?: throw IOException("Response body is null. URL: ${request.url}")
            }
        }
    }


    private fun updateColorSwatches(hexCodes: List<String>?) {
        val swatches = listOf(colorSwatch1, colorSwatch2, colorSwatch3)
        if (hexCodes.isNullOrEmpty()) {
            colorSwatchesLayout.visibility = View.GONE // Hide the whole layout
            // swatches.forEach { it.visibility = View.GONE } // Individual hiding not needed if layout is GONE
            return
        }

        colorSwatchesLayout.visibility = View.VISIBLE
        swatches.forEachIndexed { index, swatch ->
            if (index < hexCodes.size) {
                try {
                    swatch.setBackgroundColor(Color.parseColor(hexCodes[index]))
                    swatch.visibility = View.VISIBLE
                } catch (e: IllegalArgumentException) {
                    Log.e("HoroscopeDetail", "Invalid hex color code: ${hexCodes[index]}", e)
                    swatch.visibility = View.GONE
                }
            } else {
                swatch.visibility = View.GONE
            }
        }
    }

    private fun showLuckyInfo() {
        // These are always made visible at the end of fetching,
        // their content (TextViews) will show data, "N/A", or "Error".
        luckyNumberSectionLayout.visibility = View.VISIBLE
        luckyColorSectionLayout.visibility = View.VISIBLE
        // colorSwatchesLayout visibility is handled by updateColorSwatches
    }

    private fun hideLuckyInfo() {
        // Initially hide them until data is fetched or determined to be unavailable.
        luckyNumberSectionLayout.visibility = View.GONE
        luckyColorSectionLayout.visibility = View.GONE
        colorSwatchesLayout.visibility = View.GONE
    }
}