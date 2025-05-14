package com.example.horoscopes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HoroscopeViewModel : ViewModel() {

    // Holds the horoscope text. MutableStateFlow allows it to be observed for changes.
    private val _horoscopeText = MutableStateFlow<String?>(null)
    val horoscopeText: StateFlow<String?> = _horoscopeText.asStateFlow()

    // Holds loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Holds error messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Initialize the GenerativeModel
    // Make sure you've added your API key to local.properties and build.gradle.kts
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash-latest", // Or another suitable model
        apiKey = BuildConfig.GEMINI_API_KEY // Access the API key securely
    )

    fun fetchHoroscope(zodiacSign: String) {
        viewModelScope.launch { // Coroutine scope tied to this ViewModel
            _isLoading.value = true
            _horoscopeText.value = null // Clear previous horoscope
            _errorMessage.value = null // Clear previous error

            try {
                val currentDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
                val prompt = "Give me the daily horoscope for $zodiacSign for today, $currentDate. " +
                        "Focus on a positive and insightful outlook covering aspects like love, career, and health if possible. " +
                        "Make it about 2-3 short paragraphs."

                val response = generativeModel.generateContent(prompt)

                _horoscopeText.value = response.text
                _isLoading.value = false

            } catch (e: Exception) {
                // Handle exceptions (e.g., network errors, API errors)
                _errorMessage.value = "Failed to fetch horoscope: ${e.message}"
                _isLoading.value = false
                // You could log the full error for debugging: Log.e("HoroscopeViewModel", "API Error", e)
            }
        }
    }
}