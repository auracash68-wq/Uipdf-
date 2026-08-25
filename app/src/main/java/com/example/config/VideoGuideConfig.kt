package com.example.config

object VideoGuideConfig {
    /**
     * Central placeholder YouTube video URL for the guide.
     * Easily replaceable by the developer in one single place.
     */
    const val DEFAULT_GUIDE_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

    // Per-tool video URLs mapping (allowing individual video tutorials per tool)
    private val toolGuideUrls = mapOf(
        "merge" to DEFAULT_GUIDE_URL,
        "split" to DEFAULT_GUIDE_URL,
        "image_to_pdf" to DEFAULT_GUIDE_URL,
        "text_to_pdf" to DEFAULT_GUIDE_URL,
        "lock" to DEFAULT_GUIDE_URL,
        "unlock" to DEFAULT_GUIDE_URL,
        "compress" to DEFAULT_GUIDE_URL,
        "sign" to DEFAULT_GUIDE_URL,
        "extract" to DEFAULT_GUIDE_URL,
        "rotate" to DEFAULT_GUIDE_URL
    )

    fun getVideoUrlForTool(toolKey: String): String {
        return toolGuideUrls[toolKey] ?: DEFAULT_GUIDE_URL
    }

    /**
     * Extracts video ID from YouTube URL (supports standard, short, embed formats).
     */
    fun extractYouTubeVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val pattern = "^(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})(?:.*)?$"
        val matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(trimmed)
        return if (matcher.matches()) {
            matcher.group(1)
        } else {
            // fallback if it's already an 11-char ID
            if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains(".")) trimmed else null
        }
    }
}
