package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;

import java.util.ArrayList;
import java.util.Objects;

public final class Bus {

    private ArrayList<Component> attachedComponents = new ArrayList<>();

    public void attach(Component component) {
        Objects.requireNonNull(component);
        attachedComponents.add(component);
    }

    public int read(int address) {
        Preconditions.checkBits16(address);
        for (Component component : attachedComponents) {
            int value = component.read(address);
            if (value != Component.NO_DATA)
                return value;
        }
        return 0xFF;
    }

    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        for (Component component : attachedComponents) {
            component.write(address, data);
        }
    }



}
