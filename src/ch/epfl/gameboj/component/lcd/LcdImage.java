package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LcdImage {

    private int width, height;
    private List<LcdImageLine> lines;

    /**
     * Creates a new LcdImage with given dimensions from a list of lines
     * @param width the width of the image
     * @param height the height of the image
     * @param lines the list of lines to create the image with
     * @throws IllegalArgumentException if either {@code width}
     * or {@code height} is invalid
     */
    public LcdImage(int width, int height, List<LcdImageLine> lines) {
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        this.width = width;
        this.height = height;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof LcdImage))
            return false;
        return lines.equals(((LcdImage) that).lines);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return lines.hashCode();
    }

    /**
     * @return the width of the image
     */
    public int width() {
        return width;
    }

    /**
     * @return the height of the image
     */
    public int height() {
        return height;
    }

    /**
     * Gets the value at a given position by combining the values of
     * the MSB and the LSB
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     * @return the integer value at position (x, y)
     */
    public int get(int x, int y) {
        LcdImageLine line = lines.get(y);
        int msb = line.msb().testBit(x) ? 1 : 0;
        int lsb = line.lsb().testBit(x) ? 1 : 0;
        return (msb << 1) + lsb;
    }

    /**
     * LcdImage.Builder
     *
     * A class used to create a LcdImage from scratch
     */
    public static final class Builder {

        int width, height;
        private List<LcdImageLine> lines;

        /**
         * Creates a new LcdImage.Builder with given dimensions
         * @param width the width of the image
         * @param height the height of the image
         */
        public Builder(int width, int height) {
            Preconditions.checkArgument(width > 0);
            Preconditions.checkArgument(height > 0);
            this.width = width;
            this.height = height;

            BitVector emptyVector = new BitVector(width);
            lines = new ArrayList<>(
                    Collections.nCopies(
                            height,
                            new LcdImageLine(
                                emptyVector,
                                emptyVector,
                                emptyVector
                            )
                    )
            );
        }

        /**
         * Sets the line of i-th index
         * @param index the index of the line
         * @param line the line to replace
         * @return the builder instance
         * @throws IndexOutOfBoundsException if {@code index} is invalid
         * @throws NullPointerException if {@code line} is null
         * @throws IllegalArgumentException if {@code line} isn't
         * the same width as the image
         */
        public Builder setLine(int index, LcdImageLine line) {
            Objects.checkIndex(index, height);
            Objects.requireNonNull(line);
            Preconditions.checkArgument(line.size() == width);
            lines.set(index, line);
            return this;
        }

        /**
         * Builds the LcdImage
         * @return the built LcdImage
         */
        public LcdImage build() {
            return new LcdImage(width, height, lines);
        }

    }

}
