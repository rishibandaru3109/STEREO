package com.nova.music.core.designsystem.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nova.music.core.designsystem.theme.StereoTheme

@Composable
fun WallpaperBackdrop(
    config: WallpaperConfig,
    artworkUrl: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = StereoTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        when (config.mode) {
            WallpaperMode.THEME_SOLID -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                )
            }

            WallpaperMode.GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(config.gradientPreset.getBrush(colors))
                )
            }

            WallpaperMode.ALBUM_ART -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                )

                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(config.blurRadius.dp),
                        colorFilter = if (config.isDesaturated) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        } else {
                            null
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = config.dimmingAlpha))
                )
            }

            WallpaperMode.CUSTOM_IMAGE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                )

                if (!config.customImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = config.customImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(config.blurRadius.dp),
                        colorFilter = if (config.isDesaturated) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        } else {
                            null
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = config.dimmingAlpha))
                )
            }
        }

        content()
    }
}
