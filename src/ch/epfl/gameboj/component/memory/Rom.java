package ch.epfl.gameboj.component.memory;

import java.util.Arrays;

public class Rom {

    private byte[] data;

    public Rom(byte[] data) {
        this.data = Arrays.copyOf(data, data.length);
    }

    public int size() {
        return this.data.length;
    }

    public int read(int index) {
        //TODO: check IndexOutOfBoundsException
        byte v = this.data[index];
        return Byte.toUnsignedInt(v);
    }

}
