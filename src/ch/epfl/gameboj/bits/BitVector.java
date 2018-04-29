package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;

import java.util.Arrays;
import java.util.Objects;

/**
 * BitVector
 *
 * A class that represents a vector of Bits, of infinitely large size
 *
 * @author Ulysse Ramage (282300)
 */
public final class BitVector {

    private final int[] elements;

    private static final int ELEMENT_SIZE = Integer.SIZE;

    private enum ExtractMode {
        ZERO_EXTENDED, WRAPPED
    }

    /** Private constructor */
    private BitVector(int[] elements) {
        this.elements = elements;
    }

    /**
     * Creates a new BitVector with a given size and initial value
     * @param size the initial size of the vector
     * @param value the default value as a boolean (true = 1, false = 0)
     * @throws IllegalArgumentException if {@code size} is not a multiple of
     * {@code ELEMENT_SIZE} (32)
     */
    public BitVector(int size, boolean value) {
        Preconditions.checkArgument(size > 0 && size % ELEMENT_SIZE == 0);
        this.elements = new int[size / ELEMENT_SIZE];
        if (value)
            Arrays.fill(elements, -1);
    }

    /**
     * The default value defaults to false
     * @see #BitVector(int, boolean)
     */
    public BitVector(int size) {
        this(size, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof BitVector))
            return false;
        return Arrays.equals(elements, ((BitVector) that).elements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = intSize() - 1; i >= 0; --i) {
            String elementString = Integer.toBinaryString(elements[i]);
            for (int j = 0; j < ELEMENT_SIZE - elementString.length(); ++j) {
                stringBuilder.append('0');
            }
            stringBuilder.append(elementString);
        }
        return stringBuilder.toString();
    }

    /**
     * @return the size of the vector
     */
    public int size() {
        return intSize() * ELEMENT_SIZE;
    }

    /**
     * Tests the bit of given index
     * @param index the bit index to check
     * @return a boolean, the value of the checked bit
     * @throws IndexOutOfBoundsException if {@code index} is invalid
     */
    public boolean testBit(int index) {
        int remainder = Math.floorMod(index, ELEMENT_SIZE);
        index = Math.floorDiv(index, ELEMENT_SIZE);
        return Bits.test(elements[index], remainder);
    }

    /**
     * Creates a new BitVector, the bitwise complement of the instance
     * @return the resulting BitVector
     */
    public BitVector not() {
        int[] newElements = new int[intSize()];
        for (int i = 0; i < intSize(); ++i) {
            newElements[i] = ~elements[i];
        }
        return new BitVector(newElements);
    }

    /**
     * Creates a new BitVector from the bitwise AND operation with another
     * instance of BitVector
     * @param that the other BitVector
     * @return the resulting BitVector
     * @throws IllegalArgumentException if the two vectors don't have the
     * same size
     */
    public BitVector and(BitVector that) {
        Preconditions.checkArgument(size() == that.size());
        int[] newElements = new int[intSize()];
        for (int i = 0; i < intSize(); ++i) {
            newElements[i] = elements[i] & that.elements[i];
        }
        return new BitVector(newElements);
    }

    /**
     * Creates a new BitVector from the bitwise OR operation with another
     * instance of BitVector
     * @param that the other BitVector
     * @return the resulting BitVector
     * @throws IllegalArgumentException if the two vectors don't have the
     * same size
     */
    public BitVector or(BitVector that) {
        Preconditions.checkArgument(size() == that.size());
        int[] newElements = new int[intSize()];
        for (int i = 0; i < intSize(); ++i) {
            newElements[i] = elements[i] | that.elements[i];
        }
        return new BitVector(newElements);
    }

    /**
     * Extracts a new BitVector from a given index and size, using
     * the zero-extended method
     * @param offsetIndex the starting index
     * @param size the size of the extracted vector
     * @return the resulting BitVector
     * @throws IllegalArgumentException if {@code size} is invalid
     */
    public BitVector extractZeroExtended(int offsetIndex, int size) {
        return extract(offsetIndex, size, ExtractMode.ZERO_EXTENDED);
    }

    /**
     * Extracts a new BitVector from a given index and size, using
     * the wrapped method
     * @param offsetIndex the starting index
     * @param size the size of the extracted vector
     * @return the resulting BitVector
     * @throws IllegalArgumentException if {@code size} is invalid
     */
    public BitVector extractWrapped(int offsetIndex, int size) {
        return extract(offsetIndex, size, ExtractMode.WRAPPED);
    }

    /**
     * Creates a new BitVector using a logic shift of a given distance
     * @param distance the distance to shift
     * @return the resulting BitVector
     */
    public BitVector shift(int distance) {
        return extractZeroExtended(-distance, size());
    }

    private int intSize() {
        return elements.length;
    }

    private BitVector extract(int offsetIndex, int size, ExtractMode mode) {
        Preconditions.checkArgument(size > 0 && size % ELEMENT_SIZE == 0);
        int[] elements = new int[size / ELEMENT_SIZE];
        for (int i = 0; i < elements.length; ++i) {
            int index = i * ELEMENT_SIZE + offsetIndex;
            elements[i] = getExtractedElement(index, mode);
        }
        return new BitVector(elements);
    }

    private int getExtractedElement(int index, ExtractMode mode) {
        int remainder = Math.floorMod(index, ELEMENT_SIZE);
        int intIndex = Math.floorDiv(index, ELEMENT_SIZE);
        if (remainder == 0) {
            switch (mode) {
                case ZERO_EXTENDED: {
                    if (intIndex < 0) return 0;
                    return intIndex < intSize() ? elements[intIndex] : 0;
                }
                case WRAPPED: {
                    return elements[intIndex % intSize()];
                }
                default: throw new IllegalArgumentException();
            }
        } else {
            int b = getExtractedElement((intIndex + 1) * ELEMENT_SIZE, mode);
            int a = getExtractedElement(intIndex * ELEMENT_SIZE, mode);
            return (b << (ELEMENT_SIZE - remainder)) | (a >>> (remainder));
        }
    }

    /**
     * BitVector.Builder
     *
     * A class used to construct a BitVector from scratch
     */
    public static final class Builder {

        private int[] elements;

        private static final int BYTE_MASK = 0b1111_1111;

        /**
         * Creates a new BitVector.Builder with given size
         * @param size the size of the BitVector
         * @throws IllegalArgumentException if {@code size} is invalid
         */
        public Builder(int size) {
            Preconditions.checkArgument(size > 0 && size % ELEMENT_SIZE == 0);
            elements = new int[size / ELEMENT_SIZE];
        }

        /**
         * Sets the byte of given index
         * @param index the index of the byte
         * @param b the value to set
         * @return the builder instance
         * @throws IndexOutOfBoundsException if {@code index} is invalid
         * @throws IllegalArgumentException if {@code b} is not an 8-bit value
         * @throws IllegalStateException if the instance has already been built
         */
        public Builder setByte(int index, int b) {
            ensureNotBuilt();
            int length = ELEMENT_SIZE / Byte.SIZE;
            Objects.checkIndex(index, elements.length * length);
            Preconditions.checkBits8(b);
            int elementIndex = index / length;
            int byteOffset = (index % length) * Byte.SIZE;

            int newElement = elements[elementIndex];
            newElement &= (~(BYTE_MASK << byteOffset));
            newElement |= (b << byteOffset);
            elements[elementIndex] = newElement;

            return this;
        }

        /**
         * Builds the BitVector
         * @return the built BitVector
         * @throws IllegalStateException if the instance has already been built
         */
        public BitVector build() {
            ensureNotBuilt();
            BitVector vector = new BitVector(elements);
            elements = null;
            return vector;
        }

        private void ensureNotBuilt() {
            if (elements == null)
                throw new IllegalStateException();
        }

    }

}
