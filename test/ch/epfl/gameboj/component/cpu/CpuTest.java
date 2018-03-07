package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import ch.epfl.gameboj.component.memory.Rom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CpuTest {

    private int[] getProgramFromString(String program) {
        int[] programBytes = new int[(program.length() + 1) / 3];
        int i = 0;
        for (String instruction: program.split(" ")) {
            programBytes[i++] = Integer.parseInt(instruction, 16);
        }
        return programBytes;
    }

    private void assertProgramOutput(String program, int[] expectedOutput) {
        Cpu cpu = new Cpu();
        Ram ram = new Ram(AddressMap.WORK_RAM_SIZE);
        Bus bus = connect(cpu, ram);

        int[] programBytes = getProgramFromString(program);
        for (int i = 0; i < programBytes.length; ++i) {
            bus.write(i, programBytes[i]);
        }

        cycleCpuTillPC(cpu, programBytes.length);

        int[] output = cpu._testGetPcSpAFBCDEHL();
        for (int i = 0; i < expectedOutput.length; ++i) {
            assertEquals(expectedOutput[i], output[i]);
        }
    }

    private Bus connect(Cpu cpu, Ram ram) {
        RamController ramController = new RamController(ram, 0);
        Bus bus = new Bus();
        cpu.attachTo(bus);
        ramController.attachTo(bus);
        return bus;
    }

    private void cycleCpu(Cpu cpu, long cycles) {
        for (long cycle = 0; cycle < cycles; ++cycle) {
            cpu.cycle(cycle);
        }
    }

    private void cycleCpuTillPC(Cpu cpu, int PC) {
        int cycle = 0;
        while (cpu._testGetPcSpAFBCDEHL()[0] < PC) {
            cpu.cycle(cycle++);
        }
    }

    @Test
    void cpuExecutesCorrectNumberOfInstructions() {
        assertProgramOutput(String.join(
                " ",
                "00",
                "06 00",
                "3E 01",
                "0E 0A",
                "57",
                "80",
                "42",
                "0D",
                "C2 06 00",
                "76"
        ), new int[] {
                15
        });
    }

    @Test
    void cpuExecutesFibonacciCorrectly() {
        assertProgramOutput(String.join(
                " ",
                "00",
                "06 00",
                "3E 01",
                "0E 0A",
                "57",
                "80",
                "42",
                "0D",
                "C2 06 00",
                "76"
        ), new int[] {
                15,
                0,
                89 // Expected result
        });
    }

    @Test
    void cpuStoresCorrectValueInRegisterA() {
        assertProgramOutput(String.join(
                " ",
                "00",
                "3E 08"             // A = 08
        ), new int[] {
                3, 0, 8             // A == 08
        });
    }

    @Test
    void cpuReadsValueAtHl() {
        assertProgramOutput(String.join(
                " ",
                "00",
                "21 01 00",         // HL = 00 01
                "23",               // HL += 1
                "7E"                // A = BUS[HL]
        ), new int[] {
                6, 0, 1             // A = 01
        });
    }

    @Test
    void cpuAddsValueCorrectly() {
        assertProgramOutput(String.join(
                " ",
                "00",
                "3E 02",            // A = 02
                "C6 07"             // A += 07
        ), new int[] {
                5, 0, 9             // A == 09
        });
    }

    @Test
    void programFailsForUnknownOpcode() {
        assertThrows(NullPointerException.class,
                () -> assertProgramOutput("00 FD", new int[]{}));
    }

    @Test
    void nopDoesNothing() {
        assertProgramOutput(String.join(
                " ",
                "00"
        ), new int[] {
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0
        });
    }

}

