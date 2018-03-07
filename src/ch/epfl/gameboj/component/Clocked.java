package ch.epfl.gameboj.component;

public interface Clocked {

    /**
     * Executes all operations of the provided cycle
     * @param cycle the index of the cycle
     */
    void cycle(long cycle);

}
