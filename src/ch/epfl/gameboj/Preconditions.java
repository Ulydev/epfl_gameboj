package ch.epfl.gameboj;

public interface Preconditions {

    static void checkArgument(boolean b) {
        if (!b)
            throw new IllegalArgumentException();
    }

    static int checkBits8(int v) {
        if (0 <= v && v <= 0xFF) {
            return v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    static int checkBits16(int v) {
        if (0 <= v && v <= 0xFFFF) {
            return v;
        } else {
            throw new IllegalArgumentException();
        }
    }

}
