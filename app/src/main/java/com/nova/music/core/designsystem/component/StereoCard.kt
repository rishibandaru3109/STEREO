package com.nova.music.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nova.music.core.designsystem.theme.CardStyleMode
import com.nova.music.core.designsystem.theme.StereoColors
import com.nova.music.core.designsystem.theme.StereoTheme

/**
 * Universal Card Container for STEREO.
 * Strictly adheres to Phase D:
 * Mode A — DEFAULT: Theme colors, standard rounded elevation and subtle border.
 * Mode B — THEME_SPECIFIC: Custom artistic card treatment designed for the active theme.
 */
@Composable
fun StereoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: (() -> Unit)? = null,
    cardMode: CardStyleMode = StereoTheme.cardMode,
    colors: StereoColors = StereoTheme.colors,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    if (cardMode == CardStyleMode.DEFAULT) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(colors.surfaceElevated)
                .border(0.5.dp, colors.surfaceHighlight, shape)
                .then(clickModifier),
            content = content
        )
    } else {
        // Theme-Specific Card Treatment
        when (colors.id) {
            "monochrome_minimal" -> {
                val monoShape = RoundedCornerShape(8.dp)
                Box(
                    modifier = modifier
                        .clip(monoShape)
                        .background(Color(0xFF161616))
                        .border(1.dp, Color(0xFF444444), monoShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "frosted_glass" -> {
                val glassShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = modifier
                        .clip(glassShape)
                        .background(colors.surfaceElevated.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), glassShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "black_yellow" -> {
                val byShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = modifier
                        .clip(byShape)
                        .background(Color(0xFF161618))
                        .border(1.dp, colors.primaryAccent.copy(alpha = 0.75f), byShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "bluey" -> {
                val blueyShape = RoundedCornerShape(12.dp)
                Box(
                    modifier = modifier
                        .clip(blueyShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF132247), Color(0xFF0C162E))
                            )
                        )
                        .border(0.8.dp, colors.primaryAccent.copy(alpha = 0.45f), blueyShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "starry_glow" -> {
                val starShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = modifier
                        .clip(starShape)
                        .background(Color(0xFF15162B))
                        .border(1.dp, colors.secondaryAccent.copy(alpha = 0.45f), starShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "ivory_beige" -> {
                val ivoryShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = modifier
                        .clip(ivoryShape)
                        .background(Color(0xFFEDE4D5))
                        .border(0.8.dp, Color(0xFFD6C8B4), ivoryShape)
                        .then(clickModifier),
                    content = content
                )
            }
            "pure_black" -> {
                val pureShape = RoundedCornerShape(8.dp)
                Box(
                    modifier = modifier
                        .clip(pureShape)
                        .background(Color(0xFF080808))
                        .border(1.dp, Color(0xFF242424), pureShape)
                        .then(clickModifier),
                    content = content
                )
            }
            else -> {
                Box(
                    modifier = modifier
                        .clip(shape)
                        .background(colors.surfaceElevated)
                        .border(0.5.dp, colors.surfaceHighlight, shape)
                        .then(clickModifier),
                    content = content
                )
            }
        }
    }
}
