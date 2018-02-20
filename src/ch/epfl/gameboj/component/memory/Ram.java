package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

public class Ram implements Component {

    private byte[] data;

    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);
        this.data = new byte[size];
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
    public void write(int index, int value) {
        Preconditions.checkBits8(value);
        this.data[index] = (byte)value;
    }

}
