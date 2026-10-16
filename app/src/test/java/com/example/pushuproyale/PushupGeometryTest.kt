package com.example.pushuproyale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushupGeometryTest {

    @Test
    fun testComputeAngleRightAngle() {
        val p1 = Point2D(0f, 10f)
        val p2 = Point2D(0f, 0f)
        val p3 = Point2D(10f, 0f)
        val angle = PushupAnalyzer.computeAngle(p1, p2, p3)
        assertEquals(90.0, angle, 0.01)
    }

    @Test
    fun testComputeAngleStraightLine() {
        val p1 = Point2D(-10f, 0f)
        val p2 = Point2D(0f, 0f)
        val p3 = Point2D(10f, 0f)
        val angle = PushupAnalyzer.computeAngle(p1, p2, p3)
        assertEquals(180.0, angle, 0.01)
    }

    @Test
    fun testComputeAngleAcute() {
        val p1 = Point2D(10f, 10f)
        val p2 = Point2D(0f, 0f)
        val p3 = Point2D(10f, 0f)
        val angle = PushupAnalyzer.computeAngle(p1, p2, p3)
        assertEquals(45.0, angle, 0.01)
    }

    @Test
    fun testComputeAngleAlwaysBetweenZeroAnd180() {
        val p1 = Point2D(10f, -10f)
        val p2 = Point2D(0f, 0f)
        val p3 = Point2D(10f, 10f)
        val angle = PushupAnalyzer.computeAngle(p1, p2, p3)
        assertTrue(angle in 0.0..180.0)
        assertEquals(90.0, angle, 0.01)
    }

    @Test
    fun testPlankValidWithStraightPose() {
        val shoulder = Point2D(100f, 100f)
        val hip      = Point2D(100f, 200f)
        val ankle    = Point2D(100f, 300f)
        assertTrue("Straight vertical/horizontal line should pass plank check",
            PushupAnalyzer.isPlankValid(shoulder, hip, ankle))
    }

    @Test
    fun testPlankInvalidWithSaggingOrPikeHips() {
        // Hip bent at 90 degrees
        val shoulder = Point2D(100f, 100f)
        val hip      = Point2D(100f, 200f)
        val ankle    = Point2D(200f, 200f)
        assertFalse("90-degree hip angle should fail plank check",
            PushupAnalyzer.isPlankValid(shoulder, hip, ankle))
    }

    @Test
    fun testPlankInvalidWhenAnyLandmarkMissing() {
        val validPoint = Point2D(100f, 100f)
        assertFalse("Missing shoulder must strictly fail plank check",
            PushupAnalyzer.isPlankValid(null, validPoint, validPoint))
        assertFalse("Missing hip must strictly fail plank check",
            PushupAnalyzer.isPlankValid(validPoint, null, validPoint))
        assertFalse("Missing leg must strictly fail plank check",
            PushupAnalyzer.isPlankValid(validPoint, validPoint, null))
        assertFalse("Missing all landmarks must fail plank check",
            PushupAnalyzer.isPlankValid(null, null, null))
    }
}
