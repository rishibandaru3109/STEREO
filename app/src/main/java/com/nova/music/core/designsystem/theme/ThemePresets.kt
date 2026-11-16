package com.nova.music.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Built-in Theme Presets for STEREO.
 * 12 distinct, high-quality audio aesthetic palettes.
 */
object ThemePresets {

    val MidnightStereo = StereoColors(
        id = "midnight_stereo",
        name = "Midnight Stereo",
        isDark = true,
        background = Color(0xFF0D0D11),
        backgroundSecondary = Color(0xFF15151A),
        surface = Color(0xFF1B1B22),
        surfaceElevated = Color(0xFF24242D),
        surfaceHighlight = Color(0xFF32323D),
        primaryAccent = Color(0xFFE58B24),
        secondaryAccent = Color(0xFFD4AF37),
        textPrimary = Color(0xFFF5F2EB),
        textSecondary = Color(0xFFA39F93),
        textTertiary = Color(0xFF6A675F),
        playerBackground = Color(0xFF0D0D11),
        miniPlayerBackground = Color(0xFF181820),
        controlActive = Color(0xFFE58B24),
        controlInactive = Color(0xFF6A675F),
        sliderTrack = Color(0xFF32323D),
        sliderThumb = Color(0xFFE58B24),
        divider = Color(0xFF22222C)
    )

    val VintageCream = StereoColors(
        id = "vintage_cream",
        name = "Vintage Cream",
        isDark = false,
        background = Color(0xFFF8F5EE),
        backgroundSecondary = Color(0xFFEFECE3),
        surface = Color(0xFFE6E1D5),
        surfaceElevated = Color(0xFFDCD7C9),
        surfaceHighlight = Color(0xFFCECBBD),
        primaryAccent = Color(0xFFC85A32),
        secondaryAccent = Color(0xFFB86B35),
        textPrimary = Color(0xFF2B2520),
        textSecondary = Color(0xFF5C5248),
        textTertiary = Color(0xFF8C8174),
        playerBackground = Color(0xFFF8F5EE),
        miniPlayerBackground = Color(0xFFEAE5D9),
        controlActive = Color(0xFFC85A32),
        controlInactive = Color(0xFF8C8174),
        sliderTrack = Color(0xFFCECBBD),
        sliderThumb = Color(0xFFC85A32),
        divider = Color(0xFFDCD7C9)
    )

    val BurgundyVinyl = StereoColors(
        id = "burgundy_vinyl",
        name = "Burgundy Vinyl",
        isDark = true,
        background = Color(0xFF150A0E),
        backgroundSecondary = Color(0xFF201016),
        surface = Color(0xFF2D161F),
        surfaceElevated = Color(0xFF3A1E29),
        surfaceHighlight = Color(0xFF4C2937),
        primaryAccent = Color(0xFFE05A78),
        secondaryAccent = Color(0xFFC73E60),
        textPrimary = Color(0xFFF7ECEF),
        textSecondary = Color(0xFFB89CA4),
        textTertiary = Color(0xFF7B6269),
        playerBackground = Color(0xFF150A0E),
        miniPlayerBackground = Color(0xFF26121A),
        controlActive = Color(0xFFE05A78),
        controlInactive = Color(0xFF7B6269),
        sliderTrack = Color(0xFF4C2937),
        sliderThumb = Color(0xFFE05A78),
        divider = Color(0xFF3A1E29)
    )

    val OliveRadio = StereoColors(
        id = "olive_radio",
        name = "Olive Radio",
        isDark = true,
        background = Color(0xFF111611),
        backgroundSecondary = Color(0xFF182018),
        surface = Color(0xFF212B21),
        surfaceElevated = Color(0xFF2D3A2D),
        surfaceHighlight = Color(0xFF3B4B3B),
        primaryAccent = Color(0xFFD4AF37),
        secondaryAccent = Color(0xFF8F9E7B),
        textPrimary = Color(0xFFF0F4E8),
        textSecondary = Color(0xFFA3AD94),
        textTertiary = Color(0xFF6A735E),
        playerBackground = Color(0xFF111611),
        miniPlayerBackground = Color(0xFF1C241C),
        controlActive = Color(0xFFD4AF37),
        controlInactive = Color(0xFF6A735E),
        sliderTrack = Color(0xFF3B4B3B),
        sliderThumb = Color(0xFFD4AF37),
        divider = Color(0xFF2D3A2D)
    )

