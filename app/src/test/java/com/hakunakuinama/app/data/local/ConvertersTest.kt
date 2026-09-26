package com.hakunakuinama.app.data.local

import com.hakunakuinama.app.domain.model.FoodCategory
import com.hakunakuinama.app.domain.model.MealDifficulty
import com.hakunakuinama.app.domain.model.MealSlot
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The type converters that persist enums **by name**.
 *
 * This test is the JVM half of a release-only hazard. `MealSlot.BREAKFAST` is written to
 * SQLite as the string `"BREAKFAST"` and read back with `valueOf("BREAKFAST")`. R8 renames
 * enum constants unless `proguard-rules.pro` tells it not to. If that rule is ever dropped,
 * the debug build keeps working and the release build throws
 * `IllegalArgumentException: No enum constant` on the first query — for every slot,
 * category and difficulty in the database.
 *
 * So: this test pins the round trip, and the keep rule pins the names.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `meal slot survives a round trip through its stored name`() {
        MealSlot.entries.forEach { slot ->
            val stored = converters.fromMealSlot(slot)
            assertEquals(slot.name, stored)
            assertEquals(slot, converters.toMealSlot(stored))
        }
    }

    @Test
    fun `difficulty survives a round trip through its stored name`() {
        MealDifficulty.entries.forEach { difficulty ->
            val stored = converters.fromMealDifficulty(difficulty)
            assertEquals(difficulty.name, stored)
            assertEquals(difficulty, converters.toMealDifficulty(stored))
        }
    }

    @Test
    fun `food category survives a round trip through its stored name`() {
        FoodCategory.entries.forEach { category ->
            val stored = converters.fromFoodCategory(category)
            assertEquals(category.name, stored)
            assertEquals(category, converters.toFoodCategory(stored))
        }
    }

    @Test
    fun `a tag set survives a round trip`() {
        val tags = setOf("budget", "quick", "vegan")
        assertEquals(tags, converters.toStringSet(converters.fromStringSet(tags)))
    }

    @Test
    fun `an empty tag set round trips to an empty set, not a set holding a blank`() {
        val result = converters.toStringSet(converters.fromStringSet(emptySet()))
        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `a null tag set round trips to an empty set`() {
        assertEquals(emptySet<String>(), converters.toStringSet(converters.fromStringSet(null)))
    }

    @Test
    fun `a tag containing the delimiter is rejected rather than silently corrupting the row`() {
        // "budget|quick" would split into two tags on the way back out. Dropping it is
        // visible and safe; silently merging it is neither.
        val stored = converters.fromStringSet(setOf("budget", "bad|tag"))
        assertEquals("budget", stored)
        assertEquals(setOf("budget"), converters.toStringSet(stored))
    }

    @Test
    fun `a meal id list survives a round trip`() {
        val ids = listOf(3L, 7L, 11L)
        assertEquals(ids, converters.toLongList(converters.fromLongList(ids)))
    }

    @Test
    fun `an empty id list round trips to an empty list`() {
        assertEquals(emptyList<Long>(), converters.toLongList(converters.fromLongList(emptyList())))
    }

    @Test
    fun `unparseable id list entries are dropped instead of crashing the query`() {
        // Defensive: a corrupted row should cost us one id, not the whole shopping list.
        assertEquals(listOf(7L), converters.toLongList("7|oops|"))
    }
}
