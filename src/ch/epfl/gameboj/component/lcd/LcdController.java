package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.Ram;

import java.util.Collections;

/**
 * LcdController
 *
 * A class that represents a Lcd Controller
 *
 * @author Ulysse Ramage (282300)
 */
public final class LcdController implements Component, Clocked {

    public static final int LCD_WIDTH = 160;
    public static final int LCD_HEIGHT = 144;
    private static final LcdImageLine EMPTY_LINE =
            new LcdImageLine.Builder(LCD_WIDTH).build();
    private static final LcdImage EMPTY_IMAGE = new LcdImage(
            LCD_WIDTH,
            LCD_HEIGHT,
            Collections.nCopies(LCD_HEIGHT, EMPTY_LINE)
    );
    private static final int
            TILE_SIZE = 8,
            IMAGE_WIDTH = 32,
            IMAGE_SIZE = IMAGE_WIDTH * TILE_SIZE;

    private final Cpu cpu;

    private final Ram videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);

    private LcdImage.Builder nextImageBuilder;
    private LcdImage currentImage;

    private long nextNonIdleCycle = Long.MAX_VALUE;

    /** Registers */
    private int LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX;
    private enum ConfigBits implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }
    private enum StatBits implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC
    }
    private int winY;

    private enum ImageSource {
        BACKGROUND, WINDOW
    }


    /**
     * Creates a new LcdController with the given cpu
     * @param cpu the GameBoy cpu
     */
    public LcdController(Cpu cpu) {
        this.cpu = cpu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (isWithinRegsBounds(address)) {
            switch (address - AddressMap.REGS_LCDC_START) {
                case 0: return LCDC;
                case 1: return STAT;
                case 2: return SCY;
                case 3: return SCX;
                case 4: return LY;
                case 5: return LYC;
                case 6: return DMA;
                case 7: return BGP;
                case 8: return OBP0;
                case 9: return OBP1;
                case 10: return WY;
                case 11: return WX;
            }
        }
        if (isWithinVideoRamBounds(address)) {
            return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        }
        return Component.NO_DATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if (isWithinRegsBounds(address)) {
            switch (address - AddressMap.REGS_LCDC_START) {
                case 0: {
                    boolean deactivated = Bits.test(LCDC, ConfigBits.LCD_STATUS)
                            && !Bits.test(data, ConfigBits.LCD_STATUS);
                    LCDC = data;
                    if (deactivated) {
                        STAT = STAT & (-1 << 2);
                        LY = 0;
                        nextNonIdleCycle = Long.MAX_VALUE;
                    }
                } break;
                case 1: {
                    int mask = -1 << 3;
                    STAT = (data & mask) | (STAT & ~mask);
                } break;
                case 2: SCY = data; break;
                case 3: SCX = data; break;
                case 4: break;
                case 5: {
                    LYC = data;
                    check_LYC_EQ_LY();
                } break;
                case 6: DMA = data; break;
                case 7: BGP = data; break;
                case 8: OBP0 = data; break;
                case 9: OBP1 = data; break;
                case 10: WY = data; break;
                case 11: WX = data; break;
            }
        }
        if (isWithinVideoRamBounds(address)) {
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cycle(long cycle) {
        if (nextNonIdleCycle == Long.MAX_VALUE
                && Bits.test(LCDC, ConfigBits.LCD_STATUS)) {
            nextNonIdleCycle = cycle;
            STAT = (STAT & (-1 << 2)) | 1;
            startDrawingLine(0);
        }
        if (cycle != nextNonIdleCycle) return;
        reallyCycle(cycle);
    }

    /**
     * @return the image currently displayed on screen (empty if it hasn't
     * been drawn yet)
     */
    public LcdImage currentImage() {
        return (currentImage != null) ? currentImage : EMPTY_IMAGE;
    }

    private boolean isWithinRegsBounds(int address) {
        return (AddressMap.REGS_LCDC_START <= address
                && address < AddressMap.REGS_LCDC_END);
    }

    private boolean isWithinVideoRamBounds(int address) {
        return (AddressMap.VIDEO_RAM_START <= address
                && address < AddressMap.VIDEO_RAM_END);
    }

    /** Simulates a cycle */
    private void reallyCycle(long cycle) {
        switch (getMode()) {
            case 0: {
                if (LY < LCD_HEIGHT - 1) {
                    startDrawingLine(LY + 1);
                } else if (LY == LCD_HEIGHT - 1) {
                    setMode(1);
                    LY++;
                    nextNonIdleCycle += 114;
                }
            } break;
            case 1: {
                if (LY == LCD_HEIGHT + 10 - 1) {
                    startDrawingLine(0);
                } else {
                    LY++;
                    check_LYC_EQ_LY();
                    nextNonIdleCycle += 114;
                }
            } break;
            case 2: {
                nextNonIdleCycle += 43;
                setMode(3);
            } break;
            case 3: {
                nextNonIdleCycle += 51;
                setMode(0);
            } break;
        }
    }

    private void startDrawingLine(int line) {
        LY = line;
        check_LYC_EQ_LY();
        setMode(2);
        nextNonIdleCycle += 20;
    }

    private int readTileLine(int tileImageAddress, int lineIndex) {
        int address = tileImageAddress + lineIndex * 2;
        int msb = Bits.reverse8(read(address + 1));
        int lsb = Bits.reverse8(read(address));
        return msb << 8 | lsb;
    }

    private int getTileImageAddress(ImageSource source, int tileIndex) {
        boolean rangeBit;
        switch (source) {
            case BACKGROUND: rangeBit = Bits.test(LCDC, ConfigBits.BG_AREA); break;
            case WINDOW: rangeBit = Bits.test(LCDC, ConfigBits.WIN_AREA); break;
            default: throw new IllegalArgumentException();
        }
        int tileAddress = read(
                AddressMap.BG_DISPLAY_DATA[rangeBit ? 1 : 0] + tileIndex
        );
        boolean tileSourceBit = Bits.test(LCDC, ConfigBits.TILE_SOURCE);
        if (!tileSourceBit) {
            tileAddress = Bits.clip(8, tileAddress + 0x80);
        }
        tileAddress *= TILE_SIZE * 2;
        tileAddress += AddressMap.TILE_SOURCE[tileSourceBit ? 1 : 0];
        return tileAddress;
    }

    private LcdImageLine readImageLine(ImageSource source, int lineIndex) {
        LcdImageLine.Builder imageLineBuilder = new LcdImageLine.Builder(IMAGE_SIZE);
        for (int tileIndex = 0; tileIndex < IMAGE_WIDTH; ++tileIndex) {
            int tileLine = readTileLine(
                    getTileImageAddress(
                            source,
                            lineIndex / TILE_SIZE * IMAGE_WIDTH + tileIndex
                    ),
                    lineIndex % TILE_SIZE
            );
            imageLineBuilder.setBytes(
                    tileIndex,
                    Bits.extract(tileLine, 8, 8),
                    Bits.extract(tileLine, 0, 8)
            );
        }
        return imageLineBuilder.build();
    }

    private boolean isBackgroundActive() {
        return Bits.test(LCDC, ConfigBits.BG);
    }

    private boolean isWindowActive() {
        return Bits.test(LCDC, ConfigBits.WIN)
                && (0 <= WXP() && WXP() < 160);
    }

    private int WXP() {
        return WX - 7;
    }

    private void computeLine(int index) {
        LcdImageLine line = new LcdImageLine.Builder(LCD_WIDTH).build();
        if (isBackgroundActive()) {
            line = line.below(
                    readImageLine(ImageSource.BACKGROUND, (SCY + index) % IMAGE_SIZE)
                            .extractWrapped(SCX, LCD_WIDTH)
                            .mapColors(BGP)
            );
        }
        if (isWindowActive()) {
            if (winY - WY >= 0) {
                line = line.join(
                        readImageLine(ImageSource.WINDOW, winY - WY).shift(WXP())
                                .extractWrapped(0, LCD_WIDTH)
                                .mapColors(BGP),
                        WXP()
                );
            }
            //FIXME: am I sure I need the (if)?
            ++winY;
        }
        nextImageBuilder.setLine(index, line);
    }

    private void startDrawingImage() {
        winY = 0;
        nextImageBuilder = new LcdImage.Builder(LCD_WIDTH, LCD_HEIGHT);
    }

    private void finishDrawingImage() {
        currentImage = nextImageBuilder.build();
    }

    private int getMode() {
        return Bits.clip(2, STAT);
    }

    private void setMode(int mode) {
        STAT = (STAT & (-1 << 2)) | mode;
        handleModeInterrupt(mode);
        handleModeDrawing(mode);
    }

    private void handleModeInterrupt(int mode) {
        boolean raiseInterrupt = false;
        switch (mode) {
        case 0: raiseInterrupt = Bits.test(STAT, StatBits.INT_MODE0); break;
        case 1: raiseInterrupt = Bits.test(STAT, StatBits.INT_MODE1); break;
        case 2: raiseInterrupt = Bits.test(STAT, StatBits.INT_MODE2); break;
        }
        if (raiseInterrupt) {
            cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);
        }
        if (mode == 1) {
            cpu.requestInterrupt(Cpu.Interrupt.VBLANK);
        }
    }

    private void handleModeDrawing(int mode) {
        switch (mode) {
            case 1: {
                finishDrawingImage();
            } break;
            case 2: {
                if (LY == 0) {
                    startDrawingImage();
                }
            } break;
            case 3: {
                computeLine(LY);
            } break;
        }
    }

    private void check_LYC_EQ_LY() {
        //boolean old_LYC_EQ_LY = Bits.test(STAT, 2);
        boolean new_LYC_EQ_LY = LYC == LY;
        //if (old_LYC_EQ_LY != new_LYC_EQ_LY) {
            STAT = Bits.set(STAT, StatBits.INT_LYC, new_LYC_EQ_LY);
            if (new_LYC_EQ_LY && Bits.test(STAT, StatBits.INT_LYC)) {
                cpu.requestInterrupt(Cpu.Interrupt.LCD_STAT);
            }
        //}
    }

    /** Debug helpers */
    private void printCurrentImage() {
        for (int y = 0; y < currentImage.height(); ++y) {
            for (int x = 0; x < currentImage.width(); ++x) {
                System.out.print(currentImage.get(x, y));
            }
            System.out.print('\n');
        }
    }
    private void printBackgroundTileIndices() {
        for (int y = 0; y < IMAGE_WIDTH; ++y) {
            for (int x = 0; x < IMAGE_WIDTH; ++x) {
                System.out.printf("%d ", getTileImageAddress(ImageSource.BACKGROUND, y * 32 + x));
            }
            System.out.println("");
        }
    }
    private void printLine(LcdImageLine line) {
        LcdImage imageFromLine = new LcdImage.Builder(line.size(), 1).setLine(0, line).build();
        for (int x = 0; x < imageFromLine.width(); ++x) {
            System.out.print(imageFromLine.get(x, 0));
        }
        System.out.println("");
    }

}
