package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * RegisterFile
 *
 * A class representing an array of registers that can be manipulated
 *
 * @param <E> the type of the registers to use (extends Register)
 *
 * @author Ulysse Ramage (282300)
 */
public final class RegisterFile<E extends Register> {

    /** Array of registers - initial value is 0 */
    byte[] registers;

    /**
     * Creates a new RegisterFile containing the values of the
     * provided registers
     * @param allRegs the registers to use
     */
    public RegisterFile(E[] allRegs) {
        registers = new byte[allRegs.length];
    }

    /**
     * Gets the current value of a register
     * @param reg the register to get the value from
     * @return an unsigned 8-bit integer, the value contained in {@code reg}
     */
    public int get(E reg) {
        return Byte.toUnsignedInt(registers[reg.index()]);
    }

    /**
     * Stores a value in a register
     * @param reg the register to write to
     * @param newValue the new value to store
     * @throws IllegalArgumentException if {@code newValue} isn't 8-bit
     */
    public void set(E reg, int newValue) {
        Preconditions.checkBits8(newValue);
        registers[reg.index()] = (byte)newValue;
    }

    /**
     * Tests a bit of a register value
     * @param reg the register containing the value
     * @param b the bit to test
     * @return true if and only if the bit {@code b} of the value stored in
     * {@code reg} is 1
     */
    public boolean testBit(E reg, Bit b) {
        return Bits.test(get(reg), b);
    }

    /**
     * Sets a bit of a register value
     * @param reg the register containing the value
     * @param bit the bit to modify
     * @param newValue the new bit value
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        set(reg, Bits.set(get(reg), bit.index(), newValue));
    }

}
