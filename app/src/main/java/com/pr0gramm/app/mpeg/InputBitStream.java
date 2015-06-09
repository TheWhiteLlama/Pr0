package com.pr0gramm.app.mpeg;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputBitStream {
    private static final int BUFFER_SIZE = 4096;

    private InputStream stream;

    // General stuff

    private byte[] buffer = new byte[BUFFER_SIZE];
    private int bufferHead = 0;
    private int bytesLeft = 0;
    private int bitsLeft = 0;
    private int byteBuffer;

    public InputBitStream(InputStream stream) {
        this.stream = stream;
    }

    public final int readBit() throws IOException {
        if (bitsLeft > 0) {
            --bitsLeft;
            byteBuffer <<= 1;
            return (byteBuffer & 0x100) >> 8;
        } else {
            if (bytesLeft > 0) {
                --bytesLeft;
                bitsLeft = 7;
                ++bufferHead;
                byteBuffer = buffer[bufferHead] << 1;
                return (byteBuffer & 0x100) >> 8;
            } else {
                bytesLeft = stream.read(buffer, 0, BUFFER_SIZE) - 1;
                if (bytesLeft == -2) {
                    throw new EOFException();
                }
                bitsLeft = 7;
                bufferHead = 0;
                byteBuffer = buffer[0] << 1;
                return (byteBuffer & 0x100) >> 8;
            }
        }
    }

    public final int readBits(int n) throws IOException {
        int r = 0;
        while (n > 0) {
            if (bitsLeft == 0) {
                if (bytesLeft > 0) {
                    --bytesLeft;
                    bitsLeft = 8;
                    ++bufferHead;
                    byteBuffer = buffer[bufferHead];
                } else {
                    bytesLeft = stream.read(buffer, 0, BUFFER_SIZE) - 1;
                    if (bytesLeft == -2) {
                        throw new EOFException();
                    }
                    bitsLeft = 8;
                    bufferHead = 0;
                    byteBuffer = buffer[0];
                }
            }
            int bits = (bitsLeft < n ? bitsLeft : n);
            n -= bits;
            r <<= bits;
            r |= (byteBuffer & (-1 << (8 - bits)) & 0xff) >> (8 - bits);
            byteBuffer <<= bits;
            bitsLeft -= bits;
        }
        return r;
    }

    public final int readNextByte() throws IOException {
        bitsLeft = 0;
        if (bytesLeft > 0) {
            --bytesLeft;
            ++bufferHead;
            return buffer[bufferHead] & 0xff;
        } else {
            bytesLeft = stream.read(buffer, 0, BUFFER_SIZE) - 1;
            if (bytesLeft == -2) {
                throw new EOFException();
            }
            bufferHead = 0;
            return buffer[0] & 0xff;
        }
    }

    // MPEG-specific stuff

    public final boolean noStartCode() throws IOException {
        if ((byteBuffer & 0xff) != 0) return true;

        if (bytesLeft < 3) {
            int i;
            for (i = 0; i <= bytesLeft; ++i) {
                buffer[i] = buffer[bufferHead];
                ++bufferHead;
            }
            bufferHead = 0;
            i = stream.read(buffer, i, BUFFER_SIZE - i);
            bytesLeft += i;
            if (bytesLeft < 3) {
                throw new EOFException();
            }
        }

        return buffer[bufferHead + 1] != 0 ||
                buffer[bufferHead + 2] != 0 || buffer[bufferHead + 3] != 1;
    }


    // MPEG-1 VLC

    //  macroblock_stuffing decodes as 34.
    //  macroblock_escape decodes as 35.

    public static final int[] macroblockAddressIncrement = {
            3, 2 * 3, 0, //   0
            3 * 3, 4 * 3, 0, //   1  0
            0, 0, 1, //   2  1.
            5 * 3, 6 * 3, 0, //   3  00
            7 * 3, 8 * 3, 0, //   4  01
            9 * 3, 10 * 3, 0, //   5  000
            11 * 3, 12 * 3, 0, //   6  001
            0, 0, 3, //   7  010.
            0, 0, 2, //   8  011.
            13 * 3, 14 * 3, 0, //   9  0000
            15 * 3, 16 * 3, 0, //  10  0001
            0, 0, 5, //  11  0010.
            0, 0, 4, //  12  0011.
            17 * 3, 18 * 3, 0, //  13  0000 0
            19 * 3, 20 * 3, 0, //  14  0000 1
            0, 0, 7, //  15  0001 0.
            0, 0, 6, //  16  0001 1.
            21 * 3, 22 * 3, 0, //  17  0000 00
            23 * 3, 24 * 3, 0, //  18  0000 01
            25 * 3, 26 * 3, 0, //  19  0000 10
            27 * 3, 28 * 3, 0, //  20  0000 11
            -1, 29 * 3, 0, //  21  0000 000
            -1, 30 * 3, 0, //  22  0000 001
            31 * 3, 32 * 3, 0, //  23  0000 010
            33 * 3, 34 * 3, 0, //  24  0000 011
            35 * 3, 36 * 3, 0, //  25  0000 100
            37 * 3, 38 * 3, 0, //  26  0000 101
            0, 0, 9, //  27  0000 110.
            0, 0, 8, //  28  0000 111.
            39 * 3, 40 * 3, 0, //  29  0000 0001
            41 * 3, 42 * 3, 0, //  30  0000 0011
            43 * 3, 44 * 3, 0, //  31  0000 0100
            45 * 3, 46 * 3, 0, //  32  0000 0101
            0, 0, 15, //  33  0000 0110.
            0, 0, 14, //  34  0000 0111.
            0, 0, 13, //  35  0000 1000.
            0, 0, 12, //  36  0000 1001.
            0, 0, 11, //  37  0000 1010.
            0, 0, 10, //  38  0000 1011.
            47 * 3, -1, 0, //  39  0000 0001 0
            -1, 48 * 3, 0, //  40  0000 0001 1
            49 * 3, 50 * 3, 0, //  41  0000 0011 0
            51 * 3, 52 * 3, 0, //  42  0000 0011 1
            53 * 3, 54 * 3, 0, //  43  0000 0100 0
            55 * 3, 56 * 3, 0, //  44  0000 0100 1
            57 * 3, 58 * 3, 0, //  45  0000 0101 0
            59 * 3, 60 * 3, 0, //  46  0000 0101 1
            61 * 3, -1, 0, //  47  0000 0001 00
            -1, 62 * 3, 0, //  48  0000 0001 11
            63 * 3, 64 * 3, 0, //  49  0000 0011 00
            65 * 3, 66 * 3, 0, //  50  0000 0011 01
            67 * 3, 68 * 3, 0, //  51  0000 0011 10
            69 * 3, 70 * 3, 0, //  52  0000 0011 11
            71 * 3, 72 * 3, 0, //  53  0000 0100 00
            73 * 3, 74 * 3, 0, //  54  0000 0100 01
            0, 0, 21, //  55  0000 0100 10.
            0, 0, 20, //  56  0000 0100 11.
            0, 0, 19, //  57  0000 0101 00.
            0, 0, 18, //  58  0000 0101 01.
            0, 0, 17, //  59  0000 0101 10.
            0, 0, 16, //  60  0000 0101 11.
            0, 0, 35, //  61  0000 0001 000. -- macroblock_escape
            0, 0, 34, //  62  0000 0001 111. -- macroblock_stuffing
            0, 0, 33, //  63  0000 0011 000.
            0, 0, 32, //  64  0000 0011 001.
            0, 0, 31, //  65  0000 0011 010.
            0, 0, 30, //  66  0000 0011 011.
            0, 0, 29, //  67  0000 0011 100.
            0, 0, 28, //  68  0000 0011 101.
            0, 0, 27, //  69  0000 0011 110.
            0, 0, 26, //  70  0000 0011 111.
            0, 0, 25, //  71  0000 0100 000.
            0, 0, 24, //  72  0000 0100 001.
            0, 0, 23, //  73  0000 0100 010.
            0, 0, 22  //  74  0000 0100 011.
    };

    //  macroblock_type bitmap:
    //    0x10  macroblock_quant
    //    0x08  macroblock_motion_forward
    //    0x04  macroblock_motion_backward
    //    0x02  macrobkock_pattern
    //    0x01  macroblock_intra
    //

    public static final int[] macroblockTypeI = {
            3, 2 * 3, 0, //   0
            -1, 3 * 3, 0, //   1  0
            0, 0, 0x01, //   2  1.
            0, 0, 0x11  //   3  01.
    };

    public static final int[] macroblockTypeP = {
            3, 2 * 3, 0, //  0
            3 * 3, 4 * 3, 0, //  1  0
            0, 0, 0x0a, //  2  1.
            5 * 3, 6 * 3, 0, //  3  00
            0, 0, 0x02, //  4  01.
            7 * 3, 8 * 3, 0, //  5  000
            0, 0, 0x08, //  6  001.
            9 * 3, 10 * 3, 0, //  7  0000
            11 * 3, 12 * 3, 0, //  8  0001
            -1, 13 * 3, 0, //  9  00000
            0, 0, 0x12, // 10  00001.
            0, 0, 0x1a, // 11  00010.
            0, 0, 0x01, // 12  00011.
            0, 0, 0x11  // 13  000001.
    };

    public static final int[] macroblockTypeB = {
            3, 2 * 3, 0,  //  0
            3 * 3, 5 * 3, 0,  //  1  0
            4 * 3, 6 * 3, 0,  //  2  1
            8 * 3, 7 * 3, 0,  //  3  00
            0, 0, 0x0c,  //  4  10.
            9 * 3, 10 * 3, 0,  //  5  01
            0, 0, 0x0e,  //  6  11.
            13 * 3, 14 * 3, 0,  //  7  001
            12 * 3, 11 * 3, 0,  //  8  000
            0, 0, 0x04,  //  9  010.
            0, 0, 0x06,  // 10  011.
            18 * 3, 16 * 3, 0,  // 11  0001
            15 * 3, 17 * 3, 0,  // 12  0000
            0, 0, 0x08,  // 13  0010.
            0, 0, 0x0a,  // 14  0011.
            -1, 19 * 3, 0,  // 15  00000
            0, 0, 0x01,  // 16  00011.
            20 * 3, 21 * 3, 0,  // 17  00001
            0, 0, 0x1e,  // 18  00010.
            0, 0, 0x11,  // 19  000001.
            0, 0, 0x16,  // 20  000010.
            0, 0, 0x1a   // 21  000011.
    };

    public static final int[] codedBlockPattern = {
            2 * 3, 3, 0,  //   0
            3 * 3, 6 * 3, 0,  //   1  1
            4 * 3, 5 * 3, 0,  //   2  0
            8 * 3, 11 * 3, 0,  //   3  10
            12 * 3, 13 * 3, 0,  //   4  00
            9 * 3, 7 * 3, 0,  //   5  01
            10 * 3, 14 * 3, 0,  //   6  11
            20 * 3, 19 * 3, 0,  //   7  011
            18 * 3, 16 * 3, 0,  //   8  100
            23 * 3, 17 * 3, 0,  //   9  010
            27 * 3, 25 * 3, 0,  //  10  110
            21 * 3, 28 * 3, 0,  //  11  101
            15 * 3, 22 * 3, 0,  //  12  000
            24 * 3, 26 * 3, 0,  //  13  001
            0, 0, 60,  //  14  111.
            35 * 3, 40 * 3, 0,  //  15  0000
            44 * 3, 48 * 3, 0,  //  16  1001
            38 * 3, 36 * 3, 0,  //  17  0101
            42 * 3, 47 * 3, 0,  //  18  1000
            29 * 3, 31 * 3, 0,  //  19  0111
            39 * 3, 32 * 3, 0,  //  20  0110
            0, 0, 32,  //  21  1010.
            45 * 3, 46 * 3, 0,  //  22  0001
            33 * 3, 41 * 3, 0,  //  23  0100
            43 * 3, 34 * 3, 0,  //  24  0010
            0, 0, 4,  //  25  1101.
            30 * 3, 37 * 3, 0,  //  26  0011
            0, 0, 8,  //  27  1100.
            0, 0, 16,  //  28  1011.
            0, 0, 44,  //  29  0111 0.
            50 * 3, 56 * 3, 0,  //  30  0011 0
            0, 0, 28,  //  31  0111 1.
            0, 0, 52,  //  32  0110 1.
            0, 0, 62,  //  33  0100 0.
            61 * 3, 59 * 3, 0,  //  34  0010 1
            52 * 3, 60 * 3, 0,  //  35  0000 0
            0, 0, 1,  //  36  0101 1.
            55 * 3, 54 * 3, 0,  //  37  0011 1
            0, 0, 61,  //  38  0101 0.
            0, 0, 56,  //  39  0110 0.
            57 * 3, 58 * 3, 0,  //  40  0000 1
            0, 0, 2,  //  41  0100 1.
            0, 0, 40,  //  42  1000 0.
            51 * 3, 62 * 3, 0,  //  43  0010 0
            0, 0, 48,  //  44  1001 0.
            64 * 3, 63 * 3, 0,  //  45  0001 0
            49 * 3, 53 * 3, 0,  //  46  0001 1
            0, 0, 20,  //  47  1000 1.
            0, 0, 12,  //  48  1001 1.
            80 * 3, 83 * 3, 0,  //  49  0001 10
            0, 0, 63,  //  50  0011 00.
            77 * 3, 75 * 3, 0,  //  51  0010 00
            65 * 3, 73 * 3, 0,  //  52  0000 00
            84 * 3, 66 * 3, 0,  //  53  0001 11
            0, 0, 24,  //  54  0011 11.
            0, 0, 36,  //  55  0011 10.
            0, 0, 3,  //  56  0011 01.
            69 * 3, 87 * 3, 0,  //  57  0000 10
            81 * 3, 79 * 3, 0,  //  58  0000 11
            68 * 3, 71 * 3, 0,  //  59  0010 11
            70 * 3, 78 * 3, 0,  //  60  0000 01
            67 * 3, 76 * 3, 0,  //  61  0010 10
            72 * 3, 74 * 3, 0,  //  62  0010 01
            86 * 3, 85 * 3, 0,  //  63  0001 01
            88 * 3, 82 * 3, 0,  //  64  0001 00
            -1, 94 * 3, 0,  //  65  0000 000
            95 * 3, 97 * 3, 0,  //  66  0001 111
            0, 0, 33,  //  67  0010 100.
            0, 0, 9,  //  68  0010 110.
            106 * 3, 110 * 3, 0,  //  69  0000 100
            102 * 3, 116 * 3, 0,  //  70  0000 010
            0, 0, 5,  //  71  0010 111.
            0, 0, 10,  //  72  0010 010.
            93 * 3, 89 * 3, 0,  //  73  0000 001
            0, 0, 6,  //  74  0010 011.
            0, 0, 18,  //  75  0010 001.
            0, 0, 17,  //  76  0010 101.
            0, 0, 34,  //  77  0010 000.
            113 * 3, 119 * 3, 0,  //  78  0000 011
            103 * 3, 104 * 3, 0,  //  79  0000 111
            90 * 3, 92 * 3, 0,  //  80  0001 100
            109 * 3, 107 * 3, 0,  //  81  0000 110
            117 * 3, 118 * 3, 0,  //  82  0001 001
            101 * 3, 99 * 3, 0,  //  83  0001 101
            98 * 3, 96 * 3, 0,  //  84  0001 110
            100 * 3, 91 * 3, 0,  //  85  0001 011
            114 * 3, 115 * 3, 0,  //  86  0001 010
            105 * 3, 108 * 3, 0,  //  87  0000 101
            112 * 3, 111 * 3, 0,  //  88  0001 000
            121 * 3, 125 * 3, 0,  //  89  0000 0011
            0, 0, 41,  //  90  0001 1000.
            0, 0, 14,  //  91  0001 0111.
            0, 0, 21,  //  92  0001 1001.
            124 * 3, 122 * 3, 0,  //  93  0000 0010
            120 * 3, 123 * 3, 0,  //  94  0000 0001
            0, 0, 11,  //  95  0001 1110.
            0, 0, 19,  //  96  0001 1101.
            0, 0, 7,  //  97  0001 1111.
            0, 0, 35,  //  98  0001 1100.
            0, 0, 13,  //  99  0001 1011.
            0, 0, 50,  // 100  0001 0110.
            0, 0, 49,  // 101  0001 1010.
            0, 0, 58,  // 102  0000 0100.
            0, 0, 37,  // 103  0000 1110.
            0, 0, 25,  // 104  0000 1111.
            0, 0, 45,  // 105  0000 1010.
            0, 0, 57,  // 106  0000 1000.
            0, 0, 26,  // 107  0000 1101.
            0, 0, 29,  // 108  0000 1011.
            0, 0, 38,  // 109  0000 1100.
            0, 0, 53,  // 110  0000 1001.
            0, 0, 23,  // 111  0001 0001.
            0, 0, 43,  // 112  0001 0000.
            0, 0, 46,  // 113  0000 0110.
            0, 0, 42,  // 114  0001 0100.
            0, 0, 22,  // 115  0001 0101.
            0, 0, 54,  // 116  0000 0101.
            0, 0, 51,  // 117  0001 0010.
            0, 0, 15,  // 118  0001 0011.
            0, 0, 30,  // 119  0000 0111.
            0, 0, 39,  // 120  0000 0001 0.
            0, 0, 47,  // 121  0000 0011 0.
            0, 0, 55,  // 122  0000 0010 1.
            0, 0, 27,  // 123  0000 0001 1.
            0, 0, 59,  // 124  0000 0010 0.
            0, 0, 31   // 125  0000 0011 1.
    };

    public static final int[] motion = {
            3, 2 * 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  0
            0, 0, 0,  //   2  1.
            6 * 3, 5 * 3, 0,  //   3  01
            8 * 3, 7 * 3, 0,  //   4  00
            0, 0, -1,  //   5  011.
            0, 0, 1,  //   6  010.
            9 * 3, 10 * 3, 0,  //   7  001
            12 * 3, 11 * 3, 0,  //   8  000
            0, 0, 2,  //   9  0010.
            0, 0, -2,  //  10  0011.
            14 * 3, 15 * 3, 0,  //  11  0001
            16 * 3, 13 * 3, 0,  //  12  0000
            20 * 3, 18 * 3, 0,  //  13  0000 1
            0, 0, 3,  //  14  0001 0.
            0, 0, -3,  //  15  0001 1.
            17 * 3, 19 * 3, 0,  //  16  0000 0
            -1, 23 * 3, 0,  //  17  0000 00
            27 * 3, 25 * 3, 0,  //  18  0000 11
            26 * 3, 21 * 3, 0,  //  19  0000 01
            24 * 3, 22 * 3, 0,  //  20  0000 10
            32 * 3, 28 * 3, 0,  //  21  0000 011
            29 * 3, 31 * 3, 0,  //  22  0000 101
            -1, 33 * 3, 0,  //  23  0000 001
            36 * 3, 35 * 3, 0,  //  24  0000 100
            0, 0, -4,  //  25  0000 111.
            30 * 3, 34 * 3, 0,  //  26  0000 010
            0, 0, 4,  //  27  0000 110.
            0, 0, -7,  //  28  0000 0111.
            0, 0, 5,  //  29  0000 1010.
            37 * 3, 41 * 3, 0,  //  30  0000 0100
            0, 0, -5,  //  31  0000 1011.
            0, 0, 7,  //  32  0000 0110.
            38 * 3, 40 * 3, 0,  //  33  0000 0011
            42 * 3, 39 * 3, 0,  //  34  0000 0101
            0, 0, -6,  //  35  0000 1001.
            0, 0, 6,  //  36  0000 1000.
            51 * 3, 54 * 3, 0,  //  37  0000 0100 0
            50 * 3, 49 * 3, 0,  //  38  0000 0011 0
            45 * 3, 46 * 3, 0,  //  39  0000 0101 1
            52 * 3, 47 * 3, 0,  //  40  0000 0011 1
            43 * 3, 53 * 3, 0,  //  41  0000 0100 1
            44 * 3, 48 * 3, 0,  //  42  0000 0101 0
            0, 0, 10,  //  43  0000 0100 10.
            0, 0, 9,  //  44  0000 0101 00.
            0, 0, 8,  //  45  0000 0101 10.
            0, 0, -8,  //  46  0000 0101 11.
            57 * 3, 66 * 3, 0,  //  47  0000 0011 11
            0, 0, -9,  //  48  0000 0101 01.
            60 * 3, 64 * 3, 0,  //  49  0000 0011 01
            56 * 3, 61 * 3, 0,  //  50  0000 0011 00
            55 * 3, 62 * 3, 0,  //  51  0000 0100 00
            58 * 3, 63 * 3, 0,  //  52  0000 0011 10
            0, 0, -10,  //  53  0000 0100 11.
            59 * 3, 65 * 3, 0,  //  54  0000 0100 01
            0, 0, 12,  //  55  0000 0100 000.
            0, 0, 16,  //  56  0000 0011 000.
            0, 0, 13,  //  57  0000 0011 110.
            0, 0, 14,  //  58  0000 0011 100.
            0, 0, 11,  //  59  0000 0100 010.
            0, 0, 15,  //  60  0000 0011 010.
            0, 0, -16,  //  61  0000 0011 001.
            0, 0, -12,  //  62  0000 0100 001.
            0, 0, -14,  //  63  0000 0011 101.
            0, 0, -15,  //  64  0000 0011 011.
            0, 0, -11,  //  65  0000 0100 011.
            0, 0, -13   //  66  0000 0011 111.
    };

    public static final int[] DCT_DC_SizeLuminance = {
            2 * 3, 3, 0,  //   0
            6 * 3, 5 * 3, 0,  //   1  1
            3 * 3, 4 * 3, 0,  //   2  0
            0, 0, 1,  //   3  00.
            0, 0, 2,  //   4  01.
            9 * 3, 8 * 3, 0,  //   5  11
            7 * 3, 10 * 3, 0,  //   6  10
            0, 0, 0,  //   7  100.
            12 * 3, 11 * 3, 0,  //   8  111
            0, 0, 4,  //   9  110.
            0, 0, 3,  //  10  101.
            13 * 3, 14 * 3, 0,  //  11  1111
            0, 0, 5,  //  12  1110.
            0, 0, 6,  //  13  1111 0.
            16 * 3, 15 * 3, 0,  //  14  1111 1
            17 * 3, -1, 0,  //  15  1111 11
            0, 0, 7,  //  16  1111 10.
            0, 0, 8   //  17  1111 110.
    };

    public static final int[] DCT_DC_SizeChrominance = {
            2 * 3, 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  1
            6 * 3, 5 * 3, 0,  //   2  0
            8 * 3, 7 * 3, 0,  //   3  11
            0, 0, 2,  //   4  10.
            0, 0, 1,  //   5  01.
            0, 0, 0,  //   6  00.
            10 * 3, 9 * 3, 0,  //   7  111
            0, 0, 3,  //   8  110.
            12 * 3, 11 * 3, 0,  //   9  1111
            0, 0, 4,  //  10  1110.
            14 * 3, 13 * 3, 0,  //  11  1111 1
            0, 0, 5,  //  12  1111 0.
            16 * 3, 15 * 3, 0,  //  13  1111 11
            0, 0, 6,  //  14  1111 10.
            17 * 3, -1, 0,  //  15  1111 111
            0, 0, 7,  //  16  1111 110.
            0, 0, 8   //  17  1111 1110.
    };

    //  dct_coeff bitmap:
    //    0xff00  run
    //    0x00ff  level

    //  Decoded values are unsigned. Sign bit follows in the stream.

    //  Interpretation of the value 0x0001
    //    for dc_coeff_first:  run=0, level=1
    //    for dc_coeff_next:   If the next bit is 1: run=0, level=1
    //                         If the next bit is 0: end_of_block

    //  escape decodes as 0xffff.

    public static final int[] DCT_Coeff = {
            3, 2 * 3, 0,  //   0
            4 * 3, 3 * 3, 0,  //   1  0
            0, 0, 0x0001,  //   2  1.
            7 * 3, 8 * 3, 0,  //   3  01
            6 * 3, 5 * 3, 0,  //   4  00
            13 * 3, 9 * 3, 0,  //   5  001
            11 * 3, 10 * 3, 0,  //   6  000
            14 * 3, 12 * 3, 0,  //   7  010
            0, 0, 0x0101,  //   8  011.
            20 * 3, 22 * 3, 0,  //   9  0011
            18 * 3, 21 * 3, 0,  //  10  0001
            16 * 3, 19 * 3, 0,  //  11  0000
            0, 0, 0x0201,  //  12  0101.
            17 * 3, 15 * 3, 0,  //  13  0010
            0, 0, 0x0002,  //  14  0100.
            0, 0, 0x0003,  //  15  0010 1.
            27 * 3, 25 * 3, 0,  //  16  0000 0
            29 * 3, 31 * 3, 0,  //  17  0010 0
            24 * 3, 26 * 3, 0,  //  18  0001 0
            32 * 3, 30 * 3, 0,  //  19  0000 1
            0, 0, 0x0401,  //  20  0011 0.
            23 * 3, 28 * 3, 0,  //  21  0001 1
            0, 0, 0x0301,  //  22  0011 1.
            0, 0, 0x0102,  //  23  0001 10.
            0, 0, 0x0701,  //  24  0001 00.
            0, 0, 0xffff,  //  25  0000 01. -- escape
            0, 0, 0x0601,  //  26  0001 01.
            37 * 3, 36 * 3, 0,  //  27  0000 00
            0, 0, 0x0501,  //  28  0001 11.
            35 * 3, 34 * 3, 0,  //  29  0010 00
            39 * 3, 38 * 3, 0,  //  30  0000 11
            33 * 3, 42 * 3, 0,  //  31  0010 01
            40 * 3, 41 * 3, 0,  //  32  0000 10
            52 * 3, 50 * 3, 0,  //  33  0010 010
            54 * 3, 53 * 3, 0,  //  34  0010 001
            48 * 3, 49 * 3, 0,  //  35  0010 000
            43 * 3, 45 * 3, 0,  //  36  0000 001
            46 * 3, 44 * 3, 0,  //  37  0000 000
            0, 0, 0x0801,  //  38  0000 111.
            0, 0, 0x0004,  //  39  0000 110.
            0, 0, 0x0202,  //  40  0000 100.
            0, 0, 0x0901,  //  41  0000 101.
            51 * 3, 47 * 3, 0,  //  42  0010 011
            55 * 3, 57 * 3, 0,  //  43  0000 0010
            60 * 3, 56 * 3, 0,  //  44  0000 0001
            59 * 3, 58 * 3, 0,  //  45  0000 0011
            61 * 3, 62 * 3, 0,  //  46  0000 0000
            0, 0, 0x0a01,  //  47  0010 0111.
            0, 0, 0x0d01,  //  48  0010 0000.
            0, 0, 0x0006,  //  49  0010 0001.
            0, 0, 0x0103,  //  50  0010 0101.
            0, 0, 0x0005,  //  51  0010 0110.
            0, 0, 0x0302,  //  52  0010 0100.
            0, 0, 0x0b01,  //  53  0010 0011.
            0, 0, 0x0c01,  //  54  0010 0010.
            76 * 3, 75 * 3, 0,  //  55  0000 0010 0
            67 * 3, 70 * 3, 0,  //  56  0000 0001 1
            73 * 3, 71 * 3, 0,  //  57  0000 0010 1
            78 * 3, 74 * 3, 0,  //  58  0000 0011 1
            72 * 3, 77 * 3, 0,  //  59  0000 0011 0
            69 * 3, 64 * 3, 0,  //  60  0000 0001 0
            68 * 3, 63 * 3, 0,  //  61  0000 0000 0
            66 * 3, 65 * 3, 0,  //  62  0000 0000 1
            81 * 3, 87 * 3, 0,  //  63  0000 0000 01
            91 * 3, 80 * 3, 0,  //  64  0000 0001 01
            82 * 3, 79 * 3, 0,  //  65  0000 0000 11
            83 * 3, 86 * 3, 0,  //  66  0000 0000 10
            93 * 3, 92 * 3, 0,  //  67  0000 0001 10
            84 * 3, 85 * 3, 0,  //  68  0000 0000 00
            90 * 3, 94 * 3, 0,  //  69  0000 0001 00
            88 * 3, 89 * 3, 0,  //  70  0000 0001 11
            0, 0, 0x0203,  //  71  0000 0010 11.
            0, 0, 0x0104,  //  72  0000 0011 00.
            0, 0, 0x0007,  //  73  0000 0010 10.
            0, 0, 0x0402,  //  74  0000 0011 11.
            0, 0, 0x0502,  //  75  0000 0010 01.
            0, 0, 0x1001,  //  76  0000 0010 00.
            0, 0, 0x0f01,  //  77  0000 0011 01.
            0, 0, 0x0e01,  //  78  0000 0011 10.
            105 * 3, 107 * 3, 0,  //  79  0000 0000 111
            111 * 3, 114 * 3, 0,  //  80  0000 0001 011
            104 * 3, 97 * 3, 0,  //  81  0000 0000 010
            125 * 3, 119 * 3, 0,  //  82  0000 0000 110
            96 * 3, 98 * 3, 0,  //  83  0000 0000 100
            -1, 123 * 3, 0,  //  84  0000 0000 000
            95 * 3, 101 * 3, 0,  //  85  0000 0000 001
            106 * 3, 121 * 3, 0,  //  86  0000 0000 101
            99 * 3, 102 * 3, 0,  //  87  0000 0000 011
            113 * 3, 103 * 3, 0,  //  88  0000 0001 110
            112 * 3, 116 * 3, 0,  //  89  0000 0001 111
            110 * 3, 100 * 3, 0,  //  90  0000 0001 000
            124 * 3, 115 * 3, 0,  //  91  0000 0001 010
            117 * 3, 122 * 3, 0,  //  92  0000 0001 101
            109 * 3, 118 * 3, 0,  //  93  0000 0001 100
            120 * 3, 108 * 3, 0,  //  94  0000 0001 001
            127 * 3, 136 * 3, 0,  //  95  0000 0000 0010
            139 * 3, 140 * 3, 0,  //  96  0000 0000 1000
            130 * 3, 126 * 3, 0,  //  97  0000 0000 0101
            145 * 3, 146 * 3, 0,  //  98  0000 0000 1001
            128 * 3, 129 * 3, 0,  //  99  0000 0000 0110
            0, 0, 0x0802,  // 100  0000 0001 0001.
            132 * 3, 134 * 3, 0,  // 101  0000 0000 0011
            155 * 3, 154 * 3, 0,  // 102  0000 0000 0111
            0, 0, 0x0008,  // 103  0000 0001 1101.
            137 * 3, 133 * 3, 0,  // 104  0000 0000 0100
            143 * 3, 144 * 3, 0,  // 105  0000 0000 1110
            151 * 3, 138 * 3, 0,  // 106  0000 0000 1010
            142 * 3, 141 * 3, 0,  // 107  0000 0000 1111
            0, 0, 0x000a,  // 108  0000 0001 0011.
            0, 0, 0x0009,  // 109  0000 0001 1000.
            0, 0, 0x000b,  // 110  0000 0001 0000.
            0, 0, 0x1501,  // 111  0000 0001 0110.
            0, 0, 0x0602,  // 112  0000 0001 1110.
            0, 0, 0x0303,  // 113  0000 0001 1100.
            0, 0, 0x1401,  // 114  0000 0001 0111.
            0, 0, 0x0702,  // 115  0000 0001 0101.
            0, 0, 0x1101,  // 116  0000 0001 1111.
            0, 0, 0x1201,  // 117  0000 0001 1010.
            0, 0, 0x1301,  // 118  0000 0001 1001.
            148 * 3, 152 * 3, 0,  // 119  0000 0000 1101
            0, 0, 0x0403,  // 120  0000 0001 0010.
            153 * 3, 150 * 3, 0,  // 121  0000 0000 1011
            0, 0, 0x0105,  // 122  0000 0001 1011.
            131 * 3, 135 * 3, 0,  // 123  0000 0000 0001
            0, 0, 0x0204,  // 124  0000 0001 0100.
            149 * 3, 147 * 3, 0,  // 125  0000 0000 1100
            172 * 3, 173 * 3, 0,  // 126  0000 0000 0101 1
            162 * 3, 158 * 3, 0,  // 127  0000 0000 0010 0
            170 * 3, 161 * 3, 0,  // 128  0000 0000 0110 0
            168 * 3, 166 * 3, 0,  // 129  0000 0000 0110 1
            157 * 3, 179 * 3, 0,  // 130  0000 0000 0101 0
            169 * 3, 167 * 3, 0,  // 131  0000 0000 0001 0
            174 * 3, 171 * 3, 0,  // 132  0000 0000 0011 0
            178 * 3, 177 * 3, 0,  // 133  0000 0000 0100 1
            156 * 3, 159 * 3, 0,  // 134  0000 0000 0011 1
            164 * 3, 165 * 3, 0,  // 135  0000 0000 0001 1
            183 * 3, 182 * 3, 0,  // 136  0000 0000 0010 1
            175 * 3, 176 * 3, 0,  // 137  0000 0000 0100 0
            0, 0, 0x0107,  // 138  0000 0000 1010 1.
            0, 0, 0x0a02,  // 139  0000 0000 1000 0.
            0, 0, 0x0902,  // 140  0000 0000 1000 1.
            0, 0, 0x1601,  // 141  0000 0000 1111 1.
            0, 0, 0x1701,  // 142  0000 0000 1111 0.
            0, 0, 0x1901,  // 143  0000 0000 1110 0.
            0, 0, 0x1801,  // 144  0000 0000 1110 1.
            0, 0, 0x0503,  // 145  0000 0000 1001 0.
            0, 0, 0x0304,  // 146  0000 0000 1001 1.
            0, 0, 0x000d,  // 147  0000 0000 1100 1.
            0, 0, 0x000c,  // 148  0000 0000 1101 0.
            0, 0, 0x000e,  // 149  0000 0000 1100 0.
            0, 0, 0x000f,  // 150  0000 0000 1011 1.
            0, 0, 0x0205,  // 151  0000 0000 1010 0.
            0, 0, 0x1a01,  // 152  0000 0000 1101 1.
            0, 0, 0x0106,  // 153  0000 0000 1011 0.
            180 * 3, 181 * 3, 0,  // 154  0000 0000 0111 1
            160 * 3, 163 * 3, 0,  // 155  0000 0000 0111 0
            196 * 3, 199 * 3, 0,  // 156  0000 0000 0011 10
            0, 0, 0x001b,  // 157  0000 0000 0101 00.
            203 * 3, 185 * 3, 0,  // 158  0000 0000 0010 01
            202 * 3, 201 * 3, 0,  // 159  0000 0000 0011 11
            0, 0, 0x0013,  // 160  0000 0000 0111 00.
            0, 0, 0x0016,  // 161  0000 0000 0110 01.
            197 * 3, 207 * 3, 0,  // 162  0000 0000 0010 00
            0, 0, 0x0012,  // 163  0000 0000 0111 01.
            191 * 3, 192 * 3, 0,  // 164  0000 0000 0001 10
            188 * 3, 190 * 3, 0,  // 165  0000 0000 0001 11
            0, 0, 0x0014,  // 166  0000 0000 0110 11.
            184 * 3, 194 * 3, 0,  // 167  0000 0000 0001 01
            0, 0, 0x0015,  // 168  0000 0000 0110 10.
            186 * 3, 193 * 3, 0,  // 169  0000 0000 0001 00
            0, 0, 0x0017,  // 170  0000 0000 0110 00.
            204 * 3, 198 * 3, 0,  // 171  0000 0000 0011 01
            0, 0, 0x0019,  // 172  0000 0000 0101 10.
            0, 0, 0x0018,  // 173  0000 0000 0101 11.
            200 * 3, 205 * 3, 0,  // 174  0000 0000 0011 00
            0, 0, 0x001f,  // 175  0000 0000 0100 00.
            0, 0, 0x001e,  // 176  0000 0000 0100 01.
            0, 0, 0x001c,  // 177  0000 0000 0100 11.
            0, 0, 0x001d,  // 178  0000 0000 0100 10.
            0, 0, 0x001a,  // 179  0000 0000 0101 01.
            0, 0, 0x0011,  // 180  0000 0000 0111 10.
            0, 0, 0x0010,  // 181  0000 0000 0111 11.
            189 * 3, 206 * 3, 0,  // 182  0000 0000 0010 11
            187 * 3, 195 * 3, 0,  // 183  0000 0000 0010 10
            218 * 3, 211 * 3, 0,  // 184  0000 0000 0001 010
            0, 0, 0x0025,  // 185  0000 0000 0010 011.
            215 * 3, 216 * 3, 0,  // 186  0000 0000 0001 000
            0, 0, 0x0024,  // 187  0000 0000 0010 100.
            210 * 3, 212 * 3, 0,  // 188  0000 0000 0001 110
            0, 0, 0x0022,  // 189  0000 0000 0010 110.
            213 * 3, 209 * 3, 0,  // 190  0000 0000 0001 111
            221 * 3, 222 * 3, 0,  // 191  0000 0000 0001 100
            219 * 3, 208 * 3, 0,  // 192  0000 0000 0001 101
            217 * 3, 214 * 3, 0,  // 193  0000 0000 0001 001
            223 * 3, 220 * 3, 0,  // 194  0000 0000 0001 011
            0, 0, 0x0023,  // 195  0000 0000 0010 101.
            0, 0, 0x010b,  // 196  0000 0000 0011 100.
            0, 0, 0x0028,  // 197  0000 0000 0010 000.
            0, 0, 0x010c,  // 198  0000 0000 0011 011.
            0, 0, 0x010a,  // 199  0000 0000 0011 101.
            0, 0, 0x0020,  // 200  0000 0000 0011 000.
            0, 0, 0x0108,  // 201  0000 0000 0011 111.
            0, 0, 0x0109,  // 202  0000 0000 0011 110.
            0, 0, 0x0026,  // 203  0000 0000 0010 010.
            0, 0, 0x010d,  // 204  0000 0000 0011 010.
            0, 0, 0x010e,  // 205  0000 0000 0011 001.
            0, 0, 0x0021,  // 206  0000 0000 0010 111.
            0, 0, 0x0027,  // 207  0000 0000 0010 001.
            0, 0, 0x1f01,  // 208  0000 0000 0001 1011.
            0, 0, 0x1b01,  // 209  0000 0000 0001 1111.
            0, 0, 0x1e01,  // 210  0000 0000 0001 1100.
            0, 0, 0x1002,  // 211  0000 0000 0001 0101.
            0, 0, 0x1d01,  // 212  0000 0000 0001 1101.
            0, 0, 0x1c01,  // 213  0000 0000 0001 1110.
            0, 0, 0x010f,  // 214  0000 0000 0001 0011.
            0, 0, 0x0112,  // 215  0000 0000 0001 0000.
            0, 0, 0x0111,  // 216  0000 0000 0001 0001.
            0, 0, 0x0110,  // 217  0000 0000 0001 0010.
            0, 0, 0x0603,  // 218  0000 0000 0001 0100.
            0, 0, 0x0b02,  // 219  0000 0000 0001 1010.
            0, 0, 0x0e02,  // 220  0000 0000 0001 0111.
            0, 0, 0x0d02,  // 221  0000 0000 0001 1000.
            0, 0, 0x0c02,  // 222  0000 0000 0001 1001.
            0, 0, 0x0f02   // 223  0000 0000 0001 0110.
    };

    public final int readCode(int[] codeTable) throws IOException {
        int state = 0;
        try {
            do {
                state = codeTable[state + readBit()];
            } while (codeTable[state] != 0);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidVLCException();
        }
        return codeTable[state + 2];
    }

    public static class InvalidVLCException extends IOException {
    }
}