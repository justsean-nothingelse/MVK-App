package com.seanpereyra.mvk.scan

/**
 * Turns the raw OCR lines of a voucher slip into fields.
 *
 * Expected slip layout:
 *   CICTLAB [153]
 *   Voucher Code
 *   FWxWU
 *   6h
 */
object VoucherParser {

    data class Result(
        val code: String,
        val network: String,
        val slot: String,
        val duration: String,
    )

    const val CODE_LENGTH = 5

    private val headerRx = Regex("""([A-Za-z]{3,})\s*\[?\s*(\d{1,5})\s*]""")
    private val slotRx = Regex("""\[\s*(\d{1,5})\s*]""")
    private val durationRx = Regex("""\b(\d{1,3}\s*[dh](\s*\d{1,2}\s*m)?(\s*\d{1,2}\s*s)?|\d{1,3}\s*m(\s*\d{1,2}\s*s)?|\d{1,3}\s*s)\b""", RegexOption.IGNORE_CASE)
    private val codeRx = Regex("""^[A-Za-z0-9]{$CODE_LENGTH}$""")
    private val stopWords = setOf("VOUCH", "CODE", "VOUCHER", "WIFI", "HOURS", "VALID")

    fun parse(lines: List<String>): Result? {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
        if (clean.isEmpty()) return null
        val all = clean.joinToString("\n")

        val header = headerRx.find(all)
        val network = header?.groupValues?.get(1)?.uppercase()
            ?: clean.firstOrNull { it.length >= 4 && it.all(Char::isLetter) && it.uppercase() !in stopWords && it.uppercase() == it }
            ?: ""
        val slot = header?.groupValues?.get(2) ?: slotRx.find(all)?.groupValues?.get(1) ?: ""

        // Look for the code on the lines right after "Voucher Code" first.
        val labelIdx = clean.indexOfFirst { it.contains("voucher", true) || it.contains("code", true) }
        // Don't grab random words: only trust text that actually looks like a voucher slip.
        if (labelIdx < 0 && header == null) return null
        val searchOrder = if (labelIdx >= 0) clean.drop(labelIdx + 1) + clean.take(labelIdx + 1) else clean

        val code = searchOrder.asSequence()
            .flatMap { line -> line.split(Regex("""[\s|\[\]()]+""")).asSequence() }
            .map { it.trim('.', ',', ':', ';', '-', '_') }
            .firstOrNull { tok ->
                codeRx.matches(tok) &&
                    tok.uppercase() !in stopWords &&
                    !tok.equals(network, true) &&
                    !durationRx.matches(tok) &&
                    !tok.all(Char::isDigit) // slot numbers / pure digits are unlikely codes
            } ?: return null

        val duration = clean
            .filterNot { it.contains(code) }
            .firstNotNullOfOrNull { line -> durationRx.find(line)?.value?.replace(" ", "")?.lowercase() }
            ?: ""

        return Result(code = code, network = network, slot = slot, duration = duration)
    }
}
