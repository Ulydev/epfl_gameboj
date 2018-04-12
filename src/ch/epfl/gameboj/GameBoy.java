package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

import java.util.Objects;

/**
 * GameBoy
 *
 * A class representing a GameBoy and its components
 *
 * @author Ulysse Ramage (282300)
 */
public final class GameBoy {

    private final Bus bus;
    private final Cpu cpu;
    private final Timer timer;

    private long simulatedCycles;

    /**
     * Creates a new GameBoy from the given cartridge
     * @param cartridge the cartridge to read
     */
    public GameBoy(Cartridge cartridge) {
        Objects.requireNonNull(cartridge);

        bus = new Bus();

        // Ram
        Ram ram = new Ram(AddressMap.WORK_RAM_SIZE);
        RamController ramController = new RamController(
                ram,
                AddressMap.WORK_RAM_START
        );
        RamController echoRamController = new RamController(
                ram,
                AddressMap.ECHO_RAM_START,
                AddressMap.ECHO_RAM_END
        );
        ramController.attachTo(bus);
        echoRamController.attachTo(bus);

        // Cpu
        cpu = new Cpu();
        cpu.attachTo(bus);

        // BootRomController
        BootRomController brm = new BootRomController(cartridge);
        brm.attachTo(bus);

        // Timer
        timer = new Timer(cpu);
        timer.attachTo(bus);
    }

    /**
     * @return the GameBoy bus
     */
    public Bus bus() {
        return bus;
    }

    /**
     * @return the GameBoy cpu
     */
    public Cpu cpu() {
        return cpu;
    }

    /**
     * @return the GameBoy timer
     */
    public Timer timer() {
        return timer;
    }

    /**
     * Runs the processor until a given cycle is reached
     * @param cycle the cycle limit
     */
    public void runUntil(long cycle) {
        Preconditions.checkArgument(simulatedCycles <= cycle);
        while (simulatedCycles < cycle) {
            timer.cycle(simulatedCycles);
            cpu.cycle(simulatedCycles);
            simulatedCycles++;
        }
    }

    /**
     * @return the number of cycles already simulated
     */
    public long cycles() {
        return simulatedCycles;
    }

}
