package com.example.interviewassistant.feature.interviewassistant.data.remote.ocr

/**
 * Prepares OCR text for HTML preview by converting common LaTeX / Markdown fragments
 * that PaddleOCR embeds into readable HTML.
 */
object OcrDisplayHtml {
    /**
     * Builds a full HTML document for preview widgets.
     *
     * @param content Raw OCR text (HTML fragment, Markdown-ish, or mixed LaTeX).
     * @param baseUri Optional `file://` base used to resolve cached OCR images.
     */
    fun document(content: String, baseUri: String? = null): String {
        val normalized = normalize(content)
        val baseTag = baseUri
            ?.takeIf(String::isNotBlank)
            ?.let { """<base href="$it"/>""" }
            .orEmpty()
        if (normalized.contains("<html", ignoreCase = true)) {
            return if (baseTag.isEmpty() || normalized.contains("<base", ignoreCase = true)) {
                normalized
            } else {
                normalized.replaceFirst(
                    Regex("<head([^>]*)>", RegexOption.IGNORE_CASE),
                    "<head$1>$baseTag",
                )
            }
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
              $baseTag
              <meta charset="utf-8"/>
              <style>
                body { font-family: "Microsoft YaHei", sans-serif; padding: 16px; line-height: 1.6; color: #1f1f1f; }
                table { border-collapse: collapse; width: 100%; margin: 12px 0; }
                td, th { border: 1px solid #cfcfcf; padding: 8px; vertical-align: top; }
                img { max-width: 100%; height: auto; }
                h1, h2, h3 { margin: 0.6em 0 0.35em; }
                h1 { font-size: 1.4em; }
                h2 { font-size: 1.25em; }
                h3 { font-size: 1.1em; }
                u { text-decoration: underline; }
                div { margin: 0.5em 0; }
              </style>
            </head>
            <body>$normalized</body>
            </html>
        """.trimIndent()
    }

    /**
     * Converts OCR-specific LaTeX / light Markdown into HTML tags and preserves line breaks.
     */
    fun normalize(content: String): String {
        var result = content.trim().replace("\r\n", "\n").replace('\r', '\n')
        result = UNDERLINE_TEXT.replace(result) { match ->
            "<u>${match.groupValues[1]}</u>"
        }
        result = UNDERLINE_PLAIN.replace(result) { match ->
            "<u>${match.groupValues[1]}</u>"
        }
        result = TEXTBF.replace(result) { match ->
            "<strong>${match.groupValues[1]}</strong>"
        }
        result = TEXTIT.replace(result) { match ->
            "<em>${match.groupValues[1]}</em>"
        }
        result = TEXT_ONLY.replace(result) { match ->
            match.groupValues[1]
        }
        // Drop leftover inline-math delimiters around already-converted or plain text.
        result = INLINE_MATH.replace(result) { match ->
            match.groupValues[1].trim()
        }
        result = convertMarkdownHeaders(result)
        result = convertNewlinesOutsideTags(result)
        result = breakGluedNumberedItems(result)
        return result
    }

    /**
     * Returns a plain-text snippet suitable for list-card previews.
     */
    fun plainPreview(content: String, maxLength: Int = 160): String {
        val plain = normalize(content)
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("</(h[1-6]|div|tr)>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("&lt;", RegexOption.IGNORE_CASE), "<")
            .replace(Regex("&gt;", RegexOption.IGNORE_CASE), ">")
            .replace(Regex("&amp;", RegexOption.IGNORE_CASE), "&")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (plain.length <= maxLength) plain else plain.take(maxLength).trimEnd() + "…"
    }

    private fun convertMarkdownHeaders(input: String): String {
        return input.lineSequence().joinToString("\n") { line ->
            when {
                HEADER_3.matches(line) -> "<h3>${HEADER_3.matchEntire(line)!!.groupValues[1]}</h3>"
                HEADER_2.matches(line) -> "<h2>${HEADER_2.matchEntire(line)!!.groupValues[1]}</h2>"
                HEADER_1.matches(line) -> "<h1>${HEADER_1.matchEntire(line)!!.groupValues[1]}</h1>"
                else -> line
            }
        }
    }

    /**
     * HTML collapses plain `\n`; convert them to `<br/>` outside of tags so OCR line breaks survive.
     */
    private fun convertNewlinesOutsideTags(input: String): String {
        val output = StringBuilder(input.length)
        var inTag = false
        var index = 0
        while (index < input.length) {
            val char = input[index]
            when {
                char == '<' -> {
                    inTag = true
                    output.append(char)
                }
                char == '>' -> {
                    inTag = false
                    output.append(char)
                }
                !inTag && char == '\n' -> {
                    // Avoid redundant breaks directly against block-level tags.
                    if (!endsWithBlockTag(output) && !startsWithBlockTag(input, index + 1)) {
                        output.append("<br/>")
                    }
                }
                else -> output.append(char)
            }
            index += 1
        }
        return output.toString()
    }

    private fun endsWithBlockTag(buffer: StringBuilder): Boolean {
        val tail = buffer.takeLast(48).toString().lowercase().trimEnd()
        return BLOCK_CLOSE_TAGS.any(tail::endsWith) ||
            BLOCK_OPEN_TAGS.any { tag ->
                Regex("""<$tag\b[^>]*>$""", RegexOption.IGNORE_CASE).containsMatchIn(tail)
            }
    }

    private fun startsWithBlockTag(input: String, start: Int): Boolean {
        val rest = input.substring(start).trimStart().lowercase()
        return BLOCK_OPEN_TAGS.any { tag -> rest.startsWith("<$tag") } ||
            BLOCK_CLOSE_TAGS.any(rest::startsWith)
    }

    /**
     * OCR often keeps `1.` / `2.` on the same line after `。` / `；`; force a visual break.
     */
    private fun breakGluedNumberedItems(input: String): String {
        return GLUED_NUMBERED_ITEM.replace(input) { match ->
            "${match.groupValues[1]}<br/>${match.groupValues[2]}"
        }
    }

    private fun StringBuilder.takeLast(count: Int): CharSequence {
        val start = (length - count).coerceAtLeast(0)
        return subSequence(start, length)
    }

    private val UNDERLINE_TEXT =
        Regex("""\$\s*\\underline\{\s*\\text\{([\s\S]*?)\}\s*\}\s*\$""")
    private val UNDERLINE_PLAIN =
        Regex("""\$\s*\\underline\{\s*([\s\S]*?)\s*\}\s*\$""")
    private val TEXTBF =
        Regex("""\\textbf\{\s*([\s\S]*?)\s*\}""")
    private val TEXTIT =
        Regex("""\\textit\{\s*([\s\S]*?)\s*\}""")
    private val TEXT_ONLY =
        Regex("""\\text\{\s*([\s\S]*?)\s*\}""")
    private val INLINE_MATH =
        Regex("""\$\s*([^$]+?)\s*\$""")
    private val HEADER_1 = Regex("""^#\s+(.+)$""")
    private val HEADER_2 = Regex("""^##\s+(.+)$""")
    private val HEADER_3 = Regex("""^###\s+(.+)$""")
    private val GLUED_NUMBERED_ITEM =
        Regex("""([。！？；;])\s*(\d+\.\s+)""")
    private val BLOCK_OPEN_TAGS = listOf("h1", "h2", "h3", "div", "p", "table", "tr", "ul", "ol", "li")
    private val BLOCK_CLOSE_TAGS = listOf(
        "</h1>", "</h2>", "</h3>", "</div>", "</p>", "</table>", "</tr>", "</ul>", "</ol>", "</li>",
    )
}
