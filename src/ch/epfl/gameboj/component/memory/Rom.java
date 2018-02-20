package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.component.Component;

import java.util.Arrays;
import java.util.Objects;

public class Rom implements Component {

    private byte[] data;

    public Rom(byte[] data) {
        Objects.requireNonNull(data);
        this.data = Arrays.copyOf(data, data.length);
    }

    public int size() {
        return this.data.length;
    }

    @Override
    public int read(int index) {
        byte v = this.data[index];
        return Byte.toUnsignedInt(v);
    }

    @Override
    public void write(int index, int value) {}

}
