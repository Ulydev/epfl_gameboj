package ch.epfl.gameboj;

import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**
 * GameBoy
 * A class representing a GameBoy and its components
 *
 * @author Ulysse Ramage (282300)
 */
public class GameBoy {

    /** Bus to connect all components */
    Bus bus;

    /**
     * Creates a new GameBoy from the given cartridge
     * @param cartridge the cartridge to read
     */
    public GameBoy(Object cartridge) {
        bus = new Bus();

        Ram ram = new Ram(AddressMap.WORK_RAM_SIZE);
        bus.attach(new RamController(
                ram,
                AddressMap.WORK_RAM_START
        ));
        bus.attach(new RamController(
                ram,
                AddressMap.ECHO_RAM_START,
                AddressMap.ECHO_RAM_END
        ));
    }

    /**
     * @return the GameBoy bus
     */
    public Bus bus() {
        return bus;
    }

}
