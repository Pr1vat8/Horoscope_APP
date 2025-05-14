package com.example.horoscopes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // For text color if needed
import androidx.compose.ui.layout.ContentScale // Import for ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.horoscopes.ui.theme.HoroscopesTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // This helps the background go edge to edge
        setContent {
            HoroscopesTheme {
                // Use a Box to layer the background image and the content
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Image
                    Image(
                        painter = painterResource(id = R.drawable.galaxy), // Use your image name
                        contentDescription = "Galaxy background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop // Or ContentScale.FillBounds, etc.
                    )

                    // Your main content (Scaffold and HoroscopeListScreen)
                    Scaffold(
                        // Make Scaffold background transparent so the image behind shows through
                        containerColor = Color.Transparent,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        HoroscopeListScreen(
                            modifier = Modifier.padding(innerPadding),
                            zodiacSignList = zodiacs
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoroscopeListScreen(modifier: Modifier = Modifier, zodiacSignList: List<ZodiacSign>) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp) // This padding will be applied on top of the background
    ) {
        Text(
            text = "Daily Horoscopes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White, // << CHANGE TEXT COLOR TO BE VISIBLE ON DARK BACKGROUND
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(zodiacSignList) { zodiacSign ->
                ZodiacSignRow(zodiacSign = zodiacSign)
            }
        }
    }
}

@Composable
fun ZodiacSignRow(zodiacSign: ZodiacSign) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(context, HoroscopeDetailActivity::class.java)
                intent.putExtra("ZODIAC_SIGN_NAME", zodiacSign.name)
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
                    .size(48.dp) // Slightly smaller if space is tight with background
                    .padding(end = 12.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Spacer(modifier = Modifier.size(48.dp).padding(end = 12.dp))
        }

        Text(
            text = zodiacSign.name,
            fontSize = 18.sp, // Slightly smaller
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White // << CHANGE TEXT COLOR
        )
    }
}