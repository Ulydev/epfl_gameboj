package ch.epfl.gameboj.component.memory;

public class Ram {

    private byte[] data;

    public Ram(int size) {
        //TODO: IllegalArgumentException if size < 0
        this.data = new byte[size];
    }

    public int size() {
        return this.data.length;
    }

    public int read(int index) {
        //TODO: check IndexOutOfBoundsException
        byte v = this.data[index];
        return Byte.toUnsignedInt(v);
    }

    public void write(int index, int value) {
        //TODO: check IndexOutOfBoundsException, IllegalArgumentException if value isn't 8 bits
        this.data[index] = (byte)value;
    }

}
