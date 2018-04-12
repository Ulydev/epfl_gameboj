package ch.epfl.gameboj.component;

/**
 * Clocked
 *
 * An interface designed to synchronize components
 *
 * @author Ulysse Ramage (282300)
 */
public interface Clocked {

    /**
     * Executes all operations of the provided cycle
     * @param cycle the index of the cycle
     */
    void cycle(long cycle);

}
