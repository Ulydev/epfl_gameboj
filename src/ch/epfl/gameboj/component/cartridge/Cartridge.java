package ch.epfl.gameboj.component.cartridge;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class Cartridge implements Component {

    Component mbc;

    public static int TYPE_ADDRESS = 0x147;

    /**
     * Creates a new Cartridge associated with the given Memory Bank Controller
     * @param mbc the associated MBC
     */
    private Cartridge(Component mbc) {
        this.mbc = mbc;
    }

    /**
     *
     * @param romFile
     * @return
     * @throws IOException
     */
    public static Cartridge ofFile(File romFile) throws IOException {
        byte[] data;
        try (InputStream stream = new FileInputStream(romFile)) {
            data = stream.readAllBytes();
        }
        Preconditions.checkArgument(data[TYPE_ADDRESS] == 0);
        Cartridge cartridge = new Cartridge(new MBC0(new Rom(data)));
        return cartridge;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        return mbc.read(address);
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        mbc.write(address, data);
    }
}
