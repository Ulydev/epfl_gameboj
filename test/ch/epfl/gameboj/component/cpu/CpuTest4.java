package ch.epfl.gameboj.component.cpu;

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

class CpuTest4 {

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

            for(int i = 0; get(this, Reg.PC) < instruction.length ; ++i)
                cpu.cycle(i);

        }

        public Bus bus;
        public RamController workRam;
        public Cpu cpu;
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
    void ADD_A_N8works(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42, Opcode.ADD_A_N8.encoding, 0x42};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0x84,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void ADD_A_HLRworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.ADD_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0x42);
        gb.execute(instruction);

        assertArrayEquals(new int[]{6,0,0x84,0,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void ADC_A_HLRworks(){
        int[] instruction = new int[]{Opcode.SCF.encoding,Opcode.LD_A_N8.encoding, 0x42,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.ADC_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0x42);
        gb.execute(instruction);

        assertArrayEquals(new int[]{7,0,0x84+1,0,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void INC_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0xFF, Opcode.INC_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0,160,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void INC_R16works(){
        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding, 0xFF,0xFF, Opcode.INC_SP.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);
        assertArrayEquals(new int[]{4,0,0,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void ADD_HL_R16works(){
        int[] instruction = new int[]{Opcode.LD_HL_N16.encoding, 0xFF, 0xFF ,Opcode.LD_SP_N16.encoding, 0x01, 0x00 , Opcode.ADD_HL_SP.encoding};

        CpuWithRam gb = new CpuWithRam();

        gb.execute(instruction);

        assertArrayEquals(new int[]{7,1,0,48,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void ADD_SP_Nworks(){
        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding, 40, 0x00 , Opcode.ADD_SP_N.encoding, 0b11011000};

        CpuWithRam gb = new CpuWithRam();

        gb.execute(instruction);

        assertArrayEquals(new int[]{5,0,0,48,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void ADD_HL_E8works(){
        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding, 40, 0x00 , Opcode.LD_HL_SP_N8.encoding, 0b11011000};

        CpuWithRam gb = new CpuWithRam();

        gb.execute(instruction);

        assertArrayEquals(new int[]{5,40,0,48,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SUB_A_N8works(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42, Opcode.SUB_A_N8.encoding, 0x42};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,192,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SUB_A_R8works(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42,Opcode.LD_C_N8.encoding, 0x42, Opcode.SUB_A_C.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{5,0,0,192,0,0x42,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SUB_A_HLRworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.SUB_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0x42);
        gb.execute(instruction);

        assertArrayEquals(new int[]{6,0,0,192,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SBC_A_HLRworks(){
        int[] instruction = new int[]{Opcode.SCF.encoding,Opcode.LD_A_N8.encoding, 0x42,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.SBC_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0x42);
        gb.execute(instruction);

        assertArrayEquals(new int[]{7,0,0xFF,64+32+16,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void DEC_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0, Opcode.DEC_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0,96,0,0,0xFF,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void CP_A_R8works(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x42, Opcode.LD_D_N8.encoding, 0x36, Opcode.CP_A_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{5,0,0x42,96,0,0,0x36,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void DEC_R16works(){
        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding, 0,0, Opcode.DEC_SP.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0xFFFF,0,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void AND_A_HLRworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x0F,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.AND_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0xF0);
        gb.execute(instruction);

        assertArrayEquals(new int[]{6,0,0,160,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void OR_A_HLRworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0x0F,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.OR_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0xF0);
        gb.execute(instruction);

        assertArrayEquals(new int[]{6,0,0xFF,0,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void XOR_A_HLRworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0xEF,Opcode.LD_HL_N16.encoding, 0x42, 0x35 , Opcode.XOR_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x3542,0x0F);
        gb.execute(instruction);

        assertArrayEquals(new int[]{6,0,0xE0,0,0,0,0,0,0x35,0x42},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void CPLworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0xEF,Opcode.CPL.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0x10,96 ,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SLA_R8works(){
        int[] instruction = new int[]{Opcode.LD_E_N8.encoding, 0b10001111,0xCB,Opcode.SLA_E.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,16,0,0,0,0b00011110,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SRA_R8works(){
        int[] instruction = new int[]{Opcode.LD_E_N8.encoding, 0b10001111,0xCB,Opcode.SRA_E.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,16,0,0,0,0b11000111,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SRL_R8works(){
        int[] instruction = new int[]{Opcode.LD_E_N8.encoding, 0b10001111,0xCB,Opcode.SRL_E.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,16,0,0,0,0b01000111,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void RLCAworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0b10001111,Opcode.RLCA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0b00011111,16,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void RLCAworks2(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0b0,Opcode.RLCA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void RLAworks(){
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding, 0b10001111,Opcode.RLA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{3,0,0b00011110,16,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void RL_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0b00001111,0xCB,Opcode.RL_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,0,0,0,0b00011110,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void BIT_N3_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0b00001111,0xCB,Opcode.BIT_3_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,32,0,0,0b00001111,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SET_N3_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0b00001111,0xCB,Opcode.SET_5_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,0,0,0,0b00101111,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void RES_N3_R8works(){
        int[] instruction = new int[]{Opcode.LD_D_N8.encoding, 0b00001111,0xCB,Opcode.RES_3_D.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{4,0,0,0,0,0,0b00000111,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    void SCCF_FamilyWorks(){
        int[] instruction = new int[]{Opcode.SCF.encoding, Opcode.CCF.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertArrayEquals(new int[]{2,0,0,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }
}