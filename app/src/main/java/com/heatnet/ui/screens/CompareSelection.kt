package com.heatnet.ui.screens

import com.heatnet.data.model.SessionSummary

/**
 * Rules for picking exactly two distinct sessions, including sessions of different connection types.
 */
object CompareSelection {
    const val REQUIRED = 2

    fun canSelect(selected: List<SessionSummary>, candidate: SessionSummary): Boolean {
        if (selected.any { it.id == candidate.id }) return true
        return selected.size < REQUIRED
    }

    /** Tapping an already-selected session deselects it; otherwise it is added when allowed. */
    fun toggle(selected: List<SessionSummary>, tapped: SessionSummary): List<SessionSummary> = when {
        selected.any { it.id == tapped.id } -> selected.filterNot { it.id == tapped.id }
        canSelect(selected, tapped) -> selected + tapped
        else -> selected
    }

    /** (before, after) ids with the older session first, or null until two are picked. */
    fun orderedIds(selected: List<SessionSummary>): Pair<Long, Long>? {
        if (selected.size != REQUIRED) return null
        val (older, newer) = selected.sortedBy { it.createdAt }
        return older.id to newer.id
    }
}
