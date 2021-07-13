package net.glowstone.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.bukkit.Tag;
import org.junit.jupiter.api.Test;

class GlowTagsTest {
    @Test
    public void testSanityChecks() {
        assertContainsAllAndNotOnly(Tag.SLABS.getValues(), Tag.WOODEN_SLABS.getValues());
        assertContainsAllAndNotOnly(Tag.STAIRS.getValues(), Tag.WOODEN_STAIRS.getValues());
        assertContainsAllAndNotOnly(Tag.BUTTONS.getValues(), Tag.WOODEN_BUTTONS.getValues());
    }

    private static void assertContainsAllAndNotOnly(Collection<?> superset, Collection<?> subset) {
        assertTrue(superset.containsAll(subset));
        assertFalse(subset.containsAll(superset));
    }
}
