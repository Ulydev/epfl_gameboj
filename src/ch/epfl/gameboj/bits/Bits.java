package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;

import java.util.Objects;

/**
 * Bits
 *
 * A class containing static methods to manipulate bits
 * @author Ulysse Ramage (282300)
 */
public final class Bits {

    /** Private constructor to prevent instancing */
    private Bits() {}

    /**
     * @param index the index of the mask
     * @return a value in which only the bit of given index is 0
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static int mask(int index) {
        Objects.checkIndex(index, Integer.SIZE);
        return 1 << index;
    }

    /**
     * @param bits the value to check
     * @param index the index to check
     * @return true if and only if the bit at the given index of bits is 1
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static boolean test(int bits, int index) {
        return (bits & mask(index)) != 0;
    }

    /**
     * The index is obtained from the given Bit
     * @see #test(int, int)
     */
    public static boolean test(int bits, Bit bit) {
        return test(bits, bit.index());
    }

    /**
     * @param bits the value to modify
     * @param index the index to write to
     * @param newValue the new bit value
     * @return a value equal to bits, except the bit of given index is newValue
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public static int set(int bits, int index, boolean newValue) {
        if (newValue) {
            return bits | mask(index);
        } else {
            return bits & ~mask(index);
        }
    }

    /**
     * @param size the size of the new value
     * @param bits the value to modify
     * @return a value equal to bits, except all bits of index >= size are 0
     * @throws IllegalArgumentException if size isn't between 0 (included)
     * and 32 (included)
     */
    public static int clip(int size, int bits) {
        Preconditions.checkArgument(0 <= size && size <= Integer.SIZE);
        return (size == Integer.SIZE) ? bits : (bits & ~(-1 << size));
    }

    /**
     * @param bits the value to modify
     * @param start the start index
     * @param size the size of the new value
     * @return a value obtained by extracting the bits from the start index to
     * the (start + size) index
     * @throws IndexOutOfBoundsException if the range [start, start+size] isn't
     * valid
     */
    public static int extract(int bits, int start, int size) {
        Objects.checkFromIndexSize(start, size, Integer.SIZE);
        return clip(size, bits >>> start);
    }

    /**
     * @param size the size of the desired value
     * @param bits the value to modify
     * @param distance the distance offset to rotate; to the left if it is
     * positive, to the right if it is negative
     * @return a value obtained by rotating the bits of {@code bits}
     * @throws IllegalArgumentException if {@code size} isn't between
     * 0 (excluded) and 32 (included), or if {@code bits} isn't a {@code size}-bit
     * value
     */
    public static int rotate(int size, int bits, int distance) {
        Preconditions.checkArgument(0 < size && size <= Integer.SIZE);
        if (size < Integer.SIZE) {
            Preconditions.checkArgument(0 <= bits);
            Preconditions.checkArgument(bits <= (-1 >>> (Integer.SIZE - size)));
        }
        int offset = Math.floorMod(distance, size);
        return clip(size, bits << offset) | (bits >>> size - offset);
    }

    /**
     * @param b the value to modify
     * @return a value obtained by extending the sign of {@code b}
     * @throws IllegalArgumentException if {@code b} isn't 8-bit
     */
    public static int signExtend8(int b) {
        Preconditions.checkBits8(b);
        return (int)((byte)b);
    }

    /**
     * @param b the value to modify
     * @return a value obtained by mirroring the i and 7-i bits of {@code b}
     * @throws IllegalArgumentException if {@code b} isn't 8-bit
     */
    public static int reverse8(int b) {
        Preconditions.checkBits8(b);
        int result = 0;
        for (int i = 0; i < 8; ++i) {
            result = result | (extract(b, 8 - i - 1, 1) << i);
        }
        return result;
    }

    /**
     * @param b the value to modify
     * @return a value obtained by flipping the 8 bits of {@code b}
     * @throws IllegalArgumentException if {@code b} isn't 8-bit
     */
    public static int complement8(int b) {
        Preconditions.checkBits8(b);
        return extract(~b, 0, 8);
    }

    /**
     * @param highB the left-hand value of the result
     * @param lowB the right-hand value of the result
     * @return a 16-bit integer obtained by combining {@code highB} on the left
     * and {@code lowB} on the right
     * @throws IllegalArgumentException if either {@code highB} or {@code lowB}
     * isn't 8-bit
     */
    public static int make16(int highB, int lowB) {
        Preconditions.checkBits8(highB);
        Preconditions.checkBits8(lowB);
        return (highB << 8) | lowB;
    }

}
