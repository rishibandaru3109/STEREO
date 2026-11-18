package com.nova.music.testing

/**
 * Marker interface for tests requiring an active internet connection
 * and external network access to live third-party services (e.g. YouTube Music, Google Video CDN).
 *
 * These tests are preserved in a separately runnable suite (`./gradlew testLiveIntegration`
 * or `./gradlew testDebugUnitTest -PincludeLiveTests=true`) to verify live streaming integration,
 * but are excluded from deterministic CI/unit test builds to prevent flaky failures caused by
 * data-center IP blocking, rate limits, or transient network drops.
 */
interface LiveNetworkTest
