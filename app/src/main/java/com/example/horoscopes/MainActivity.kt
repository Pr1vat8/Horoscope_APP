package com.example.horoscopes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.horoscopes.ui.theme.HoroscopesTheme

// Define a constant for SharedPreferences
private const val PREFS_NAME = "horoscope_prefs"
private const val API_KEY_PREF = "api_key"

class MainActivity : ComponentActivity() {

    val zodiacs = listOf(
        ZodiacSign("Aries", "aries"),
        ZodiacSign("Taurus", "taurus"),
        ZodiacSign("Gemini", "gemini"),
        ZodiacSign("Cancer", "cancer"),
        ZodiacSign("Leo", "leo"),
        ZodiacSign("Virgo", "virgo"),
        ZodiacSign("Libra", "libra"),
        ZodiacSign("Scorpio", "scorpio"),
        ZodiacSign("Sagittarius", "sagittarius"),
        ZodiacSign("Capricorn", "capricorn"),
        ZodiacSign("Aquarius", "aquarius"),
        ZodiacSign("Pisces", "pisces")
    )

    private fun getApiKey(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(API_KEY_PREF, "") ?: ""
    }

    private fun saveApiKey(context: Context, apiKey: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(API_KEY_PREF, apiKey)
            apply()
        }
    }

    private fun clearApiKey(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            remove(API_KEY_PREF)
            apply()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HoroscopesTheme {
                val context = LocalContext.current
                var showDialog by remember { mutableStateOf(false) }
                var apiKeyInput by remember { mutableStateOf(TextFieldValue(getApiKey(context))) }
                // Recompose when API key changes in SharedPreferences (e.g. after dialog save)
                var currentApiKeyDisplay by remember { mutableStateOf(getApiKeyForDisplay(context)) }


                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.galaxy),
                        contentDescription = "Galaxy background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Scaffold(
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                        ) {
                            ApiKeySection(
                                currentApiKeyDisplay = currentApiKeyDisplay,
                                onSetApiKeyClick = {
                                    // Update text field with current saved key when dialog opens
                                    apiKeyInput = TextFieldValue(getApiKey(context))
                                    showDialog = true
                                }
                            )
                            HoroscopeListScreen(
                                modifier = Modifier.weight(1f), // Ensure list takes remaining space
                                zodiacSignList = zodiacs,
                                currentApiKey = getApiKey(context) // Pass current API key
                            )
                        }
                    }
                }

                if (showDialog) {
                    ApiKeyInputDialog(
                        apiKeyInputValue = apiKeyInput,
                        onApiKeyInputValueChange = { apiKeyInput = it },
                        onDismiss = { showDialog = false },
                        onSave = {
                            saveApiKey(context, apiKeyInput.text)
                            currentApiKeyDisplay = getApiKeyForDisplay(context) // Update display
                            showDialog = false
                        },
                        onUseDefault = {
                            clearApiKey(context)
                            apiKeyInput = TextFieldValue("") // Clear input field
                            currentApiKeyDisplay = getApiKeyForDisplay(context) // Update display
                            showDialog = false
                        }
                    )
                }
            }
        }
    }
    private fun getApiKeyForDisplay(context: Context): String {
        val savedKey = getApiKey(context)
        return if (savedKey.isNotBlank()) "Using Custom API Key" else "Using Default API Key"
    }
}

@Composable
fun ApiKeySection(
    currentApiKeyDisplay: String,
    onSetApiKeyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = currentApiKeyDisplay,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(onClick = onSetApiKeyClick) {
            Text("Set API Key")
        }
    }
}


@Composable
fun ApiKeyInputDialog(
    apiKeyInputValue: TextFieldValue,
    onApiKeyInputValueChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUseDefault: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set API Key", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                OutlinedTextField(
                    value = apiKeyInputValue,
                    onValueChange = onApiKeyInputValueChange,
                    label = { Text("Enter API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Leave blank and save, or click 'Use Default' to use the default API key (if configured).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                Button(
                    onClick = onUseDefault,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Use Default")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}


@Composable
fun HoroscopeListScreen(
    modifier: Modifier = Modifier,
    zodiacSignList: List<ZodiacSign>,
    currentApiKey: String // Receive current API key
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp) // Padding for the content within the list screen
    ) {
        Text(
            text = "Daily Horoscopes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 16.dp) // Adjusted padding
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(zodiacSignList) { zodiacSign ->
                ZodiacSignRow(zodiacSign = zodiacSign, apiKey = currentApiKey)
            }
        }
    }
}

@Composable
fun ZodiacSignRow(zodiacSign: ZodiacSign, apiKey: String) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, HoroscopeDetailActivity::class.java).apply {
                    putExtra("ZODIAC_SIGN_NAME", zodiacSign.name)
                    putExtra("API_KEY", apiKey) // Pass the current API key
                }
                context.startActivity(intent)
            }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageResId = context.resources.getIdentifier(
            zodiacSign.imageName,
            "drawable",
            context.packageName
        )

        if (imageResId != 0) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = zodiacSign.name + " symbol",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp).padding(end = 12.dp))
        }

        Text(
            text = zodiacSign.name,
            fontSize = 18.sp,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}