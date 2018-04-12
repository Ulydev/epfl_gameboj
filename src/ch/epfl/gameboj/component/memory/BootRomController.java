package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

import java.util.Objects;

/**
 * BootRomController
 *
 * A class representing a Boot Rom Controller that gives access
 * to the contents of a cartridge once it has been disabled
 *
 * @author Ulysse Ramage (282300)
 */
public final class BootRomController implements Component {

    private final Cartridge cartridge;
    private boolean bootRomDisabled = false;

    /**
     * Creates a new BootRomController that gives access
     * to the contents of a Cartridge
     * @param cartridge the cartridge to associate the BootRomController with
     * @throws NullPointerException if {@code cartridge} is null
     */
    public BootRomController(Cartridge cartridge) {
        Objects.requireNonNull(cartridge);
        this.cartridge = cartridge;
    }

    /**
     * {@inheritDoc}
     * Redirects to the boot rom if it is enabled
     */
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if ((0 <= address && address <= 0xFF) && !bootRomDisabled) {
            return Byte.toUnsignedInt(BootRom.DATA[address]);
        } else {
            return cartridge.read(address);
        }
    }

    /**
     * {@inheritDoc}
     * Disables the boot rom if writing to the boot rom disable address
     */
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
