package com.example.engine

object ValidationUtils {

    /**
     * Parses page range expressions like "1-3, 5, 8-10" into a sorted list of 1-based page numbers.
     * Throws [IllegalArgumentException] with clean, localized-friendly messages for invalid syntax or ranges out of bounds.
     */
    fun parsePageRanges(input: String, totalPages: Int): List<Int> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Page range cannot be empty.")
        }
        if (totalPages <= 0) {
            throw IllegalArgumentException("Invalid total page count ($totalPages).")
        }

        val pages = mutableSetOf<Int>()
        val segments = trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        if (segments.isEmpty()) {
            throw IllegalArgumentException("Please enter a valid page range (e.g. 1-3, 5).")
        }

        for (segment in segments) {
            if (segment.contains("-")) {
                val parts = segment.split("-").map { it.trim() }
                if (parts.size != 2) {
                    throw IllegalArgumentException("Invalid range format '$segment'. Use format 'start-end'.")
                }
                val start = parts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid page number '${parts[0]}'.")
                val end = parts[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid page number '${parts[1]}'.")

                if (start < 1) {
                    throw IllegalArgumentException("Page numbers must be 1 or greater. Found: $start.")
                }
                if (end > totalPages) {
                    throw IllegalArgumentException("Page $end exceeds total pages ($totalPages).")
                }
                if (start > end) {
                    throw IllegalArgumentException("Start page $start cannot be greater than end page $end.")
                }

                for (p in start..end) {
                    pages.add(p)
                }
            } else {
                val page = segment.toIntOrNull() ?: throw IllegalArgumentException("Invalid page number '$segment'.")
                if (page < 1) {
                    throw IllegalArgumentException("Page numbers must be 1 or greater. Found: $page.")
                }
                if (page > totalPages) {
                    throw IllegalArgumentException("Page $page exceeds total pages ($totalPages).")
                }
                pages.add(page)
            }
        }

        return pages.sorted()
    }

    fun validatePassword(password: String, confirmPassword: String? = null): Boolean {
        if (password.isEmpty()) {
            throw IllegalArgumentException("Password cannot be empty.")
        }
        if (confirmPassword != null && password != confirmPassword) {
            throw IllegalArgumentException("Passwords do not match.")
        }
        return true
    }
}
