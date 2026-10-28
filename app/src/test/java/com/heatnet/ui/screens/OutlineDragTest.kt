package com.heatnet.ui.screens

import com.heatnet.data.db.SessionRepository
import com.heatnet.data.model.Point
import com.heatnet.measurement.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class OutlineDragTest {
    private fun editor() = OutlineViewModel(mock(SessionRepository::class.java), ConnectionType.WIFI)

    @Test fun draggingCanSnapToTheVisibleGrid() {
        val vm = editor()
        vm.corners.add(Point(10f, 10f))
        vm.snapToGrid = true
        vm.moveCorner(0, Point(31f, 51f), 200f, 200f, 24f)
        assertEquals(Point(24f, 48f), vm.corners[0])
    }

    @Test fun draggingWithoutGridKeepsTheRequestedPosition() {
        val vm = editor()
        vm.corners.add(Point(10f, 10f))
        vm.moveCorner(0, Point(31f, 51f), 200f, 200f, 24f)
        assertEquals(Point(31f, 51f), vm.corners[0])
    }

    @Test fun dragRejectsCrossingEdges() {
        val vm = editor()
        vm.corners.addAll(listOf(Point(10f, 10f), Point(100f, 10f), Point(100f, 100f), Point(10f, 100f)))
        vm.moveCorner(1, Point(50f, 150f), 200f, 200f, 24f)
        assertEquals(Point(100f, 10f), vm.corners[1])
    }
}
