package com.nova.music.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Built-in Theme Presets for STEREO.
 * Features 7 Official Themes and classic audio aesthetic palettes.
 */
object ThemePresets {

    // ========================================================================
    // 7 OFFICIAL STEREO THEMES (PHASE C)
    // ========================================================================

    /**
     * Theme 1 — Monochrome Minimal
     * Strictly monochromatic, minimal visual noise, strong typography, subtle borders.
     */
    val MonochromeMinimal = StereoColors(
        id = "monochrome_minimal",
        name = "Monochrome Minimal",
        isDark = true,
        background = Color(0xFF0A0A0A),
        backgroundSecondary = Color(0xFF141414),
        surface = Color(0xFF1E1E1E),
        surfaceElevated = Color(0xFF262626),
        surfaceHighlight = Color(0xFF383838),
        primaryAccent = Color(0xFFFFFFFF),
        secondaryAccent = Color(0xFFD4D4D4),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA3A3A3),
        textTertiary = Color(0xFF666666),
        playerBackground = Color(0xFF0A0A0A),
        miniPlayerBackground = Color(0xFF181818),
        controlActive = Color(0xFFFFFFFF),
        controlInactive = Color(0xFF666666),
        sliderTrack = Color(0xFF383838),
        sliderThumb = Color(0xFFFFFFFF),
        divider = Color(0xFF262626)
    )

    /**
     * Theme 2 — Frosted Glass
     * Translucent surfaces, frosted glass blur, glass-like navigation & player surfaces, layered depth.
     */
    val FrostedGlass = StereoColors(
        id = "frosted_glass",
        name = "Frosted Glass",
        isDark = true,
        background = Color(0xFF0E131F),
        backgroundSecondary = Color(0xFF141B2D),
        surface = Color(0xFF1D263B),
        surfaceElevated = Color(0xFF28344E),
        surfaceHighlight = Color(0xFF3B4D70),
        primaryAccent = Color(0xFF64B5F6),
        secondaryAccent = Color(0xFF80D8FF),
        textPrimary = Color(0xFFF0F4F8),
        textSecondary = Color(0xFFA6B8CE),
        textTertiary = Color(0xFF6A7B95),
        playerBackground = Color(0xFF0E131F),
        miniPlayerBackground = Color(0xFF1A2234),
        controlActive = Color(0xFF80D8FF),
        controlInactive = Color(0xFF6A7B95),
        sliderTrack = Color(0xFF3B4D70),
        sliderThumb = Color(0xFF80D8FF),
        divider = Color(0xFF28344E)
    )

    /**
     * Theme 3 — Black & Yellow
     * Deep black or charcoal backgrounds, bright yellow interactive accents.
     */
    val BlackAndYellow = StereoColors(
        id = "black_yellow",
        name = "Black & Yellow",
        isDark = true,
        background = Color(0xFF0C0C0D),
        backgroundSecondary = Color(0xFF141416),
        surface = Color(0xFF1C1C20),
        surfaceElevated = Color(0xFF26262B),
        surfaceHighlight = Color(0xFF36363D),
        primaryAccent = Color(0xFFFFD600),
        secondaryAccent = Color(0xFFFFEA00),
        textPrimary = Color(0xFFFDFDFD),
        textSecondary = Color(0xFFB8B8BD),
        textTertiary = Color(0xFF7A7A80),
        playerBackground = Color(0xFF0C0C0D),
        miniPlayerBackground = Color(0xFF19191C),
        controlActive = Color(0xFFFFD600),
        controlInactive = Color(0xFF7A7A80),
        sliderTrack = Color(0xFF36363D),
        sliderThumb = Color(0xFFFFD600),
        divider = Color(0xFF26262B)
    )

    /**
     * Theme 4 — Bluey
     * Deep navy surfaces, rich blue gradients, electric/icy blue accents.
     */
    val Bluey = StereoColors(
        id = "bluey",
        name = "Bluey",
        isDark = true,
        background = Color(0xFF070D1E),
        backgroundSecondary = Color(0xFF0C162E),
        surface = Color(0xFF121F40),
        surfaceElevated = Color(0xFF1A2C56),
        surfaceHighlight = Color(0xFF263D73),
        primaryAccent = Color(0xFF00B4D8),
        secondaryAccent = Color(0xFF90E0EF),
        textPrimary = Color(0xFFE8F4F8),
        textSecondary = Color(0xFF9AB8CC),
        textTertiary = Color(0xFF5C7C94),
        playerBackground = Color(0xFF070D1E),
        miniPlayerBackground = Color(0xFF101C38),
        controlActive = Color(0xFF00B4D8),
        controlInactive = Color(0xFF5C7C94),
        sliderTrack = Color(0xFF263D73),
        sliderThumb = Color(0xFF00B4D8),
        divider = Color(0xFF1A2C56)
    )

    /**
     * Theme 5 — Starry Glow
     * Dark night-sky background, subtle star-like elements, soft ambient glows.
     */
    val StarryGlow = StereoColors(
        id = "starry_glow",
        name = "Starry Glow",
        isDark = true,
        background = Color(0xFF080812),
        backgroundSecondary = Color(0xFF0F1020),
        surface = Color(0xFF171830),
        surfaceElevated = Color(0xFF222442),
        surfaceHighlight = Color(0xFF343760),
        primaryAccent = Color(0xFF9D4EDD),
        secondaryAccent = Color(0xFFC77DFF),
        textPrimary = Color(0xFFF3E8FF),
        textSecondary = Color(0xFFB4A0D4),
        textTertiary = Color(0xFF736094),
        playerBackground = Color(0xFF080812),
        miniPlayerBackground = Color(0xFF141528),
        controlActive = Color(0xFFC77DFF),
        controlInactive = Color(0xFF736094),
        sliderTrack = Color(0xFF343760),
        sliderThumb = Color(0xFFC77DFF),
        divider = Color(0xFF222442)
    )

    /**
     * Theme 6 — Ivory & Beige
     * Warm light theme, soft white and cream backgrounds, beige and muted warm accents.
     */
    val IvoryAndBeige = StereoColors(
        id = "ivory_beige",
        name = "Ivory & Beige",
        isDark = false,
        background = Color(0xFFFBF8F3),
        backgroundSecondary = Color(0xFFF4EDE2),
        surface = Color(0xFFEAE1D2),
        surfaceElevated = Color(0xFFDFD4C1),
        surfaceHighlight = Color(0xFFCFC2AC),
        primaryAccent = Color(0xFF9C6644),
        secondaryAccent = Color(0xFFB08968),
        textPrimary = Color(0xFF2D251E),
        textSecondary = Color(0xFF68594E),
        textTertiary = Color(0xFF968476),
        playerBackground = Color(0xFFFBF8F3),
        miniPlayerBackground = Color(0xFFF0E7D8),
        controlActive = Color(0xFF9C6644),
        controlInactive = Color(0xFF968476),
        sliderTrack = Color(0xFFCFC2AC),
        sliderThumb = Color(0xFF9C6644),
        divider = Color(0xFFDFD4C1)
    )

    /**
     * Theme 7 — Pure Black
     * OLED-oriented true black background, subtle gray surfaces, high contrast.
     */
    val PureBlack = StereoColors(
        id = "pure_black",
        name = "Pure Black",
        isDark = true,
        background = Color(0xFF000000),
        backgroundSecondary = Color(0xFF080808),
        surface = Color(0xFF121212),
        surfaceElevated = Color(0xFF1B1B1B),
        surfaceHighlight = Color(0xFF262626),
        primaryAccent = Color(0xFFE58B24),
        secondaryAccent = Color(0xFFD4AF37),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFB3B3B3),
        textTertiary = Color(0xFF707070),
        playerBackground = Color(0xFF000000),
        miniPlayerBackground = Color(0xFF101010),
        controlActive = Color(0xFFE58B24),
        controlInactive = Color(0xFF707070),
        sliderTrack = Color(0xFF222222),
        sliderThumb = Color(0xFFE58B24),
        divider = Color(0xFF181818)
    )

    // ========================================================================
    // CLASSIC STEREO PALETTES
    // ========================================================================

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

    val CassetteSunset = StereoColors(
        id = "cassette_sunset",
        name = "Cassette Sunset",
        isDark = true,
        background = Color(0xFF130E14),
        backgroundSecondary = Color(0xFF1D141F),
        surface = Color(0xFF281A2B),
        surfaceElevated = Color(0xFF35223A),
        surfaceHighlight = Color(0xFF472D4D),
        primaryAccent = Color(0xFFF97316),
        secondaryAccent = Color(0xFFE11D48),
        textPrimary = Color(0xFFFFF1F2),
        textSecondary = Color(0xFFBEAABF),
        textTertiary = Color(0xFF7A687B),
        playerBackground = Color(0xFF130E14),
        miniPlayerBackground = Color(0xFF221625),
        controlActive = Color(0xFFF97316),
        controlInactive = Color(0xFF7A687B),
        sliderTrack = Color(0xFF472D4D),
        sliderThumb = Color(0xFFF97316),
        divider = Color(0xFF35223A)
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
     * The 7 Official STEREO Themes required by Directive Phase C.
     */
    val officialThemes: List<StereoColors> = listOf(
        MonochromeMinimal,
        FrostedGlass,
        BlackAndYellow,
        Bluey,
        StarryGlow,
        IvoryAndBeige,
        PureBlack
    )

    /**
     * All built-in presets in display order.
     */
    val allPresets: List<StereoColors> = officialThemes + listOf(
        MidnightStereo,
        VintageCream,
        BurgundyVinyl,
        OliveRadio,
        CassetteSunset,
        PaperAndInk
    )

    fun getById(id: String?): StereoColors {
        return allPresets.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: MidnightStereo
    }
}
