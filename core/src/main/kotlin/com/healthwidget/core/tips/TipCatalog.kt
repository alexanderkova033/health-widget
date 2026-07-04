package com.healthwidget.core.tips

/**
 * Bundled tip content, grouped by [DayPart]. Loaded from plain-text resources (one tip per
 * line, `#` for comments) rather than JSON: the catalog is a handful of static string lists,
 * so a hand-rolled line reader avoids pulling a JSON parsing dependency into a module whose
 * whole point is to stay dependency-free and trivially JVM-testable.
 */
data class TipCatalog(
    val general: List<String>,
    val morning: List<String>,
    val afternoon: List<String>,
    val evening: List<String>,
    val sleepLate: String,
    val sleepEarlyHours: String,
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

        private fun loadPool(fileName: String): List<String> =
            resourceLines(fileName).also {
                require(it.isNotEmpty()) { "Tip pool 'tips/$fileName' must not be empty" }
            }

        private fun loadSingle(fileName: String): String {
            val lines = resourceLines(fileName)
            require(lines.size == 1) {
                "Tip resource 'tips/$fileName' must contain exactly one message, found ${lines.size}"
            }
            return lines.first()
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
