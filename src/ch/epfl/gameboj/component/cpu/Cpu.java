package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.*;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Ram;

/**
 * Cpu
 *
 * A class representing a processor component that can run a program and
 * execute various instructions, written in memory as opcodes
 *
 * @author Ulysse Ramage
 */
public final class Cpu implements Component, Clocked {

    private Bus bus;

    /** Indexed arrays containing all possible opcodes (DIRECT, PREFIXED) */
    private static final Opcode[] DIRECT_OPCODE_TABLE =
            buildOpcodeTable(Opcode.Kind.DIRECT);
    private static final Opcode[] PREFIXED_OPCODE_TABLE =
            buildOpcodeTable(Opcode.Kind.PREFIXED);
    private static final int OPCODE_PREFIX = 0xCB;

    /** Registers */
    private enum Reg implements Register {
        A, F, B, C, D, E, H, L
    }
    private enum Reg16 implements Register {
        AF, BC, DE, HL
    }
    private final Reg[] regValues = Reg.values();
    private final RegisterFile<Reg> regFile = new RegisterFile<>(regValues);
    private int PC, SP;

    private long nextNonIdleCycle;
    private boolean alteredPC;
    private boolean conditionFailed;

    /** Interrupts */
    public enum Interrupt implements Bit {
        VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
    }
    private int IE, IF;
    boolean IME = false;

    private enum FlagSrc {
        V0, V1, ALU, CPU
    }

    private final Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

