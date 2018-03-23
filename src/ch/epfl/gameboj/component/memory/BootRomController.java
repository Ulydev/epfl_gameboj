package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

import java.util.Objects;

public final class BootRomController implements Component {

    Cartridge cartridge;
    boolean bootRomDisabled = false;

    public BootRomController(Cartridge cartridge) {
        Objects.requireNonNull(cartridge);
        this.cartridge = cartridge;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if ((0 <= address && address <= 0xFF) && !bootRomDisabled) {
            return Byte.toUnsignedInt(BootRom.DATA[address]);
        } else {
            return cartridge.read(address);
        }
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if ((address == AddressMap.REG_BOOT_ROM_DISABLE) && !bootRomDisabled) {
            bootRomDisabled = true;
        }
        cartridge.write(address, data);
    }
}
