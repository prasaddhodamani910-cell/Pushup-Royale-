package com.example.pushuproyale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RanksLogicTest {

    @Test
    fun testInitialBronzeRank() {
        val info = Ranks.info(0)
        assertEquals("Bronze", info.name)
        assertEquals("🥉", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(500, info.xpToNext)
        assertEquals(0, info.tierIndex)
    }

    @Test
    fun testBronzeMidProgress() {
        val info = Ranks.info(250)
        assertEquals("Bronze", info.name)
        assertEquals("🥉", info.emoji)
        assertEquals(0.5f, info.progress, 0.001f)
        assertEquals(250, info.xpToNext)
        assertEquals(0, info.tierIndex)
    }

    @Test
    fun testSilverRank() {
        val info = Ranks.info(500)
        assertEquals("Silver", info.name)
        assertEquals("🥈", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(1000, info.xpToNext)
        assertEquals(1, info.tierIndex)
    }

    @Test
    fun testGoldRank() {
        val info = Ranks.info(1500)
        assertEquals("Gold", info.name)
        assertEquals("🥇", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(2000, info.xpToNext)
        assertEquals(2, info.tierIndex)
    }

    @Test
    fun testPlatinumRank() {
        val info = Ranks.info(3500)
        assertEquals("Platinum", info.name)
        assertEquals("💎", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(3500, info.xpToNext)
        assertEquals(3, info.tierIndex)
    }

    @Test
    fun testDiamondRank() {
        val info = Ranks.info(7000)
        assertEquals("Diamond", info.name)
        assertEquals("💠", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(5000, info.xpToNext)
        assertEquals(4, info.tierIndex)
    }

    @Test
    fun testCrownRank() {
        val info = Ranks.info(12000)
        assertEquals("Crown", info.name)
        assertEquals("👑", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(8000, info.xpToNext)
        assertEquals(5, info.tierIndex)
    }

    @Test
    fun testAceRank() {
        val info = Ranks.info(20000)
        assertEquals("Ace", info.name)
        assertEquals("🃏", info.emoji)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(15000, info.xpToNext)
        assertEquals(6, info.tierIndex)
    }

    @Test
    fun testConquerorMaxTier() {
        val info = Ranks.info(35000)
        assertEquals("Conqueror", info.name)
        assertEquals("⚔️", info.emoji)
        assertEquals(1.0f, info.progress, 0.001f)
        assertEquals(0, info.xpToNext)
        assertEquals(7, info.tierIndex)
    }

    @Test
    fun testExcessXPAtMaxTier() {
        val info = Ranks.info(100000)
        assertEquals("Conqueror", info.name)
        assertEquals(1.0f, info.progress, 0.001f)
        assertEquals(0, info.xpToNext)
        assertEquals(7, info.tierIndex)
    }

    @Test
    fun testTierStrictIncreasingOrder() {
        val tiers = Ranks.tiers
        assertEquals(8, tiers.size)
        for (i in 0 until tiers.size - 1) {
            assertTrue("Tier ${tiers[i + 1].name} XP should be greater than ${tiers[i].name} XP",
                tiers[i + 1].xp > tiers[i].xp)
        }
    }

    @Test
    fun testDefaultConstants() {
        assertEquals(30, Ranks.DEFAULT_GOAL)
        assertEquals(90f, Ranks.DEFAULT_DOWN_ANGLE, 0.001f)
        assertEquals(150f, Ranks.DEFAULT_UP_ANGLE, 0.001f)
    }
}
