package ch.epfl.gameboj.component;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;

import java.util.Objects;

/**
 * Timer
 *
 * A class representing a Timer component that keeps track of
 * the elapsed time
 *
 * @author Ulysse Ramage (282300)
 */
public final class Timer implements Component, Clocked {

    private final Cpu cpu;

    private int counter, TIMA, TMA, TAC;

    /**
     * Creates a new Timer associated with a Cpu
     * @param cpu the cpu associated to the timer
     * @throws NullPointerException if {@code cpu} is null
     */
    public Timer(Cpu cpu) {
        Objects.requireNonNull(cpu);
        this.cpu = cpu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        switch (address) {
            case AddressMap.REG_DIV: {
                return Bits.extract(counter, 8, 8);
            }
            case AddressMap.REG_TIMA: return TIMA;
            case AddressMap.REG_TMA: return TMA;
            case AddressMap.REG_TAC: return TAC;
            default: return Component.NO_DATA;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        boolean previousState = state();
        switch (address) {
            case AddressMap.REG_DIV: counter = 0; break;
            case AddressMap.REG_TIMA: TIMA = data; break;
            case AddressMap.REG_TMA: TMA = data; break;
            case AddressMap.REG_TAC: TAC = data; break;
        }
        incIfChange(previousState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cycle(long cycle) {
        boolean previousState = state();
        counter = Bits.clip(16, counter + 4);
        incIfChange(previousState);
    }

    private boolean state() {
        boolean active = Bits.test(TAC, 2);
        boolean condition = Bits.test(counter, getTACIndex());
        return active && condition;
    }

    private void incIfChange(boolean previousState) {
        if (previousState && !state()) {
            if (TIMA == 0xFF) {
                cpu.requestInterrupt(Cpu.Interrupt.TIMER);
                TIMA = TMA;
            } else {
                TIMA++;
            }
        }
    }

    private int getTACIndex() {
        int index = Bits.clip(2, TAC);
        switch (index) {
            case 0b00: return 9;
            case 0b01: return 3;
            case 0b10: return 5;
            case 0b11: return 7;
            default: throw new IllegalArgumentException();
        }
    }

}
