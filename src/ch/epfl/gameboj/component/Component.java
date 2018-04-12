package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

/**
 * Component
 *
 * An interface that represents an electronic component
 *
 * @author Ulysse Ramage (282300)
 */
public interface Component {

    static final int NO_DATA = 0x100;

    /**
     * Reads the data of the component at the specified address
     * @param address the address to read at
     * @return the read value
     * @throws IllegalArgumentException if {@code address} isn't 16-bit
     */
    int read(int address);

    /**
     * Writes data in the component at the specified address
     * @param address the address to write at
     * @param data the data to write
     * @throws IllegalArgumentException if {@code address} isn't 16-bit,
     * or if {@code data} isn't 8-bit
     */
    void write(int address, int data);

    /**
     * Attaches the component to the passed bus
     * @param bus the bus to attach the component to
     */
    default void attachTo(Bus bus) {
        bus.attach(this);
    };

}
