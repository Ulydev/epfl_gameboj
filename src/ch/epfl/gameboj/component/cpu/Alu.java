package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

import java.util.Objects;

/**
 * Alu
 *
 * A class containing static methods to execute various
 * arithmetic and logic operations
 *
 * @author Ulysse Ramage (282300)
 */
public final class Alu {

    /** Flags used by the processing unit */
    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3,
        C, H, N, Z
    }

    /** Rotation direction */
    public enum RotDir {
        LEFT, RIGHT
    }

    /** Private constructor to prevent instancing */
    private Alu() {}

    /**
     * Packs a 16-bit value and four booleans into an integer
     * @param v the value
     * @param z the Z flag
     * @param n the N flag
     * @param h the H flag
     * @param c the C flag
     * @return the packed integer
     * @throws IllegalArgumentException if {@code v} isn't 16-bit
     */
    private static int packValueZNHC(int v, boolean z, boolean n, boolean h, boolean c) {
        Preconditions.checkBits16(v);
        return (v << 8) + maskZNHC(z, n, h, c);
    }

    /**
     * Computes the half-carry flag of an addition
     * @param l the first value to add
     * @param r the second value to add
     * @param c0 a boolean, the initial carry
     * @return a boolean, the H flag for this operation
     */
    private static boolean getAddH(int l, int r, boolean c0) {
        return (Bits.clip(4, l) + Bits.clip(4, r) + (c0 ? 1 : 0)) > 0xF;
    }

    /**
     * Computes the carry flag of an addition
     * @param l the first value to add
     * @param r the second value to add
     * @param c0 a boolean, the initial carry
     * @return a boolean, the C flag for this operation
     */
    private static boolean getAddC(int l, int r, boolean c0) {
        return (Bits.clip(8, l) + Bits.clip(8, r) + (c0 ? 1 : 0)) > 0xFF;
    }

    /**
     * Creates a mask corresponding to the provided ZNHC flags
     * @param z the Z flag
     * @param n the N flag
     * @param h the H flag
     * @param c the C flag
     * @return a mask corresponding to the provided flags
     */
    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {
        int mask = 0;
        if (z) mask += Flag.Z.mask();
        if (n) mask += Flag.N.mask();
        if (h) mask += Flag.H.mask();
        if (c) mask += Flag.C.mask();
        return mask;
    }

    /**
     * Checks if a packed value is correctly formatted
     * @param valueFlags the packed value to check
     * @throws IllegalArgumentException if {@code valueFlags} is invalid
     * (one of the first 4 bits is 1)
     */
    private static void checkPackedValue(int valueFlags) {
        Preconditions.checkArgument(
                !Bits.test(valueFlags, Flag.UNUSED_0)
                && !Bits.test(valueFlags, Flag.UNUSED_1)
                && !Bits.test(valueFlags, Flag.UNUSED_2)
                && !Bits.test(valueFlags, Flag.UNUSED_3)
        );
    }

    /**
     * Extracts the 16-bit value of a given packed integer
     * @param valueFlags the packed integer
     * @return the extracted value
     * @throws IllegalArgumentException if {@code valueFlags} is invalid
     * @see #packValueZNHC(int, boolean, boolean, boolean, boolean)
     */
    public static int unpackValue(int valueFlags) {
        checkPackedValue(valueFlags);
        return Bits.extract(valueFlags, 8, 16);
    }

    /**
     * Extracts the flags of a given packed integer
     * @param valueFlags the packed integer
     * @return the extracted flags
     * @throws IllegalArgumentException if {@code valueFlags} is invalid
     * @see #packValueZNHC(int, boolean, boolean, boolean, boolean)
     */
    public static int unpackFlags(int valueFlags) {
        checkPackedValue(valueFlags);
        return Bits.extract(valueFlags, 0, 8);
    }

    /**
     * Adds two 8-bit values, given an initial carry
     * @param l the first value to add
     * @param r the second value to add
     * @param c0 the initial carry, true if 1 and false if 0
     * @return a packed integer containing the result and the flags
     * of the operation
     * @throws IllegalArgumentException if either {@code l}
     * or {@code r} isn't 8-bit
     */
    public static int add(int l, int r, boolean c0) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int result = Bits.clip(8, l + r + (c0 ? 1 : 0));
        boolean z = (result == 0),
                h = getAddH(l, r, c0),
                c = getAddC(l, r, c0);
        return packValueZNHC(result, z, false, h, c);
    }

    /**
     * The initial carry {@code c0} defaults to 0
     * @see #add(int, int, boolean)
     */
    public static int add(int l, int r) {
        return add(l, r, false);
    }

    /**
     * Adds two 16-bit values
     * @param l the first value to add
     * @param r the second value to add
     * @param high whether H and C should correspond to the addition of the 8
     * high bits (if true) or the 8 low bits (if false)
     * @return a packed integer containing {@code l+r} and the flags 00HC
     * @throws IllegalArgumentException if either {@code l} or {@code r} isn't 16-bit
     */
    private static int add16(int l, int r, boolean high) {
        Preconditions.checkBits16(l);
        Preconditions.checkBits16(r);
        int result = Bits.clip(16, l + r);
        boolean h, c;
        if (high) {
            int highL = Bits.extract(l, 8, 8);
            int highR = Bits.extract(r, 8, 8);
            boolean c0 = getAddC(l, r, false);
            h = getAddH(highL, highR, c0);
            c = getAddC(highL, highR, c0);
        } else {
            int lowL = Bits.clip(8, l);
            int lowR = Bits.clip(8, r);
            h = getAddH(lowL, lowR, false);
            c = getAddC(lowL, lowR, false);
        }
        return packValueZNHC(result, false, false, h, c);
    }

    /**
     * The H and C flags correspond to the addition of the 8 low bits
     * @see #add16(int, int, boolean)
     */
    public static int add16L(int l, int r) {
        return add16(l, r, false);
    }

    /**
     * The H and C flags correspond to the addition of the 8 high bits
     * @see #add16(int, int, boolean)
     */
    public static int add16H(int l, int r) {
        return add16(l, r, true);
    }

    /**
     * Subtracts two 8-bit values
     * @param l the value to execute the subtraction on
     * @param r the value to subtract to {@code l}
     * @param b0 the initial borrow, true if 1 and false if 0
     * @return a packed integer containing {@code l-r} and the flags Z1HC
     * @throws IllegalArgumentException if either {@code l} or {@code r} isn't 8-bit
     */
    public static int sub(int l, int r, boolean b0) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int result = Bits.clip(8, l - r - (b0 ? 1 : 0));
        boolean z = (result == 0),
                h = (Bits.clip(4, l) < Bits.clip(4, r) + (b0 ? 1 : 0)),
                c = (l < r + (b0 ? 1 : 0));
        return packValueZNHC(result, z, true, h, c);
    }

    /**
     * The initial borrow {@code b0} defaults to 0
     * @see #sub(int, int, boolean)
     */
    public static int sub(int l, int r) {
        return sub(l, r, false);
    }

    /**
     * Adjusts an 8-bit value into DCB format, given NHC flags
     * @param v the value to adjust
     * @param n the N flag
     * @param h the H flag
     * @param c the C flag
     * @return a packed integer containing {@code v}, adjusted into DCB format,
     * and the corresponding flags
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        Preconditions.checkBits8(v);
        boolean fixL = h || (!n && Bits.clip(4, v) > 9);
        boolean fixH = c || (!n && v > 0x99);
        int fix = 0x60 * (fixH ? 1 : 0) + 0x06 * (fixL ? 1 : 0);
        int result = n ? (v - fix) : (v + fix);
        result = Bits.clip(8, result);
        boolean z = (result == 0);
        return packValueZNHC(result, z, n, false, fixH);
    }

    /**
     * Computes the bitwise {@code and} of two 8-bit values
     * @param l the first value
     * @param r the second value
     * @return a packed integer contaning the result and the flags Z010
     * @throws IllegalArgumentException if either {@code l} or {@code r} isn't 8-bit
     */
    public static int and(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int result = l & r;
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, true, false);
    }

    /**
     * Computes the bitwise {@code or} of two 8-bit values
     * @param l the first value
     * @param r the second value
     * @return a packed integer containing the result and the flags Z000
     * @throws IllegalArgumentException if either {@code l} or {@code r} isn't 8-bit
     */
    public static int or(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int result = l | r;
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, false, false);
    }

    /**
     * Computes the bitwise {@code xor} of two 8-bit values
     * @param l the first value
     * @param r the second value
     * @return a packed integer containing the result and the flags Z000
     * @throws IllegalArgumentException if either {@code l} or {@code r} isn't 8-bit
     */
    public static int xor(int l, int r) {
        Preconditions.checkBits8(l);
        Preconditions.checkBits8(r);
        int result = l ^ r;
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, false, false);
    }

    /**
     * Shifts an 8-bit value of 1 bit to the left
     * @param v the value to shift
     * @return a packed integer containing {@code v}, shifted of 1 bit to the left,
     * and the flags Z00C, where C is the bit ejected by the shift
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int shiftLeft(int v) {
        Preconditions.checkBits8(v);
        int result = Bits.clip(8, v << 1);
        boolean z = (result == 0),
                c = Bits.test(v, 8 - 1);
        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Shifts an 8-bit value of 1 bit to the left, keeping its sign bit using
     * arithmetic shift
     * @param v the value to shift
     * @return a packed integer containing {@code v}, shifted of 1 bit to the right,
     * and the flags Z00C, where C is the bit ejected by the shift
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int shiftRightA(int v) {
        Preconditions.checkBits8(v);
        int result = Bits.clip(8, Bits.signExtend8(v) >> 1);
        boolean z = (result == 0),
                c = Bits.test(v, 0);
        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Shifts an 8-bit value of 1 bit to the left, using logical shift
     * @param v the value to shift
     * @return a packed integer containing {@code v}, shifted of 1 bit to the right,
     * and the flags Z00C, where C is the bit ejected by the shift
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int shiftRightL(int v) {
        Preconditions.checkBits8(v);
        int result = v >>> 1;
        boolean z = (result == 0),
                c = Bits.test(v, 0);
        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Rotates an 8-bit value of 1 bit
     * @param d the direction of the rotation
     * @param v the value to rotate
     * @return a packed integer containing {@code v}, rotated of 1 bit,
     * and the flags Z00C, where C is the bit that was swapped from one end to another
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int rotate(RotDir d, int v) {
        Preconditions.checkBits8(v);
        int result = Bits.rotate(8, v, (d == RotDir.LEFT) ? 1 : -1);
        boolean c = Bits.test(v, (d == RotDir.LEFT) ? (8 - 1) : 0);
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Rotates an 8-bit value combined with a given carry
     * @param d the direction of the rotation
     * @param v the value to rotate
     * @param c the carry
     * @return a packed integer containing {@code cv}, rotated of 1 bit and
     * clipped to 8-bit, and the flags Z00C, where C is the new 9-th bit
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int rotate(RotDir d, int v, boolean c) {
        Preconditions.checkBits8(v);
        int cv = Bits.make16(c ? 1 : 0, v);
        int result = Bits.rotate(8 + 1, cv, (d == RotDir.LEFT) ? 1 : -1);
        c = Bits.test(result, 8);
        result = Bits.clip(8, result);
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, false, c);
    }

    /**
     * Swaps the first and last four bits of an 8-bit value
     * @param v the value
     * @return a packed integer contaning the swapped value and the flags Z000
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     */
    public static int swap(int v) {
        Preconditions.checkBits8(v);
        int result = Bits.rotate(8, v, 4);
        boolean z = (result == 0);
        return packValueZNHC(result, z, false, false, false);
    }

    /**
     * Tests the i-th bit of a value
     * @param v the value to test
     * @param bitIndex the index of the bit to test
     * @return a packed integer containing 0 and the flags Z010, where Z is 1
     * if and only if the tested bit is 0
     * @throws IllegalArgumentException if {@code v} isn't 8-bit
     * @throws IndexOutOfBoundsException if {@code bitIndex} isn't between 0 and 7
     */
    public static int testBit(int v, int bitIndex) {
        Preconditions.checkBits8(v);
        Objects.checkIndex(bitIndex, 8);
        boolean z = !Bits.test(v, bitIndex);
        return packValueZNHC(0, z, false, true, false);
    }

}
