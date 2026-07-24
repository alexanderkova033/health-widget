package com.healthwidget.core.tips

/**
 * Bundled tip content, grouped by [DayPart]. Loaded from plain-text resources (one tip per
 * line, `#` for comments) rather than JSON: the catalog is a handful of static string lists,
 * so a hand-rolled line reader avoids pulling a JSON parsing dependency into a module whose
 * whole point is to stay dependency-free and trivially JVM-testable.
 *
 * Each `tips/<name>.txt` has a companion `tips/<name>_sources.txt` of the same length, one
 * citation per line as `Label<TAB>URL`, zipped in line-for-line — see `TIP_SOURCES.md` at the
 * repo root for the research those citations trace back to.
 */
data class TipCatalog(
    val general: List<Tip>,
    val morning: List<Tip>,
    val afternoon: List<Tip>,
    val evening: List<Tip>,
    val sleepLate: Tip,
    val sleepEarlyHours: Tip,
) {
    companion object {
        fun loadDefault(): TipCatalog =
            TipCatalog(
                general = loadPool("general.txt"),
                morning = loadPool("morning.txt"),
                afternoon = loadPool("afternoon.txt"),
                evening = loadPool("evening.txt"),
                sleepLate = loadSingle("sleep_late.txt"),
                sleepEarlyHours = loadSingle("sleep_early.txt"),
            )

        private fun loadPool(fileName: String): List<Tip> {
            val texts =
                resourceLines(fileName).also {
                    require(it.isNotEmpty()) { "Tip pool 'tips/$fileName' must not be empty" }
                }
            return zipWithSources(fileName, texts)
        }

        private fun loadSingle(fileName: String): Tip {
            val texts = resourceLines(fileName)
            require(texts.size == 1) {
                "Tip resource 'tips/$fileName' must contain exactly one message, found ${texts.size}"
            }
            return zipWithSources(fileName, texts).first()
        }

        private fun zipWithSources(
            fileName: String,
            texts: List<String>,
        ): List<Tip> {
            val sourceFileName = sourceFileNameFor(fileName)
            val sources = resourceLines(sourceFileName)
            require(sources.size == texts.size) {
                "'tips/$fileName' has ${texts.size} tips but 'tips/$sourceFileName' has " +
                    "${sources.size} source entries — they must match line-for-line"
            }
            return texts.zip(sources) { text, sourceLine -> text.toTip(sourceLine, sourceFileName) }
        }

        private fun sourceFileNameFor(fileName: String) = fileName.removeSuffix(".txt") + "_sources.txt"

        private fun String.toTip(
            sourceLine: String,
            sourceFileName: String,
        ): Tip {
            val parts = sourceLine.split("\t")
            require(parts.size == 2) {
                "Malformed line in 'tips/$sourceFileName': \"$sourceLine\" (expected \"Label<TAB>URL\")"
            }
            return Tip(text = this, sourceLabel = parts[0], sourceUrl = parts[1])
        }

        private fun resourceLines(fileName: String): List<String> {
            val stream =
                requireNotNull(TipCatalog::class.java.getResourceAsStream("/tips/$fileName")) {
                    "Missing bundled tip resource: tips/$fileName"
                }
            return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toList()
            }
        }
    }
}
