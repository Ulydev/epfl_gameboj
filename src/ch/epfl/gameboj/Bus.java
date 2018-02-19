package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;

import java.util.ArrayList;
import java.util.Objects;

public class Bus {

    ArrayList<Component> attachedComponents = new ArrayList<>();

    public void attach(Component component) {
        Objects.requireNonNull(component);
        attachedComponents.add(component);
    }

    public int read(int address) {
        //TODO: IllegalArgumentException if address isn't 16 bits
        for (Component component : attachedComponents) {
            int value = component.read(address);
            if (value != Component.NO_DATA)
                return value;
        }
        return 0xFF;
    }

    public void write(int address, int data) {
        //TODO: IllegalArgumentException if address isn't 16 bits, or data isn't 8 bits
        for (Component component : attachedComponents) {
            component.write(address, data);
        }
    }



}
