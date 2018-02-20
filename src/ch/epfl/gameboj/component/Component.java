package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

public interface Component {

    int NO_DATA = 0x100;

    int read(int address);
    void write(int address, int data);

    default void attachTo(Bus bus) {
        bus.attach(this);
    };

}
