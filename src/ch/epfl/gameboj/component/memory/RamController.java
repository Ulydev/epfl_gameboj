package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.component.Component;

public class RamController implements Component {

    Ram ram;
    // [startAddress, endAddress[
    int startAddress;
    int endAddress;

    public RamController(Ram ram, int startAddress, int endAddress) {
        //TODO: NullPointerException if ram is null, IllegalArgumentException if startAddress or endAddress isn't 16 bits, or interval is negative or too big
        this.ram = ram;
        this.startAddress = startAddress;
        this.endAddress = endAddress;
    }
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, ram.size() + 1);
    }

    public int read(int address) {
        //TODO: Check if address is within bounds
        return ram.read(address);
    }

    public void write(int address, int data) {
        //TODO: Check if address is within bounds
        ram.write(address, data);
    }

}
