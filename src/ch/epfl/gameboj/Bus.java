package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Bus
 *
 * A class representing a Bus that can be used to connect components together
 *
 * @author Ulysse Ramage (282300)
 */
public final class Bus {

    private final ArrayList<Component> attachedComponents = new ArrayList<>();

    /**
     * Attaches a component to the Bus
     * @param component a Component
     * @throws NullPointerException if {@code component} is null
     */
    public void attach(Component component) {
        Objects.requireNonNull(component);
        attachedComponents.add(component);
    }

    /**
     * Reads the value of the attached components at the given address
     * @param address the address to read data at
     * @return the value stored at {@code address} if at least one of the
     * components attached to the bus have a value at {@code address}, else 0xFF
     * @throws IllegalArgumentException if {@code address} isn't 16-bit
     */
    public int read(int address) {
        Preconditions.checkBits16(address);
        for (Component component : attachedComponents) {
            int value = component.read(address);
            if (value != Component.NO_DATA)
                return value;
        }
        return 0xFF;
    }

    /**
     * Writes a value at the given address to all the attached components
     * @param address the address to write at
     * @param data the data to write
     * @throws IllegalArgumentException if {@code address} isn't 16-bit,
     * or {@code data} isn't 8-bit
     */
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        for (Component component : attachedComponents) {
            component.write(address, data);
        }
    }



}
