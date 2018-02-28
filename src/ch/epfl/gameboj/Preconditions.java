package ch.epfl.gameboj;

/**
 * Preconditions
 *
 * An interface containing static methods that can be used to check
 * the validity of various arguments
 *
 * @author Ulysse Ramage (282300)
 */
public interface Preconditions {

    /**
     * Checks if a condition is satisfied
     * @param b the condition to check
     * @throws IllegalArgumentException if {@code condition} is not satisfied
     */
    static void checkArgument(boolean b) {
        if (!b)
            throw new IllegalArgumentException();
    }

    /**
     * Checks if an integer is an 8-bit value
     * @param v the integer to check
     * @throws IllegalArgumentException if {@code v} is not an 8-bit value
     * @return {@code v} if no exception has been raised
     */
    static int checkBits8(int v) {
        if (0 <= v && v <= 0xFF) {
            return v;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks if an integer is a 16-bit value
     * @param v the integer to check
     * @throws IllegalArgumentException if {@code v} is not a 16-bit value
     * @return {@code v} if no exception has been raised
     */
    static int checkBits16(int v) {
        if (0 <= v && v <= 0xFFFF) {
            return v;
        } else {
            throw new IllegalArgumentException();
        }
    }

}
