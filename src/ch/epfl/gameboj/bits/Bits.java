package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;

import java.util.Objects;

public final class Bits {
    private Bits() {}

    public static int mask(int index) {
        Objects.checkIndex(index, Integer.SIZE);
        return 1 << index;
    }

    public static boolean test(int bits, int index) {
        return (bits & mask(index)) != 0;
    }

    public static boolean test(int bits, Bit bit) {
        return test(bits, bit.index());
    }

    public static int set(int bits, int index, boolean newValue) {
        if (newValue) {
            return bits | mask(index);
        } else {
            return bits & ~mask(index);
        }
    }

    public static int clip(int size, int bits) {
        Preconditions.checkArgument(0 <= size && size <= Integer.SIZE);
        return (size == Integer.SIZE) ? bits : (bits & ~(-1 << size));
    }

    public static int extract(int bits, int start, int size) {
        Objects.checkFromIndexSize(start, size, Integer.SIZE);
        return clip(size, bits >>> start);
    }

    public static int rotate(int size, int bits, int distance) {
        Preconditions.checkArgument(0 < size && size <= Integer.SIZE);
        if (size < Integer.SIZE) {
            Preconditions.checkArgument(0 <= bits);
            Preconditions.checkArgument(bits <= (-1 >>> (Integer.SIZE - size)));
        }
        int offset = Math.floorMod(distance, size);
        return clip(size, bits << offset) | (bits >>> size - offset);
    }

    public static int signExtend8(int b) {
        Preconditions.checkBits8(b);
        return (int)((byte)b);
    }

    public static int reverse8(int b) {
        Preconditions.checkBits8(b);
        int result = 0;
        for (int i = 0; i < 0b1000; ++i) {
            result = result | (extract(b, 0b1000 - i - 1, 1) << i);
        }
        return result;
    }

    public static int complement8(int b) {
        Preconditions.checkBits8(b);
        return extract(~b, 0, 0b1000);
    }

    public static int make16(int highB, int lowB) {
        Preconditions.checkBits8(highB);
        Preconditions.checkBits8(lowB);
        return (highB << 0b1000) | lowB;
    }



}
