package com.heatnet.ui.screens

import com.heatnet.data.model.SessionSummary

/**
 * Rules for picking two sessions to compare (PRD F7): exactly two, and both on the same connection
 * type. The first pick fixes the type; sessions of the other type cannot be added.
 */
object CompareSelection {
    const val REQUIRED = 2

    fun canSelect(selected: List<SessionSummary>, candidate: SessionSummary): Boolean {
        if (selected.any { it.id == candidate.id }) return true
        return selected.size < REQUIRED && selected.all { it.connectionType == candidate.connectionType }
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
