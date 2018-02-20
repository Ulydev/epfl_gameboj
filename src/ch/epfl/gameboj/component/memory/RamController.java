package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

import java.util.Objects;

public class RamController implements Component {

    Ram ram;

    // [startAddress, endAddress[
    int startAddress;
    int endAddress;

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
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, startAddress + ram.size());
    }

    private boolean isWithinBounds(int address) {
        return (startAddress <= address && address < endAddress);
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (isWithinBounds(address)) {
            return ram.read(address - startAddress);
        } else {
            return Component.NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        if (isWithinBounds(address))
            ram.write(address - startAddress, data);
    }

}
