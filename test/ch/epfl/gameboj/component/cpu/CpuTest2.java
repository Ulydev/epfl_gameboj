package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;
import org.junit.jupiter.api.Test;
import static ch.epfl.gameboj.bits.Bits.*;

import static org.junit.jupiter.api.Assertions.*;

class CpuTest2 {

    private static int OPCODE_PREFIX = 0xCB;

    private enum Flag implements Bit {
        UNUSED_0,UNUSED_1,UNUSED_2,UNUSED_3,C,H,N,Z
    }

    private class CpuWithRam{

        public CpuWithRam() {
            Ram ram = new Ram(0xFFFF);
            this.workRam = new RamController(ram,0);
            this.cpu = new Cpu();
            this.bus = new Bus();
            cpu.attachTo(bus);
            workRam.attachTo(bus);

        }

        private boolean getFanion(Flag f) {
            return Bits.test(get(this,Reg.F),f);
        }



        private void execute(int[] instruction) {

            for(int i = 0 ; i < instruction.length ; ++i) {
                bus.write(i,instruction[i]);
            }

            for(int i = 0 ; get(this,Reg.PC) < instruction.length ; ++i)
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
        BC(Reg.B,Reg.C), DE(Reg.D,Reg.E), HL(Reg.H, Reg.L), AF(Reg.A,Reg.F);

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
    public void opcodeNOPWorks() {
        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0,Opcode.NOP.encoding);
        gb.cpu.cycle(0);
        assertArrayEquals(new int[]{1,0,0,0,0,0,0,0,0,0},gb.cpu._testGetPcSpAFBCDEHL());
    }

    @Test
    public void opcodeLD_R8_HLRWorks() {
        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0,Opcode.LD_A_HLR.encoding);
        gb.cpu.cycle(0);
        assertArrayEquals(new int[]{Opcode.LD_A_HLR.totalBytes,0,Opcode.LD_A_HLR.encoding,0,0,0,0,0,0,0}
                ,gb.cpu._testGetPcSpAFBCDEHL());
    }


