package ch.epfl.gameboj.component.cpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AluTest {

    private void assertValueFlags(int result, int expectedValue, boolean z, boolean n, boolean h, boolean c) {
        int actualValue = Alu.unpackValue(result);
        assertEquals(expectedValue, actualValue);

        int expectedFlags = Alu.maskZNHC(z, n, h, c);
        int actualFlags = Alu.unpackFlags(result);
        assertEquals(expectedFlags, actualFlags);
    }

    @Test
    void unpackValueWorksForKnownValue() {
        assertEquals(0xFF, Alu.unpackValue(0xFF70));
    }

    @Test
    void unpackFailsForInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Alu.unpackFlags(0b111101010010));
    }

    @Test
    void unpackFlagsWorksForKnownValue() {
        assertEquals(0x70, Alu.unpackFlags(0xFF70));
    }

    @Test
    void maskZNHCWorksForKnownValue() {
        assertEquals(0x70, Alu.maskZNHC(false, true, true ,true));
    }

    @Test
    void addWorksForKnownValues() {
        assertValueFlags(Alu.add(0x10, 0x15), 0x25, false, false, false, false);
        assertValueFlags(Alu.add(0x08, 0x08), 0x10, false, false, true, false);
    }

    @Test
    void addWithCarryWorksForKnownValue() {
        assertValueFlags(Alu.add(0x80, 0x7F, true), 0x00, true, false, true, true);
    }

    @Test
    void subWorksForKnownValue() {
        assertValueFlags(Alu.sub(0x10, 0x10), 0x00, true, true, false, false);
        assertValueFlags(Alu.sub(0x10, 0x80), 0x90, false, true, false, true);
    }

    @Test
    void subWithBorrowWorksForKnownValue() {
        assertValueFlags(Alu.sub(0x01, 0x01, true), 0xFF, false, true, true, true);
    }

    @Test
    void bcdAdjustWorksForKnownValues() {
        assertValueFlags(Alu.bcdAdjust(0x6D, false, false, false), 0x73, false, false, false, false);
        assertValueFlags(Alu.bcdAdjust(0x0F, true, true, false), 0x09, false, true, false, false);
    }

    @Test
    void andWorksForKnownValue() {
        assertValueFlags(Alu.and(0x53, 0xA7), 0x03, false, false, true, false);
    }

    @Test
    void orWorksForKnownValue() {
        assertValueFlags(Alu.or(0x53, 0xA7), 0xF7, false, false, false, false);
    }

    @Test
    void xorWorksForKnownValue() {
        assertValueFlags(Alu.xor(0x53, 0xA7), 0xF4, false, false, false, false);
    }

    @Test
    void shiftLeftWorksForKnownValue() {
        assertValueFlags(Alu.shiftLeft(0x80), 0x00, true, false, false, true);
    }

    @Test
    void shiftRightLWorksForKnownValue() {
        assertValueFlags(Alu.shiftRightL(0x80), 0x40, false, false, false, false);
    }

    @Test
    void shiftRightAWorksForKnownValue() {
        assertValueFlags(Alu.shiftRightA(0x80), 0xC0, false, false, false, false);
    }

    @Test
    void rotateWorksForKnownValue() {
        assertValueFlags(Alu.rotate(Alu.RotDir.LEFT, 0x80), 0x01, false, false, false, true);
    }

    @Test
    void rotateWithCarryWorksForKnownValues() {
        assertValueFlags(Alu.rotate(Alu.RotDir.LEFT, 0x80, false), 0x00, true, false, false, true);
        assertValueFlags(Alu.rotate(Alu.RotDir.LEFT, 0x00, true), 0x01, false, false, false, false);
    }

    @Test
    void add16LWorksForKnownValue() {
        assertValueFlags(Alu.add16L(0x11FF, 0x0001), 0x1200, false, false, true, true);
    }

    @Test
    void add16HWorksForKnownValue() {
        assertValueFlags(Alu.add16H(0x11FF, 0x0001), 0x1200, false, false, false, false);
    }

    @Test
    void swapWorksForKnownValues() {
        assertValueFlags(Alu.swap(0x8F), 0xF8, false, false, false, false);
        assertValueFlags(Alu.swap(0x12), 0x21, false, false, false, false);
    }

    @Test
    void swapWorksForZero() {
        assertValueFlags(Alu.swap(0x00), 0x00, true, false, false, false);
    }

}

