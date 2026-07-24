package com.healthwidget.core.tips

/**
 * A single tip together with the research backing it, so the UI can show users *why* a tip is
 * shown, not just the tip itself. Every bundled tip is required to have a real citation (see
 * `TipCatalogTest`) rather than leaving [sourceLabel]/[sourceUrl] nullable — see `TIP_SOURCES.md`
 * at the repo root for the underlying research this data traces back to.
 */
data class Tip(
    val text: String,
    val sourceLabel: String,
    val sourceUrl: String,
)
