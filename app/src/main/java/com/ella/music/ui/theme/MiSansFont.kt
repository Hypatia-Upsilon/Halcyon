package com.ella.music.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Asset path of the bundled MiSans Semibold. Single source of truth for the ~8 MB TTF. */
internal const val MISANS_SEMIBOLD_ASSET_PATH = "fonts/MiSans-Semibold.ttf"

/**
 * The bundled MiSans Semibold as a Compose [FontFamily], loaded straight from `assets/` instead of a
 * duplicate `res/font/` resource. Keeping only the asset copy (which is also extracted to filesDir
 * for the native LyricView) means the font ships once in the APK instead of twice (~7 MB saved).
 */
internal fun bundledMiSansSemiboldFontFamily(context: Context): FontFamily =
    FontFamily(
        Font(
            path = MISANS_SEMIBOLD_ASSET_PATH,
            assetManager = context.assets,
            weight = FontWeight(800)
        )
    )
