package ch.epfl.gameboj;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.DebugPrintComponent;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Opcode;

import java.io.File;
import java.io.IOException;

public final class DebugMain {

    public static void main(String[] args) throws IOException {

        long cycles = 30000000;

        for (String filePath : args) {
            File romFile = new File(filePath);

            GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));
            Component printer = new DebugPrintComponent();
            printer.attachTo(gb.bus());
            while (gb.cycles() < cycles) {
                long nextCycles = Math.min(gb.cycles() + 17556, cycles);
                gb.runUntil(nextCycles);
                gb.cpu().requestInterrupt(Cpu.Interrupt.VBLANK);
            }
        }
    }

}