package ch.epfl.gameboj;

/**
 * Register
 *
 * An interface representing a register
 *
 * @author Ulysse Ramage (282300)
 */
public interface Register {

    /**
     * @return the index of the Register
     */
    int ordinal();

    /**
     * @see #ordinal()
     */
    default int index() {
        return ordinal();
    }

}
