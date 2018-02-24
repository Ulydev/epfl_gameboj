package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

/**
 * Rom
 * A class representing a memory that can only be read from
 *
 * @author Ulysse Ramage (282300)
 */
public class Rom {

    /** Array containing the rom contents */
    private byte[] data;

    /**
     * Creates a rom from the specified data
     * @param data the data to initialize the rom with
     * @throws NullPointerException if {@code data} is null
     */
    public Rom(byte[] data) {
        Objects.requireNonNull(data);
        this.data = Arrays.copyOf(data, data.length);
    }

    /**
     * @return the size of the rom
     */
    public int size() {
        return this.data.length;
    }

    /**
     * Reads the value of the rom data at the given index
     * @param index the index to read data at
     * @return an unsigned integer, the value at the {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public int read(int index) {
        byte v = this.data[index];
        return Byte.toUnsignedInt(v);
    }

}
