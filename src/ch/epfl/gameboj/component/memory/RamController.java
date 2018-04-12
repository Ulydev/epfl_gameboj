package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

import java.util.Objects;

/**
 * RamController
 *
 * A Component that controls access to a ram
 *
 * @author Ulysse Ramage (282300)
 */
public final class RamController implements Component {

    private final Ram ram;

    private final int startAddress;
    private final int endAddress;

    /**
     * Creates a new RamController with the given ram and address range
     * @param ram the Ram instance to control
     * @param startAddress the start address of the controller
     * @param endAddress the end address of the controller
     * @throws IllegalArgumentException if either {@code startAddress} or
     * {@code endAddress} isn't 16-bit, or if the address range is negative or
     * greater than the size of {@code ram} itself
     * @throws NullPointerException if {@code ram} is null
     */
    public RamController(Ram ram, int startAddress, int endAddress) {
        Objects.requireNonNull(ram);
        Preconditions.checkBits16(startAddress);
        Preconditions.checkBits16(endAddress);

        int range = endAddress - startAddress;
        Preconditions.checkArgument(range >= 0);
        Preconditions.checkArgument(range <= ram.size());

        this.ram = ram;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }

    /**
     * endAddress is calculated such that the entire ram is accessible
     * @see #RamController(Ram, int, int)
     */
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, startAddress + ram.size());
    }

    private boolean isWithinBounds(int address) {
        return (startAddress <= address && address < endAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (isWithinBounds(address)) {
            return ram.read(address - startAddress);
        } else {
            return Component.NO_DATA;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if (isWithinBounds(address)) {
            ram.write(address - startAddress, data);
        }
    }

}