    val PureAmoled = StereoColors(
        id = "pure_amoled",
        name = "Pure AMOLED",
        isDark = true,
        background = Color(0xFF000000),
        backgroundSecondary = Color(0xFF080808),
        surface = Color(0xFF121212),
        surfaceElevated = Color(0xFF1C1C1C),
        surfaceHighlight = Color(0xFF2A2A2A),
        primaryAccent = Color(0xFFFFA726),
        secondaryAccent = Color(0xFFFFCC80),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB0B0B0),
        textTertiary = Color(0xFF666666),
        playerBackground = Color(0xFF000000),
        miniPlayerBackground = Color(0xFF0F0F0F),
        controlActive = Color(0xFFFFA726),
        controlInactive = Color(0xFF666666),
        sliderTrack = Color(0xFF2A2A2A),
        sliderThumb = Color(0xFFFFA726),
        divider = Color(0xFF1C1C1C)
    )

    val CassetteSunset = StereoColors(
        id = "cassette_sunset",
        name = "Cassette Sunset",
        isDark = true,
        background = Color(0xFF160F0C),
        backgroundSecondary = Color(0xFF221612),
        surface = Color(0xFF301F1A),
        surfaceElevated = Color(0xFF3F2922),
        surfaceHighlight = Color(0xFF53372E),
        primaryAccent = Color(0xFFFF6B35),
        secondaryAccent = Color(0xFFF79256),
        textPrimary = Color(0xFFFFF4ED),
        textSecondary = Color(0xFFC9A89A),
        textTertiary = Color(0xFF856A5E),
        playerBackground = Color(0xFF160F0C),
        miniPlayerBackground = Color(0xFF261813),
        controlActive = Color(0xFFFF6B35),
        controlInactive = Color(0xFF856A5E),
        sliderTrack = Color(0xFF53372E),
        sliderThumb = Color(0xFFFF6B35),
        divider = Color(0xFF3F2922)
    )

    val MinimalMono = StereoColors(
        id = "minimal_mono",
        name = "Minimal Mono",
        isDark = true,
        background = Color(0xFF0C0C0C),
        backgroundSecondary = Color(0xFF161616),
        surface = Color(0xFF202020),
        surfaceElevated = Color(0xFF2C2C2C),
        surfaceHighlight = Color(0xFF3C3C3C),
        primaryAccent = Color(0xFFE0E0E0),
        secondaryAccent = Color(0xFF9E9E9E),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFF9E9E9E),
        textTertiary = Color(0xFF616161),
        playerBackground = Color(0xFF0C0C0C),
        miniPlayerBackground = Color(0xFF1A1A1A),
        controlActive = Color(0xFFFFFFFF),
        controlInactive = Color(0xFF616161),
        sliderTrack = Color(0xFF3C3C3C),
        sliderThumb = Color(0xFFFFFFFF),
        divider = Color(0xFF2C2C2C)
    )

    val AlbumAtmosphere = StereoColors(
        id = "album_atmosphere",
        name = "Album Atmosphere",
        isDark = true,
        background = Color(0xFF110E18),
        backgroundSecondary = Color(0xFF1B1626),
        surface = Color(0xFF262035),
        surfaceElevated = Color(0xFF342C47),
        surfaceHighlight = Color(0xFF453B5D),
        primaryAccent = Color(0xFFA78BFA),
        secondaryAccent = Color(0xFF818CF8),
        textPrimary = Color(0xFFF5F3FF),
        textSecondary = Color(0xFFA7A0BA),
        textTertiary = Color(0xFF6A637C),
        playerBackground = Color(0xFF110E18),
        miniPlayerBackground = Color(0xFF1E182A),
        controlActive = Color(0xFFA78BFA),
        controlInactive = Color(0xFF6A637C),
        sliderTrack = Color(0xFF453B5D),
        sliderThumb = Color(0xFFA78BFA),
        divider = Color(0xFF342C47)
    )

    val OceanTape = StereoColors(
        id = "ocean_tape",
        name = "Ocean Tape",
        isDark = true,
        background = Color(0xFF0A1118),
        backgroundSecondary = Color(0xFF101C26),
        surface = Color(0xFF172736),
        surfaceElevated = Color(0xFF203448),
        surfaceHighlight = Color(0xFF2C4660),
        primaryAccent = Color(0xFF38BDF8),
        secondaryAccent = Color(0xFF0284C7),
        textPrimary = Color(0xFFF0F9FF),
        textSecondary = Color(0xFF94A3B8),
        textTertiary = Color(0xFF52637A),
        playerBackground = Color(0xFF0A1118),
        miniPlayerBackground = Color(0xFF121E2A),
        controlActive = Color(0xFF38BDF8),
        controlInactive = Color(0xFF52637A),
        sliderTrack = Color(0xFF2C4660),
        sliderThumb = Color(0xFF38BDF8),
        divider = Color(0xFF203448)
    )

    val LavenderHaze = StereoColors(
        id = "lavender_haze",
        name = "Lavender Haze",
        isDark = true,
        background = Color(0xFF131018),
        backgroundSecondary = Color(0xFF1C1823),
        surface = Color(0xFF272231),
        surfaceElevated = Color(0xFF352E42),
        surfaceHighlight = Color(0xFF453C56),
        primaryAccent = Color(0xFFC084FC),
        secondaryAccent = Color(0xFFE879F9),
        textPrimary = Color(0xFFFAF5FF),
        textSecondary = Color(0xFFAD9FBF),
        textTertiary = Color(0xFF6E627E),
        playerBackground = Color(0xFF131018),
        miniPlayerBackground = Color(0xFF1E1927),
        controlActive = Color(0xFFC084FC),
        controlInactive = Color(0xFF6E627E),
        sliderTrack = Color(0xFF453C56),
        sliderThumb = Color(0xFFC084FC),
        divider = Color(0xFF352E42)
    )

    val CopperStudio = StereoColors(
        id = "copper_studio",
        name = "Copper Studio",
        isDark = true,
        background = Color(0xFF140F0D),
        backgroundSecondary = Color(0xFF1F1714),
        surface = Color(0xFF2B201C),
        surfaceElevated = Color(0xFF382B25),
        surfaceHighlight = Color(0xFF4C3B33),
        primaryAccent = Color(0xFFD97706),
        secondaryAccent = Color(0xFFB45309),
        textPrimary = Color(0xFFFEF3C7),
        textSecondary = Color(0xFFB89C82),
        textTertiary = Color(0xFF7A6451),
        playerBackground = Color(0xFF140F0D),
        miniPlayerBackground = Color(0xFF221814),
        controlActive = Color(0xFFD97706),
        controlInactive = Color(0xFF7A6451),
        sliderTrack = Color(0xFF4C3B33),
        sliderThumb = Color(0xFFD97706),
        divider = Color(0xFF382B25)
    )

    val PaperAndInk = StereoColors(
        id = "paper_and_ink",
        name = "Paper & Ink",
        isDark = false,
        background = Color(0xFFF4F0E8),
        backgroundSecondary = Color(0xFFE8E2D5),
        surface = Color(0xFFDDD5C3),
        surfaceElevated = Color(0xFFD0C6B0),
        surfaceHighlight = Color(0xFFC2B69E),
        primaryAccent = Color(0xFFB91C1C),
        secondaryAccent = Color(0xFF7F1D1D),
        textPrimary = Color(0xFF18181B),
        textSecondary = Color(0xFF44444A),
        textTertiary = Color(0xFF787882),
        playerBackground = Color(0xFFF4F0E8),
        miniPlayerBackground = Color(0xFFE4DECF),
        controlActive = Color(0xFFB91C1C),
        controlInactive = Color(0xFF787882),
        sliderTrack = Color(0xFFC2B69E),
        sliderThumb = Color(0xFFB91C1C),
        divider = Color(0xFFD0C6B0)
    )

    /**
     * All 12 built-in presets in display order.
     */
    val allPresets: List<StereoColors> = listOf(
        MidnightStereo,
        VintageCream,
        BurgundyVinyl,
        OliveRadio,
        PureAmoled,
        CassetteSunset,
        MinimalMono,
        AlbumAtmosphere,
        OceanTape,
        LavenderHaze,
        CopperStudio,
        PaperAndInk
    )

    fun getById(id: String?): StereoColors {
        return allPresets.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: MidnightStereo
    }
}
