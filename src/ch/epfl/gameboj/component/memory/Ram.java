package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

/**
 * Ram
 *
 * A class representing a memory that can be read from and written to
 *
 * @author Ulysse Ramage (282300)
 */
public final class Ram {

    private final byte[] data;

    /**
     * Creates a new ram with the specified size
     * @param size the size of the ram
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);
        this.data = new byte[size];
    }

    /**
     * Reads the value of the ram data at the given index
     * @param index the index to read data at
     * @return an unsigned integer, the value at {@code index}
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public int read(int index) {
        byte v = this.data[index];
        return Byte.toUnsignedInt(v);
    }

    /**
     * Writes a value in the ram data at the given index
     * @param index the index to write data at
     * @param value the data to write
     * @throws IllegalArgumentException if {@code value} is not 8-bit
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public void write(int index, int value) {
        Preconditions.checkBits8(value);
        this.data[index] = (byte)value;
    }

    /**
     * @return the size of the ram
     */
    public int size() {
        return this.data.length;
    }

}
