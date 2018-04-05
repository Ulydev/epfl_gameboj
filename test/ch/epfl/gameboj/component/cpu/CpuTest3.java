package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Opcode;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import org.junit.jupiter.api.Test;

import static ch.epfl.gameboj.bits.Bits.make16;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CpuTest3 {

    private class CpuWithRam{

        public CpuWithRam() {
            Ram ram = new Ram(0xFFFF);
            this.workRam = new RamController(ram,0);
            this.cpu = new Cpu();
            this.bus = new Bus();
            cpu.attachTo(bus);
            workRam.attachTo(bus);

        }

        private void execute(int[] instruction) {

            for(int i = 0 ; i < instruction.length ; ++i) {
                bus.write(i,instruction[i]);
            }

            for(int i = 0; this.bus.read(this.cpu._testGetPcSpAFBCDEHL()[0]) != Opcode.HALT.encoding; ++i)
                cpu.cycle(i);

        }

        public Bus bus;
        public RamController workRam;
        public Cpu cpu;
    }

    static Cpu newCpu(Bus bus){
        Cpu cpu = new Cpu();
        Ram ram = new Ram(65534);
        RamController ramController = new RamController(ram, 0);
        ramController.attachTo(bus);
        cpu.attachTo(bus);

        return cpu;
    }

    private enum Reg implements Register{
        PC,SP,A,F,B,C,D,E,H,L
    }

    private enum Reg16 implements Register {
        BC(Reg.B, Reg.C), DE(Reg.D, Reg.E), HL(Reg.H, Reg.L), AF(Reg.A, Reg.F);

        public final Reg Lsb;
        public final Reg Hsb;

        Reg16(Reg Hsb,Reg Lsb) {
            this.Hsb = Hsb;
            this.Lsb = Lsb;
        }
    }

    private int get(CpuWithRam gb,Reg r) {
        return gb.cpu._testGetPcSpAFBCDEHL()[r.index()];
    }

    private int get(CpuWithRam gb,Reg16 r) {
        return make16(get(gb,r.Hsb),get(gb,r.Lsb));
    }

    @Test
    void fibRecursionWorks(){
        int[] instruction = new int[]{
                0x31, 0xFF, 0xFF, 0x3E,
                0x0B, 0xCD, 0x0A, 0x00,
                0x76, 0x00, 0xFE, 0x02,
                0xD8, 0xC5, 0x3D, 0x47,
                0xCD, 0x0A, 0x00, 0x4F,
                0x78, 0x3D, 0xCD, 0x0A,
                0x00, 0x81, 0xC1, 0xC9,
        };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);
        assertArrayEquals(new int[]{8,65535,89,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void fibLoopWorks(){
        int[] instruction = new int[]{
                0x06, 0x00, 0x3E, 0x01, 0x0E, 0x0A, 0x57, 0x80, 0x42, 0x0D, 0xC2, 0x06, 0x00, 0x76
        };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);
        assertArrayEquals(new int[]{13,0,89,192,55,0,55,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void interruptionsWorks(){
        Bus bus = new Bus();
        Cpu cpu = newCpu(bus);

        bus.write(0, Opcode.EI.encoding);
        bus.write(AddressMap.REG_IE,1);
        bus.write(AddressMap.REG_IF,1);
        bus.write(0x40, Opcode.LD_A_N8.encoding);
        bus.write(0x41, 42);
        bus.write(0x42, Opcode.ADD_A_N8.encoding);
        bus.write(0x43, 42);
        bus.write(0x44, Opcode.RET_NZ.encoding);
        bus.write(1, Opcode.SUB_A_N8.encoding);
        bus.write(2, 42);
        bus.write(3, Opcode.HALT.encoding);

        int i = 0;
        for(; bus.read(cpu._testGetPcSpAFBCDEHL()[0]) != Opcode.HALT.encoding; ++i)
            cpu.cycle(i);

        assertEquals(i, 16);
        assertArrayEquals(new int[]{3,0,42,96,0,0,0,0,0,0}, cpu._testGetPcSpAFBCDEHL());
    }
}