package com.example.nudger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.unit.sp
import com.example.nudger.R

// Google Font provider setup for Figtree
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Figtree font family using Google Fonts
val FigtreeFont = FontFamily(
    Font(
        googleFont = GoogleFont("Figtree"),
        fontProvider = provider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Figtree"),
        fontProvider = provider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Figtree"),
        fontProvider = provider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Figtree"),
        fontProvider = provider,
        weight = FontWeight.Bold
    ),
    Font(
        googleFont = GoogleFont("Figtree"),
        fontProvider = provider,
        weight = FontWeight.ExtraBold
    )
)

// Set of Material typography styles using Figtree font
val Typography = Typography(    
    displayLarge = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
        color = Color(0xFF333333)
    ),    
    displayMedium = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    
    displaySmall = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    
    headlineLarge = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    
    headlineMedium = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    
    headlineSmall = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    titleLarge = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        color = Color(0xFF333333)
    ),    
    titleMedium = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
        color = Color(0xFF333333)
    ),    
    titleSmall = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = Color(0xFF333333)
    ),    
    bodyLarge = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFF333333)
    ),    
    bodyMedium = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp,
        color = Color(0xFF333333)
    ),    
    bodySmall = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
        color = Color(0xFF333333)
    ),    
    labelLarge = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = Color(0xFF333333)
    ),    
    labelMedium = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFF333333)
    ),    
    labelSmall = TextStyle(
        fontFamily = FigtreeFont,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
        color = Color(0xFF333333)
    )
)