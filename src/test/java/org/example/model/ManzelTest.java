package org.example.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Manzel model class.
 * Pure logic tests — no DB or JavaFX required.
 */
class ManzelTest {

    // ── Constructor tests ──────────────────────────────────────────────────

    @Test
    void fullConstructor_setsAllFields() {
        Manzel m = new Manzel(1, "Villa Rose", "123 Main St", 500000, 4);
        assertEquals(1,            m.getId());
        assertEquals("Villa Rose", m.getName());
        assertEquals("123 Main St",m.getAddress());
        assertEquals(500000,       m.getPrice());
        assertEquals(4,            m.getRooms());
    }

    @Test
    void noIdConstructor_setsFieldsWithoutId() {
        Manzel m = new Manzel("Blue House", "45 Oak Ave", 300000, 3);
        assertEquals("Blue House", m.getName());
        assertEquals("45 Oak Ave", m.getAddress());
        assertEquals(300000,       m.getPrice());
        assertEquals(3,            m.getRooms());
        assertEquals(0,            m.getId()); // default int value
    }

    // ── Setter validation tests ────────────────────────────────────────────

    @Test
    void setName_valid_updatesName() {
        Manzel m = new Manzel("Old Name", "Addr", 100, 2);
        m.setName("New Name");
        assertEquals("New Name", m.getName());
    }

    @Test
    void setName_null_throwsIllegalArgument() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        assertThrows(IllegalArgumentException.class, () -> m.setName(null));
    }

    @Test
    void setName_exceeds30Chars_throwsIllegalArgument() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        String longName = "A".repeat(31);
        assertThrows(IllegalArgumentException.class, () -> m.setName(longName));
    }

    @Test
    void setName_exactly30Chars_isAllowed() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        String name30 = "A".repeat(30);
        assertDoesNotThrow(() -> m.setName(name30));
        assertEquals(name30, m.getName());
    }

    @Test
    void setAddress_null_throwsIllegalArgument() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        assertThrows(IllegalArgumentException.class, () -> m.setAddress(null));
    }

    @Test
    void setAddress_exceeds30Chars_throwsIllegalArgument() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        String longAddr = "B".repeat(31);
        assertThrows(IllegalArgumentException.class, () -> m.setAddress(longAddr));
    }

    @Test
    void setAddress_exactly30Chars_isAllowed() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        String addr30 = "B".repeat(30);
        assertDoesNotThrow(() -> m.setAddress(addr30));
        assertEquals(addr30, m.getAddress());
    }

    @Test
    void setPrice_updatesPrice() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        m.setPrice(999999);
        assertEquals(999999, m.getPrice());
    }

    @Test
    void setRooms_updatesRooms() {
        Manzel m = new Manzel("Name", "Addr", 100, 2);
        m.setRooms(10);
        assertEquals(10, m.getRooms());
    }

    // ── equals and hashCode tests ──────────────────────────────────────────

    @Test
    void equals_sameFields_returnsTrue() {
        Manzel a = new Manzel(1, "Villa", "Addr", 100, 2);
        Manzel b = new Manzel(1, "Villa", "Addr", 100, 2);
        assertEquals(a, b);
    }

    @Test
    void equals_differentId_returnsFalse() {
        Manzel a = new Manzel(1, "Villa", "Addr", 100, 2);
        Manzel b = new Manzel(2, "Villa", "Addr", 100, 2);
        assertNotEquals(a, b);
    }

    @Test
    void hashCode_equalObjects_samHashCode() {
        Manzel a = new Manzel(1, "Villa", "Addr", 100, 2);
        Manzel b = new Manzel(1, "Villa", "Addr", 100, 2);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ── toString test ──────────────────────────────────────────────────────

    @Test
    void toString_containsAllFields() {
        Manzel m = new Manzel(1, "Villa", "Addr", 100, 2);
        String s = m.toString();
        assertTrue(s.contains("Villa"));
        assertTrue(s.contains("Addr"));
        assertTrue(s.contains("100"));
        assertTrue(s.contains("2"));
    }
}