    private boolean isWithinHighRamBounds(int address) {
        return (AddressMap.HIGH_RAM_START <= address
                && address < AddressMap.HIGH_RAM_END);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (address == AddressMap.REG_IE) return IE;
        if (address == AddressMap.REG_IF) return IF;
        if (isWithinHighRamBounds(address)) {
            return highRam.read(address - AddressMap.HIGH_RAM_START);
        } else {
            return Component.NO_DATA;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if (address == AddressMap.REG_IE) IE = data;
        if (address == AddressMap.REG_IF) IF = data;
        if (isWithinHighRamBounds(address)) {
            highRam.write(address - AddressMap.HIGH_RAM_START, data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachTo(Bus bus) {
        Component.super.attachTo(bus);
        this.bus = bus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cycle(long cycle) {
        if ((nextNonIdleCycle == Long.MAX_VALUE) && (hasCurrentInterrupt())) {
            nextNonIdleCycle = cycle;
        }
        if (cycle != nextNonIdleCycle) return;
        reallyCycle(cycle);
    }

    /**
     * Raises an interrupt (sets the corresponding bit to 1 in register IF)
     * @param i the interrupt to raise
     */
    public void requestInterrupt(Interrupt i) {
        IF = Bits.set(IF, i.index(), true);
    }

    private void reallyCycle(long cycle) {
        Interrupt interrupt = getCurrentInterrupt();
        if (IME && interrupt != null) {
            handleInterrupt(interrupt);
        } else {
            Opcode opcode = isOpcodePrefixed() ?
                    PREFIXED_OPCODE_TABLE[readOpcodeAfterPrefix()] :
                    DIRECT_OPCODE_TABLE[readOpcode()];

            alteredPC = false;
            conditionFailed = false;
            dispatch(opcode.encoding);

            if (!alteredPC) setPC(PC + opcode.totalBytes);
            int additionalCycles = conditionFailed ? 0 : opcode.additionalCycles;
            nextNonIdleCycle += opcode.cycles + additionalCycles;
        }
    }

    /** Executes an opcode instruction, modifying the Cpu registers */
    private void dispatch(int opcodeEncoding) {
        Opcode opcode = isOpcodePrefixed() ?
                PREFIXED_OPCODE_TABLE[opcodeEncoding] :
                DIRECT_OPCODE_TABLE[opcodeEncoding];

        switch (opcode.family) {

        // Load
        case NOP: {
        } break;
        case LD_R8_HLR: {
            setReg(extractReg(opcode, 3), read8AtHl());
        } break;
        case LD_A_HLRU: {
            int address = reg16(Reg16.HL) + extractHlIncrement(opcode);
            setReg(Reg.A, read8AtHl());
            setReg16(Reg16.HL, clip16(address));
        } break;
        case LD_A_N8R: {
            int address = clip16(AddressMap.REGS_START + read8AfterOpcode());
            setReg(Reg.A, read8(address));
        } break;
        case LD_A_CR: {
            int address = clip16(AddressMap.REGS_START + reg(Reg.C));
            setReg(Reg.A, read8(address));
        } break;
        case LD_A_N16R: {
            setReg(Reg.A, read8(read16AfterOpcode()));
        } break;
        case LD_A_BCR: {
            setReg(Reg.A, read8(reg16(Reg16.BC)));
        } break;
        case LD_A_DER: {
            setReg(Reg.A, read8(reg16(Reg16.DE)));
        } break;
        case LD_R8_N8: {
            setReg(extractReg(opcode, 3), read8AfterOpcode());
        } break;
        case LD_R16SP_N16: {
            setReg16SP(extractReg16(opcode), read16AfterOpcode());
        } break;
        case POP_R16: {
            setReg16(extractReg16(opcode), pop16());
        } break;
        case LD_HLR_R8: {
            write8AtHl(reg(extractReg(opcode, 0)));
        } break;
        case LD_HLRU_A: {
            write8AtHl(reg(Reg.A));
            int value = clip16(reg16(Reg16.HL) + extractHlIncrement(opcode));
            setReg16(Reg16.HL, value);
        } break;
        case LD_N8R_A: {
            int address = clip16(AddressMap.REGS_START + read8AfterOpcode());
            write8(address, reg(Reg.A));
        } break;
        case LD_CR_A: {
            int address = clip16(AddressMap.REGS_START + reg(Reg.C));
            write8(address, reg(Reg.A));
        } break;
        case LD_N16R_A: {
            write8(read16AfterOpcode(), reg(Reg.A));
        } break;
        case LD_BCR_A: {
            write8(reg16(Reg16.BC), reg(Reg.A));
        } break;
        case LD_DER_A: {
            write8(reg16(Reg16.DE), reg(Reg.A));
        } break;
        case LD_HLR_N8: {
            write8(reg16(Reg16.HL), read8AfterOpcode());
        } break;
        case LD_N16R_SP: {
            write16(read16AfterOpcode(), SP);
        } break;
        case LD_R8_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            setReg(extractReg(opcode, 3), reg(opcodeReg));
        } break;
        case LD_SP_HL: {
            SP = reg16(Reg16.HL);
        } break;
        case PUSH_R16: {
            push16(reg16(extractReg16(opcode)));
        } break;

        // Add
        case ADD_A_N8: {
            boolean carry = extractCarry(opcode);
            int result = Alu.add(reg(Reg.A), read8AfterOpcode(), carry && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case ADD_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            boolean carry = extractCarry(opcode);
            int result = Alu.add(reg(Reg.A), reg(opcodeReg), carry && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case ADD_A_HLR: {
            boolean carry = extractCarry(opcode);
            int result = Alu.add(reg(Reg.A), read8AtHl(), carry && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case INC_R8: {
            Reg opcodeReg = extractReg(opcode, 3);
            int result = Alu.add(reg(opcodeReg), 1);
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case INC_HLR: {
            int result = Alu.add(read8AtHl(), 1);
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case INC_R16SP: {
            Reg16 opcodeReg16 = extractReg16(opcode);
            int result = Alu.add16H(reg16SP(opcodeReg16), 1);
            setReg16SP(opcodeReg16, Alu.unpackValue(result));
        } break;
        case ADD_HL_R16SP: {
            Reg16 opcodeReg16 = extractReg16(opcode);
            int result = Alu.add16H(reg16(Reg16.HL), reg16SP(opcodeReg16));
            setReg16(Reg16.HL, Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case LD_HLSP_S8: {
            int value = Bits.clip(16, Bits.signExtend8(read8AfterOpcode()));
            boolean editHL = Bits.test(opcode.encoding, 4);
            int result = Alu.add16L(SP, value);
            if (editHL) {
                setReg16(Reg16.HL, Alu.unpackValue(result));
            } else {
                SP = Alu.unpackValue(result);
            }
            combineAluFlags(result, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
        } break;

        // Subtract
        case SUB_A_N8: {
            boolean borrow = extractCarry(opcode);
            int result = Alu.sub(reg(Reg.A), read8AfterOpcode(), borrow && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case SUB_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            boolean borrow = extractCarry(opcode);
            int result = Alu.sub(reg(Reg.A), reg(opcodeReg), borrow && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case SUB_A_HLR: {
            boolean borrow = extractCarry(opcode);
            int result = Alu.sub(reg(Reg.A), read8AtHl(), borrow && getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case DEC_R8: {
            Reg opcodeReg = extractReg(opcode, 3);
            int result = Alu.sub(reg(opcodeReg), 1);
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case DEC_HLR: {
            int result = Alu.sub(read8AtHl(), 1);
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
        } break;
        case CP_A_N8: {
            int result = Alu.sub(reg(Reg.A), read8AfterOpcode());
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case CP_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.sub(reg(Reg.A), reg(opcodeReg));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case CP_A_HLR: {
            int result = Alu.sub(reg(Reg.A), read8AtHl());
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.ALU);
        } break;
        case DEC_R16SP: {
            Reg16 opcodeReg16 = extractReg16(opcode);
            setReg16SP(opcodeReg16, Bits.clip(16, reg16SP(opcodeReg16) - 1));
        } break;

        // And, or, xor, complement
        case AND_A_N8: {
            int result = Alu.and(reg(Reg.A), read8AfterOpcode());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);
        } break;
        case AND_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.and(reg(Reg.A), reg(opcodeReg));
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);
        } break;
        case AND_A_HLR: {
            int result = Alu.and(reg(Reg.A), read8AtHl());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.V0);
        } break;
        case OR_A_N8: {
            int result = Alu.or(reg(Reg.A), read8AfterOpcode());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case OR_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.or(reg(Reg.A), reg(opcodeReg));
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case OR_A_HLR: {
            int result = Alu.or(reg(Reg.A), read8AtHl());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case XOR_A_N8: {
            int result = Alu.xor(reg(Reg.A), read8AfterOpcode());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case XOR_A_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.xor(reg(Reg.A), reg(opcodeReg));
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case XOR_A_HLR: {
            int result = Alu.xor(reg(Reg.A), read8AtHl());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case CPL: {
            setReg(Reg.A, Bits.complement8(reg(Reg.A)));
            combineAluFlags(0, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);
        } break;

        // Rotate, shift
        case ROTCA: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            int result = Alu.rotate(rotDir, reg(Reg.A));
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case ROTA: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            int result = Alu.rotate(rotDir, reg(Reg.A), getC());
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case ROTC_R8: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.rotate(rotDir, reg(opcodeReg));
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case ROT_R8: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.rotate(rotDir, reg(opcodeReg), getC());
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case ROTC_HLR: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            int result = Alu.rotate(rotDir, read8AtHl());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case ROT_HLR: {
            Alu.RotDir rotDir = extractRotateDirection(opcode);
            int result = Alu.rotate(rotDir, read8AtHl(), getC());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SWAP_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.swap(reg(opcodeReg));
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case SWAP_HLR: {
            int result = Alu.swap(read8AtHl());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0);
        } break;
        case SLA_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.shiftLeft(reg(opcodeReg));
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SRA_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.shiftRightA(reg(opcodeReg));
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SRL_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.shiftRightL(reg(opcodeReg));
            setRegFromAlu(opcodeReg, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SLA_HLR: {
            int result = Alu.shiftLeft(read8AtHl());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SRA_HLR: {
            int result = Alu.shiftRightA(read8AtHl());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SRL_HLR: {
            int result = Alu.shiftRightL(read8AtHl());
            write8AtHl(Alu.unpackValue(result));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
        } break;

        // Bit test and set
        case BIT_U3_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Alu.testBit(reg(opcodeReg), extractBitIndex(opcode));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
        } break;
        case BIT_U3_HLR: {
            int result = Alu.testBit(read8AtHl(), extractBitIndex(opcode));
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
        } break;
        case CHG_U3_R8: {
            Reg opcodeReg = extractReg(opcode, 0);
            int result = Bits.set(reg(opcodeReg), extractBitIndex(opcode), extractBitValue(opcode));
            setReg(opcodeReg, result);
        } break;
        case CHG_U3_HLR: {
            int result = Bits.set(read8AtHl(), extractBitIndex(opcode), extractBitValue(opcode));
            write8AtHl(result);
        } break;

        // Misc. ALU
        case DAA: {
            boolean n = getFlag(Alu.Flag.N), h = getFlag(Alu.Flag.H), c = getC();
            int result = Alu.bcdAdjust(reg(Reg.A), n, h, c);
            setRegFromAlu(Reg.A, result);
            combineAluFlags(result, FlagSrc.ALU, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU);
        } break;
        case SCCF: {
            boolean complement = extractCarry(opcode);
            boolean carry = !complement || !getC();
            combineAluFlags(0, FlagSrc.CPU, FlagSrc.V0, FlagSrc.V0, carry ? FlagSrc.V1 : FlagSrc.V0);
        } break;

        // Jumps
        case JP_HL: {
            setPC(reg16(Reg16.HL));
        } break;
        case JP_N16: {
            setPC(read16AfterOpcode());
        } break;
        case JP_CC_N16: {
            if (evaluateCondition(opcode)) {
                setPC(read16AfterOpcode());
            } else {
                conditionFailed = true;
            }
        } break;
        case JR_E8: {
            int e8 = Bits.signExtend8(read8AfterOpcode());
            setPC(PC + opcode.totalBytes + e8);
        } break;
        case JR_CC_E8: {
            if (evaluateCondition(opcode)) {
                int e8 = Bits.signExtend8(read8AfterOpcode());
                setPC(PC + opcode.totalBytes + e8);
            } else {
                conditionFailed = true;
            }
        } break;

        // Calls and returns
        case CALL_N16: {
            push16(clip16(PC + opcode.totalBytes));
            setPC(read16AfterOpcode());
        } break;
        case CALL_CC_N16: {
            if (evaluateCondition(opcode)) {
                push16(clip16(PC + opcode.totalBytes));
                setPC(read16AfterOpcode());
            } else {
                conditionFailed = true;
            }
        } break;
        case RST_U3: {
            push16(clip16(PC + opcode.totalBytes));
            int u3 = Bits.extract(opcode.encoding, 3, 3);
            setPC(AddressMap.RESETS[u3]);
        } break;
        case RET: {
            setPC(pop16());
        } break;
        case RET_CC: {
            if (evaluateCondition(opcode)) {
                setPC(pop16());
            } else {
                conditionFailed = true;
            }
        } break;

        // Interrupts
        case EDI: {
            IME = Bits.test(opcode.encoding, 3);
        } break;
        case RETI: {
            IME = true;
            setPC(pop16());
        } break;

        // Misc control
        case HALT: {
            nextNonIdleCycle = Long.MAX_VALUE;
        } break;
        case STOP:
            throw new Error("STOP is not implemented");

        }
    }

    /** Creates an array of opcodes indexed by their encoding, given a kind */
    private static Opcode[] buildOpcodeTable(Opcode.Kind kind) {
        Opcode[] opcodeTable = new Opcode[0x100];
        for (Opcode o: Opcode.values()) {
            if (o.kind.equals(kind)) {
                opcodeTable[o.encoding] = o;
            }
        }
        return opcodeTable;
    }

    /**
     * Creates an array containing the Cpu registers, for testing purposes
     * @return an array of size 10 containing the values stored in registers
     * PC, SP, A, F, B, C, D, E, H and L respectively
     */
    public int[] _testGetPcSpAFBCDEHL() {
        return new int[] {
                PC,
                SP,
                reg(Reg.A),
                reg(Reg.F),
                reg(Reg.B),
                reg(Reg.C),
                reg(Reg.D),
                reg(Reg.E),
                reg(Reg.H),
                reg(Reg.L)
        };
    }

    private int clip16(int v) {
        return Bits.clip(16, v);
    }

    private int read8(int address) {
        return bus.read(address);
    }

    private int read8AtHl() {
        return read8(reg16(Reg16.HL));
    }

    private void write8(int address, int v) {
        bus.write(address, v);
    }

    private void write8AtHl(int v) {
        write8(reg16(Reg16.HL), v);
    }

    private int read16(int address) {
        return Bits.make16(
                read8(clip16(address + 1)),
                read8(address)
        );
    }

    private void write16(int address, int v) {
        write8(address, Bits.extract(v, 0, 8));
        write8(clip16(address + 1), Bits.extract(v, 8, 8));
    }

    private int readOpcode() {
        return read8(PC);
    }

    private int readOpcodeAfterPrefix() {
        return read8(clip16(PC + 1));
    }

    private boolean isOpcodePrefixed() {
        return readOpcode() == OPCODE_PREFIX;
    }

    private int addressAfterOpcode() {
        return clip16(PC + 1 + (isOpcodePrefixed() ? 1 : 0));
    }

    private int read8AfterOpcode() {
        return read8(addressAfterOpcode());
    }

    private int read16AfterOpcode() {
        return read16(addressAfterOpcode());
    }

    private void push16(int v) {
        SP = clip16(SP - 2);
        write16(SP, v);
    }

    private int pop16() {
        int _SP = SP;
        SP = clip16(SP + 2);
        return read16(_SP);
    }

    private int reg(Reg r) {
        return regFile.get(r);
    }

    /** Splits a 16-bit register pair into two registers */
    private Reg[] regPair(Reg16 r) {
        int index = r.index();
        return new Reg[] { regValues[index * 2], regValues[index * 2 + 1] };
    }

    private int reg16(Reg16 r) {
        Reg[] pair = regPair(r);
        return Bits.make16(
                reg(pair[0]),
                reg(pair[1])
        );
    }

    private int reg16SP(Reg16 r) {
        if (r.equals(Reg16.AF)) {
            return SP;
        } else {
            return reg16(r);
        }
    }

    private void setReg(Reg r, int newV) {
        Preconditions.checkBits8(newV);
        regFile.set(r, newV);
    }

    private void setReg16(Reg16 r, int newV) {
        Preconditions.checkBits16(newV);
        Reg[] pair = regPair(r);

        if (r.equals(Reg16.AF)) {
            newV = newV & -1 << 4;
        }
        setReg(pair[0], Bits.extract(newV, 8, 8));
        setReg(pair[1], Bits.extract(newV, 0, 8));
    }

    private void setReg16SP(Reg16 r, int newV) {
        if (r.equals(Reg16.AF)) {
            SP = newV;
        } else {
            setReg16(r, newV);
        }
    }

    private Reg extractReg(Opcode opcode, int startBit) {
        int identity = Bits.extract(opcode.encoding, startBit, 3);
        switch (identity) {
            case 0b000: return Reg.B;
            case 0b001: return Reg.C;
            case 0b010: return Reg.D;
            case 0b011: return Reg.E;
            case 0b100: return Reg.H;
            case 0b101: return Reg.L;
            case 0b111: return Reg.A;
            default: return null;
        }
    }

    private Reg16 extractReg16(Opcode opcode) {
        int identity = Bits.extract(opcode.encoding, 4, 2);
        switch (identity) {
            case 0b00: return Reg16.BC;
            case 0b01: return Reg16.DE;
            case 0b10: return Reg16.HL;
            case 0b11: return Reg16.AF;
            default: return null;
        }
    }

    private int extractHlIncrement(Opcode opcode) {
        return Bits.test(opcode.encoding, 4) ? -1 : 1;
    }

    private void setRegFromAlu(Reg r, int valueFlags) {
        setReg(r, Alu.unpackValue(valueFlags));
    }

    private void setFlags(int valueFlags) {
        setReg(Reg.F, Alu.unpackFlags(valueFlags));
    }

    private boolean combineFlag(Alu.Flag flag, FlagSrc src, int aluFlags, int cpuFlags) {
        switch (src) {
            case V0: return false;
            case V1: return true;
            case ALU: return Bits.test(aluFlags, flag.index());
            case CPU: return Bits.test(cpuFlags, flag.index());
            default: throw new IllegalArgumentException();
        }
    }

    /** Combines the flags from Alu and Cpu given sources, and stores into F */
    private void combineAluFlags(int valueFlags, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
        int aluFlags = Alu.unpackFlags(valueFlags);
        int cpuFlags = reg(Reg.F);
        setFlags(Alu.maskZNHC(
                combineFlag(Alu.Flag.Z, z, aluFlags, cpuFlags),
                combineFlag(Alu.Flag.N, n, aluFlags, cpuFlags),
                combineFlag(Alu.Flag.H, h, aluFlags, cpuFlags),
                combineFlag(Alu.Flag.C, c, aluFlags, cpuFlags)
        ));
    }

    private Alu.RotDir extractRotateDirection(Opcode opcode) {
        return Bits.test(opcode.encoding, 3) ? Alu.RotDir.RIGHT : Alu.RotDir.LEFT;
    }

    private int extractBitIndex(Opcode opcode) {
        return Bits.extract(opcode.encoding, 3, 3);
    }

    private boolean extractBitValue(Opcode opcode) {
        return Bits.test(opcode.encoding, 6);
    }

    private boolean getFlag(Alu.Flag flag) {
        return Bits.test(reg(Reg.F), flag);
    }

    private boolean getC() {
        return getFlag(Alu.Flag.C);
    }

    private boolean extractCarry(Opcode opcode) {
        return Bits.test(opcode.encoding, 3);
    }

    private void setPC(int address) {
        PC = clip16(address);
        alteredPC = true;
    }

    private boolean evaluateCondition(Opcode opcode) {
        int condition = Bits.extract(opcode.encoding, 3, 2);
        switch (condition) {
            case 0b00: {
                return !getFlag(Alu.Flag.Z);
            }
            case 0b01: {
                return getFlag(Alu.Flag.Z);
            }
            case 0b10: {
                return !getC();
            }
            case 0b11: {
                return getC();
            }
            default: return false;
        }
    }

    private Interrupt getCurrentInterrupt() {
        for (Interrupt interrupt : Interrupt.values()) {
            int index = interrupt.index();
            if (Bits.test(IF, index) && Bits.test(IE, index)) {
                return interrupt;
            }
        }
        return null;
    }

    private boolean hasCurrentInterrupt() {
        return getCurrentInterrupt() != null;
    }

    private void handleInterrupt(Interrupt interrupt) {
        IME = false;
        IF = Bits.set(IF, interrupt.index(), false);
        push16(PC);
        setPC(AddressMap.INTERRUPTS[interrupt.index()]);
        nextNonIdleCycle += 5;
    }

}
