package ch.epfl.gameboj.component.cartridge;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

import java.util.Objects;

public final class MBC0 implements Component {

    Rom rom;

    /**
     * Creates a new controller of type 0 for the given Rom
     * @param rom the Rom the control
     * @throws NullPointerException if {@code rom} is null
     * @throws IllegalArgumentException if the size of {@code rom} isn't
     * exactly {@code 0x8000}
     */
    public MBC0(Rom rom) {
        Objects.requireNonNull(rom);
        Preconditions.checkArgument(rom.size() == 0x8000);
        this.rom = rom;
    }

    @Override
    public int read(int address) {
        if (0 <= address && address < rom.size()) {
            return rom.read(address);
        } else {
            return Component.NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {  }
}
