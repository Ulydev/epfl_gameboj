package ch.epfl.gameboj.bits;

/**
 * Bit
 * An interface that represents a bit
 *
 * @author Ulysse Ramage (282300)
 */
public interface Bit {

    /**
     * @return the index of the Bit
     */
    int ordinal();

    /**
     * @see #ordinal()
     */
    default int index() {
        return ordinal();
    }

    /**
     * @return a value in which only the bit of same index as the instance is 1
     */
    default int mask() {
        return Bits.mask(index());
    }

}
