package com.seanpereyra.mvk.scan

import kotlin.math.abs

/** One line of recognized text with its position on the screenshot (pixels). */
data class OcrLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val cx: Float get() = (left + right) / 2f
    val cy: Float get() = (top + bottom) / 2f
    val h: Int get() = (bottom - top).coerceAtLeast(1)
    val w: Int get() = (right - left).coerceAtLeast(1)
}

data class ImportedVoucher(
    val code: String,
    val network: String,
    val slot: String,
    val duration: String,
    val used: Boolean,
)

/**
 * Reads every voucher ticket out of a screenshot of an MVK library (2-column grid).
 *
 * Each ticket is anchored on its "Voucher Code" label: the code is the line right below it,
 * the time is the line below that, and the "CICTLAB [153]" header is the row right above it.
 * Positions keep the two columns apart. A red "USED" stamp inside a ticket marks it used.
 */
object ScreenshotImport {
    private val labelRx = Regex("""vou?cher\s*c[o0]d""", RegexOption.IGNORE_CASE)
    private val codeRx = Regex("""^[A-Za-z0-9]{4,12}$""")
    private val slotRx = Regex("""[\[(]?\s*(\d{1,5})\s*[\])]""")
    private val digitsRx = Regex("""\b(\d{1,5})\b""")
    private val networkRx = Regex("""[A-Za-z]{3,}""")
    private val notCodes = setOf("USED", "CODE", "VOUCHER", "LIBRARY", "SCAN", "RANDOM", "AVAILABLE")

    fun parse(lines: List<OcrLine>): List<ImportedVoucher> {
        val out = mutableListOf<ImportedVoucher>()
        for (label in lines.filter { labelRx.containsMatchIn(it.text) }) {
            // Code and time are centered under the label; the header spans the full ticket width.
            val narrow = label.w * 0.8f
            val wide = label.w * 1.25f
            fun inNarrow(o: OcrLine) = abs(o.cx - label.cx) <= narrow
            fun inWide(o: OcrLine) = abs(o.cx - label.cx) <= wide

            val below = lines
                .filter { it !== label && inNarrow(it) && it.cy > label.cy && it.top - label.bottom < label.h * 4 }
                .sortedBy { it.top }
            val codeLine = below.firstOrNull { cleanCode(it.text) != null && !labelRx.containsMatchIn(it.text) } ?: continue
            val code = cleanCode(codeLine.text)!!

            val durLine = below.firstOrNull { it !== codeLine && it.cy > codeLine.bottom && it.top - codeLine.bottom < codeLine.h * 3 }
            val duration = durLine
                ?.let { VoucherParser.durationRx.find(it.text)?.value }
                ?.replace(" ", "")?.lowercase() ?: ""

            val above = lines.filter { it !== label && inWide(it) && it.cy < label.cy && label.top - it.bottom < label.h * 4 }
            val rowY = above.maxOfOrNull { it.cy }
            val header = if (rowY == null) emptyList() else above.filter { abs(it.cy - rowY) < it.h * 0.7f }.sortedBy { it.left }
            val headerText = header.joinToString(" ") { it.text }
            val slot = slotRx.find(headerText)?.groupValues?.get(1)
                ?: digitsRx.find(headerText)?.groupValues?.get(1) ?: ""
            val network = networkRx.find(headerText)?.value?.uppercase() ?: ""

            val ticketTop = header.minOfOrNull { it.top } ?: (label.top - label.h * 3)
            val ticketBottom = (durLine ?: codeLine).bottom + label.h
            val used = lines.any {
                it.text.replace(" ", "").equals("USED", ignoreCase = true) &&
                    inWide(it) && it.cy >= ticketTop && it.cy <= ticketBottom
            }
            out += ImportedVoucher(code, network, slot, duration, used)
        }
        return out.distinctBy { it.code }
    }

    fun cleanCode(text: String): String? {
        val s = text.replace(" ", "")
            .replace('Ø', '0').replace('ø', '0')
            .trim('|', '[', ']', '(', ')', '.', ',', ':', ';')
        return s.takeIf { codeRx.matches(it) && !it.all(Char::isDigit) && it.uppercase() !in notCodes }
    }
}