    @Test
    public void opcodeLD_A_HLRIWorks() {

        int[] instruction = new int[]{Opcode.LD_A_HLRI.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(1,get(gb,Reg.L));
        assertEquals(instruction[0],get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_HLRDWorks() {

        int[] instruction = new int[]{Opcode.LD_L_N8.encoding,0x10,Opcode.LD_A_HLRD.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10,0x42);
        gb.execute(instruction);

        assertEquals(0xF,get(gb,Reg.L));
        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_N8RWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8R.encoding,0x10};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10 + 0xFF00,0x42);
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_CRWorks() {

        int[] instruction = new int[]{Opcode.LD_C_N8.encoding,0x10,Opcode.LD_A_CR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10 + 0xFF00,0x42);
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_N16RRWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N16R.encoding,0x10,0x00};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10,0x42);
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_BCRWorks() {

        int[] instruction = new int[]{Opcode.LD_B_N8.encoding,0x10,Opcode.LD_C_N8.encoding,0xFF,Opcode.LD_A_BCR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10FF,0x42);
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_A_DERWorks() {

        int[] instruction = new int[]{Opcode.LD_D_N8.encoding,0x10,Opcode.LD_E_N8.encoding,0xF0,Opcode.LD_A_DER.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10F0,0x42);
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
    }

    @Test
    public void opcodeLD_R8_N8Works() {

        int[] instruction = new int[]{Opcode.LD_D_N8.encoding,0x10,Opcode.LD_B_N8.encoding,0xF0};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,get(gb,Reg.D));
        assertEquals(0xF0,get(gb,Reg.B));
    }

    @Test
    public void opcodeLD_R16SP_N16Works() {

        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding,0x10,0x20,Opcode.LD_HL_N16.encoding,0xF0,0x55};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x55F0,get(gb,Reg16.HL));
        assertEquals(0x2010,get(gb,Reg.SP));
    }

    @Test
    public void opcodePOP_R16Works() {

        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding,0x10,0xFF,Opcode.POP_AF.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0xFF10,0x32);
        gb.bus.write(0xFF11,0x42);
        gb.execute(instruction);

        assertEquals(0x4230,get(gb,Reg16.AF));
        assertEquals(0xFF12,get(gb,Reg.SP));
    }

    @Test
    public void opcodeLD_HLR_R8Works() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x42,Opcode.LD_HL_N16.encoding,0xFA,0x66,Opcode.LD_HLR_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x42,gb.bus.read(0x66FA));
    }

    @Test
    public void opcodeLD_HLRU_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x42,Opcode.LD_HL_N16.encoding,0xFA,0x66,Opcode.LD_HLRI_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x42,gb.bus.read(0x66FA));
        assertEquals(0x66FB,get(gb,Reg16.HL));
    }

    @Test
    public void opcodeLD_N8R_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x10,Opcode.LD_N8R_A.encoding,0xF0};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,gb.bus.read(0xFF00 + 0xF0));
    }

    @Test
    public void opcodeLD_CR_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x10,Opcode.LD_C_N8.encoding,0xF0,Opcode.LD_CR_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,gb.bus.read(0xFF00 + 0xF0));
    }

    @Test
    public void opcodeLD_N16R_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x10,Opcode.LD_N16R_A.encoding,0XF0,0xFA};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,gb.bus.read(0xFAF0));
    }

    @Test
    public void opcodeLD_BCR_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x10,Opcode.LD_BC_N16.encoding,0xF0,0xFA,Opcode.LD_BCR_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,gb.bus.read(0xFAF0));
    }

    @Test
    public void opcodeLD_DER_AWorks() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x10,Opcode.LD_DE_N16.encoding,0xF0,0xFA,Opcode.LD_DER_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x10,gb.bus.read(0xFAF0));
    }


    @Test
    public void opcodeLD_HLR_N8Works() {

        int[] instruction = new int[]{Opcode.LD_L_N8.encoding,0x10,Opcode.LD_HLR_N8.encoding,0xFF};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xFF,gb.bus.read(0x10));
    }

    @Test
    public void opcodeLD_N16R_SPWorks() {

        int[] instruction = new int[]{Opcode.LD_SP_N16.encoding,0x42,0x43,Opcode.LD_N16R_SP.encoding,0xF0,0xFA};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x42,gb.bus.read(0xFAF0));
        assertEquals(0x43,gb.bus.read(0xFAF1));
    }



    @Test
    public void opcodeLD_R8_R8Works() {

        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0x42,Opcode.LD_B_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x42,get(gb,Reg.A));
        assertEquals(0x42,get(gb,Reg.B));
    }

    @Test
    public void opcodeLD_SP_HLWorks() {

        int[] instruction = new int[]{Opcode.LD_HL_N16.encoding,0x42,0x43,Opcode.LD_SP_HL.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x4342,get(gb,Reg.SP));
        assertEquals(0x4342,get(gb,Reg16.HL));
    }

    @Test
    public void opcodePUSH_R16Works() {

        int[] instruction = new int[]{Opcode.LD_BC_N16.encoding,0x42,0x43,Opcode.LD_SP_N16.encoding,0xF5,0xFA,Opcode.PUSH_BC.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x4342,get(gb,Reg16.BC));
        assertEquals(0xFAF3,get(gb,Reg.SP));
        assertEquals(0x42,gb.bus.read(0xFAF3));
        assertEquals(0x43,gb.bus.read(0xFAF4));
    }

    // week 4

    @Test
    public void opcodeADD_A_R8Works() {
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0xA,Opcode.LD_B_N8.encoding,0x7,Opcode.ADD_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x11,get(gb,Reg.A));
        assertEquals(Flag.H.mask(),get(gb,Reg.F));
    }

    @Test
    public void opcodeADD_A_N8Works() {
        int[] instruction = new int[]{Opcode.LD_A_N8.encoding,0xFF,Opcode.ADD_A_N8.encoding,0x1};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00,get(gb,Reg.A));
        assertEquals(0b10110000,get(gb,Reg.F));
    }

    @Test
    public void opcodeADC_A_N8Works() {
        int[] instruction = new int[]{Opcode.SCF.encoding,Opcode.LD_A_N8.encoding,0x0F,Opcode.ADC_A_N8.encoding,0x0};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xF + 1,get(gb,Reg.A));
        assertEquals(0b00100000,get(gb,Reg.F));
    }

    @Test
    public void opcodeADD_A_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0xFF,Opcode.LD_HL_N16.encoding,0xFF,0x10
                , Opcode.ADD_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10FF,0x42);
        gb.execute(instruction);

        assertEquals(clip(8,0x42 + 0xFF), get(gb, Reg.A));
        assertEquals(0b00110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeINC_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0xFF, Opcode.INC_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeINC_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding,0xFF,0x10, Opcode.INC_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x10FF,0xFF);
        gb.execute(instruction);

        assertEquals(0x00, gb.bus.read(0x10FF));
        assertEquals(0b10100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeINC_R16SPWorks() {
        int[] instruction = new int[] { Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.INC_SP.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.SP));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeADD_HL_R16SPWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0xFF,0xFF,Opcode.LD_SP_N16.encoding
                ,0x01,0x00, Opcode.ADD_HL_SP.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb,Reg16.HL));
        assertEquals(0b00110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeLD_HLSP_S8Works() {
        int[] instruction = new int[] { Opcode.LD_SP_N16.encoding, 0xFF, 0xFF, Opcode.LD_HL_SP_N8.encoding , clip(8,-1)};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x0FFFE, get(gb, Reg16.HL));
        assertEquals(0b00110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSUB_A_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0xFF , Opcode.LD_B_N8.encoding, 0xFF , Opcode.SUB_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x0, get(gb, Reg.A));
        assertEquals(0b11000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSUB_A_N8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0x0 , Opcode.SUB_A_N8.encoding, 0x1 };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xFF, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSBC_A_N8Works() {
        int[] instruction = new int[] { Opcode.SCF.encoding, Opcode.LD_A_N8.encoding, 0x0 , Opcode.SBC_A_N8.encoding, 0x0 };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xFF, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSUB_A_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding,0x00,0x10, Opcode.LD_A_N8.encoding, 0x0 , Opcode.SUB_A_HLR.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x1000,0x1);
        gb.execute(instruction);

        assertEquals(0xFF, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeDEC_R8Works() {
        int[] instruction = new int[] { Opcode.LD_E_N8.encoding, 0x0 , Opcode.DEC_E.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xFF, get(gb, Reg.E));
        assertEquals(0b01100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeDEC_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10 , Opcode.DEC_HLR.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x1000,0x1);
        gb.execute(instruction);

        assertEquals(0x00, gb.bus.read(0x1000));
        assertEquals(0b11000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeCP_A_N8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0x0 , Opcode.CP_A_N8.encoding , 0x1};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeCP_A_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0x0, Opcode.LD_B_N8.encoding, 0x1 , Opcode.CP_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeCP_A_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10
                , Opcode.LD_HLR_N8.encoding, 0x1, Opcode.CP_A_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b01110000, get(gb, Reg.F));
    }

    @Test
    public void opcodeDEC_SPWorks() {
        int[] instruction = new int[] { Opcode.LD_SP_N16.encoding, 0x00, 0x00, Opcode.DEC_SP.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xFFFF, get(gb, Reg.SP));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeAND_A_N8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b01 , Opcode.AND_A_N8.encoding, 0b10 };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeAND_A_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding,0x00,0x10, Opcode.LD_A_N8.encoding, 0b01 , Opcode.AND_A_HLR.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x1000,0b10);
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeAND_A_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b110 , Opcode.LD_B_N8.encoding, 0b101 , Opcode.AND_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b100, get(gb, Reg.A));
        assertEquals(0b00100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeOR_A_N8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b01 , Opcode.OR_A_N8.encoding, 0b10 };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b11, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeOR_A_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding,0x00,0x10, Opcode.LD_A_N8.encoding, 0b00 , Opcode.OR_A_HLR.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x1000,0b00);
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeOR_A_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b110 , Opcode.LD_B_N8.encoding, 0b101 , Opcode.OR_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b111, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeXOR_A_N8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b01 , Opcode.XOR_A_N8.encoding, 0b10 };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b11, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeXOR_A_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding,0x00,0x10, Opcode.LD_A_N8.encoding, 0b11 , Opcode.XOR_A_HLR.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.bus.write(0x1000,0b11);
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeXOR_A_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b1100 , Opcode.LD_B_N8.encoding, 0b1010 , Opcode.XOR_A_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b0110, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeCPLWorks() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b1001,Opcode.CPL.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b11110110, get(gb, Reg.A));
        assertEquals(0b01100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTCALWorks() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b10001111,Opcode.RLCA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011111, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTALWorks() {
        int[] instruction = new int[] {Opcode.SCF.encoding, Opcode.LD_A_N8.encoding, 0b10001111,Opcode.RLA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011111, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeR0TCARWorks() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding, 0b00001111,Opcode.RRCA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTARWorks() {
        int[] instruction = new int[] {Opcode.SCF.encoding, Opcode.LD_A_N8.encoding, 0b00001111,Opcode.RRA.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));

        instruction = new int[] { Opcode.LD_A_N8.encoding, 0b00001110,Opcode.RRA.encoding};

        gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000111, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTCAL_R8Works() {
        int[] instruction = new int[] { Opcode.LD_B_N8.encoding, 0b10001111,OPCODE_PREFIX, Opcode.RLC_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011111, get(gb, Reg.B));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTAL_R8Works() {
        int[] instruction = new int[] {Opcode.LD_B_N8.encoding, 0b10001111,OPCODE_PREFIX, Opcode.RL_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011110, get(gb, Reg.B));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeR0TCAR_R8Works() {
        int[] instruction = new int[] { Opcode.LD_B_N8.encoding, 0b00001111,OPCODE_PREFIX, Opcode.RRC_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, get(gb, Reg.B));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTAR_R8Works() {
        int[] instruction = new int[] {Opcode.SCF.encoding, Opcode.LD_B_N8.encoding, 0b00001111,OPCODE_PREFIX, Opcode.RR_B.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, get(gb, Reg.B));
        assertEquals(0b00010000, get(gb, Reg.F));

        instruction = new int[] { Opcode.LD_B_N8.encoding, 0b00001110,OPCODE_PREFIX, Opcode.RR_B.encoding};

        gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000111, get(gb, Reg.B));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTCAL_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.RLC_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011111, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTAL_HLRWorks() {
        int[] instruction = new int[] {Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.RL_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011110, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeR0TCAR_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b00001111,OPCODE_PREFIX, Opcode.RRC_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeROTAR_HLRWorks() {
        int[] instruction = new int[] {Opcode.SCF.encoding, Opcode.LD_HL_N16.encoding, 0x00, 0x10
                ,  Opcode.LD_HLR_N8.encoding, 0b00001111,OPCODE_PREFIX, Opcode.RR_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10000111, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));

        instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b00001110,OPCODE_PREFIX, Opcode.RR_HLR.encoding};

        gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000111, gb.bus.read(0x1000));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSWAP_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0xFA,OPCODE_PREFIX, Opcode.SWAP_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0xAF, gb.bus.read(0x1000));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSWAP_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0x00,OPCODE_PREFIX, Opcode.SWAP_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0x00, get(gb, Reg.A));
        assertEquals(0b10000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSLA_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.SLA_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011110, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSRA_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.SRA_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b11000111, get(gb, Reg.A));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSRL_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b10001110,OPCODE_PREFIX, Opcode.SRL_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b01000111, get(gb, Reg.A));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSLA_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.SLA_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00011110, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSRA_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10001110,OPCODE_PREFIX, Opcode.SRA_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b11000111, gb.bus.read(0x1000));
        assertEquals(0b00000000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSRL_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10001111,OPCODE_PREFIX, Opcode.SRL_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b01000111, gb.bus.read(0x1000));
        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeBIT_U3_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b00001000,OPCODE_PREFIX, Opcode.BIT_3_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeBIT_U3_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10000000,OPCODE_PREFIX, Opcode.BIT_3_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10100000, get(gb, Reg.F));
    }

    @Test
    public void opcodeSET_U3_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b00001000,OPCODE_PREFIX, Opcode.SET_2_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00001100, get(gb, Reg.A));
        assertEquals(0, get(gb,Reg.F));
    }

    @Test
    public void opcodeSET_U3_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10000000,OPCODE_PREFIX, Opcode.SET_3_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b10001000, gb.bus.read(0x1000));
        assertEquals(0, get(gb,Reg.F));
    }

    @Test
    public void opcodeRES_U3_R8Works() {
        int[] instruction = new int[] { Opcode.LD_A_N8.encoding
                , 0b00001000,OPCODE_PREFIX, Opcode.RES_3_A.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000000, get(gb, Reg.A));
        assertEquals(0, get(gb,Reg.F));
    }

    @Test
    public void opcodeRES_U3_HLRWorks() {
        int[] instruction = new int[] { Opcode.LD_HL_N16.encoding, 0x00, 0x10,  Opcode.LD_HLR_N8.encoding
                , 0b10000000,OPCODE_PREFIX, Opcode.RES_7_HLR.encoding};

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b0, gb.bus.read(0x1000));
        assertEquals(0, get(gb,Reg.F));
    }

    @Test
    public void opcodeDAAWorks() {
        int[] instruction = new int[] {Opcode.LD_A_N8.encoding, 0b10011001,Opcode.ADD_A_N8.encoding, 0b0001, Opcode.DAA.encoding};


        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000000, get(gb, Reg.A));
        assertEquals(0b10010000, get(gb, Reg.F));
    }


    @Test
    public void opcodeSCFWorks() {
        int[] instruction = new int[] { Opcode.SCF.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00010000, get(gb, Reg.F));
    }

    @Test
    public void opcodeCCFWorks() {
        int[] instruction = new int[] {Opcode.CCF.encoding };

        CpuWithRam gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00010000, get(gb, Reg.F));


        instruction = new int[] {Opcode.SCF.encoding,Opcode.CCF.encoding };

        gb = new CpuWithRam();
        gb.execute(instruction);

        assertEquals(0b00000000, get(gb, Reg.F));
    }

}