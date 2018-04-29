package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

import java.util.Objects;

/**
 * LcdImageLine
 *
 * An immutable class that represents a line of pixels
 * encoded with two colors bits and an opacity bit.
 *
 * @author Ulysse Ramage (282300)
 */
public final class LcdImageLine {

    private final BitVector msb, lsb, opacity;

    private static final int IDENTITY_PALETTE = 0b11_10_01_00;
    private static final int COLORS = 4;

    /**
     * Creates a new LcdImageLine given 3 bit vectors
     * @param msb the msb vector
     * @param lsb the lsb vector
     * @param opacity the opacity vector
     * @throws IllegalArgumentException if {@code msb}, {@code lsb}
     * and {@code opacity} don't have the same size
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
        Preconditions.checkArgument(
                (msb.size() == lsb.size())
                && (lsb.size() == opacity.size())
        );
        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof LcdImageLine))
            return false;
        LcdImageLine thatImageLine = (LcdImageLine) that;
        return (
                msb.equals(thatImageLine.msb)
                && lsb.equals(thatImageLine.lsb)
                && opacity.equals(thatImageLine.opacity)
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return msb.hashCode() + lsb.hashCode() + opacity.hashCode();
    }

    /**
     * @return the size of the image line
     */
    public int size() {
        return msb.size();
    }

    /**
     * @return the msb bit vector
     */
    public BitVector msb() {
        return msb;
    }

    /**
     * @return the lsb bit vector
     */
    public BitVector lsb() {
        return lsb;
    }

    /**
     * @return the opacity bit vector
     */
    public BitVector opacity() {
        return opacity;
    }

    /**
     * Shifts the image line of a given distance
     * @param distance the distance to shift
     * @return the shifted image line
     */
    public LcdImageLine shift(int distance) {
        return new LcdImageLine(
                msb.shift(distance),
                lsb.shift(distance),
                opacity.shift(distance)
        );
    }

    /**
     * Extracts a new LcdImageLine from a given index and size, using
     * the wrapped method
     * @param offsetIndex the starting index
     * @param size the size of the extracted LcdImageLine
     * @return the extracted LcdImageLine
     * @throws IllegalArgumentException if {@code size} is invalid
     */
    public LcdImageLine extractWrapped(int offsetIndex, int size) {
        return new LcdImageLine(
                msb.extractWrapped(offsetIndex, size),
                lsb.extractWrapped(offsetIndex, size),
                opacity.extractWrapped(offsetIndex, size)
        );
    }

    /**
     * Combines two lines using the provided opacity vector (if transparent,
     * the resulting pixels are those of the below line, and vice-versa)
     * @param above the above LcdImageLine
     * @param opacity the opacity vector
     * @return a new LcdImageLine created by combining the two lines
     * @throws NullPointerException if either {@code above}
     * or {@code opacity} is null
     */
    public LcdImageLine below(LcdImageLine above, BitVector opacity) {
        Objects.requireNonNull(above);
        Objects.requireNonNull(opacity);
        BitVector notOpacity = opacity.not();
        BitVector msb = above.msb.and(opacity).or(msb().and(notOpacity));
        BitVector lsb = above.lsb.and(opacity).or(lsb().and(notOpacity));
        return new LcdImageLine(msb, lsb, msb.or(lsb));
    }

    /**
     * The opacity vector defaults to the above image line's
     * @see #below(LcdImageLine, BitVector)
     */
    public LcdImageLine below(LcdImageLine above) {
        Objects.requireNonNull(above);
        return below(above, above.opacity);
    }

    /**
     * Joins two lines, separated at a given index
     * @param other the other LcdImageLine
     * @param index the separating index
     * @return a new LcdImageLine created by joining the two lines
     * @throws NullPointerException if {@code other} is null
     */
    public LcdImageLine join(LcdImageLine other, int index) {
        Objects.requireNonNull(other);
        return below(
                other,
                new BitVector(size(), true).shift(index)
        );
    }

    /**
     * Maps the colors of the LcdImageLine using a given palette
     * @param palette the palette (color map)
     * @return a LcdImageLine with the mapped colors
     */
    public LcdImageLine mapColors(int palette) {
        if (palette == IDENTITY_PALETTE)
            return this;
        BitVector newMsb = new BitVector(size());
        BitVector newLsb = new BitVector(size());
        for (int fromColor = 0; fromColor < COLORS; ++fromColor) {
            int toColor = Bits.extract(palette, 2 * fromColor, 2);
            BitVector apply = (Bits.test(fromColor, 1) ? msb : msb.not())
                    .and(Bits.test(fromColor, 0) ? lsb : lsb.not());
            if (Bits.test(toColor, 1)) newMsb = newMsb.or(apply);
            if (Bits.test(toColor, 0)) newLsb = newLsb.or(apply);
        }
        return new LcdImageLine(newMsb, newLsb, opacity);
    }

    /**
     * LcdImageLine.Builder
     *
     * A class used to construct an LcdImageLine from scratch
     */
    public static final class Builder {

        private BitVector.Builder msbBuilder, lsbBuilder;

        /**
         * Creates a new LcdImageLine.Builder with a given size
         * @param size the size of the resulting LcdImageLine
         * @throws IllegalArgumentException if {@code size} is invalid
         */
        public Builder(int size) {
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
        }

        /**
         * Sets the bytes of the MSB and LSB
         * @param index
         * @param mb the bytes of the MSB
         * @param lb the bytes of the LSB
         * @return the builder instance
         * @throws IllegalStateException if the instance has already been built
         */
        public Builder setBytes(int index, int mb, int lb) {
            msbBuilder.setByte(index, mb);
            lsbBuilder.setByte(index, lb);
            return this;
        }

        /**
         * Builds the LcdImageLine
         * @return the built LcdImageLine
         * @throws IllegalStateException if the instance has already been built
         */
        public LcdImageLine build() {
            BitVector msb = msbBuilder.build();
            BitVector lsb = lsbBuilder.build();
            return new LcdImageLine(msb, lsb, msb.or(lsb));
        }

    }

}
