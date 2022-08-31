package org.broad.igv.sam;

/// Logic to decode CCS pulse data from uint8 compressed code to an approximation of the
/// original frame count.
///
public class CCSKineticsDecoder {

    private static final int base = 1 << 6;  // == 2 ^ mantissa-bits == 2^6 == 64

    private final short[] framePoints;

    public CCSKineticsDecoder() {
        this.framePoints = new short[256];
        for (int i = 0; i < 256; ++i) {
            this.framePoints[i] = decodeFrame((byte) i);
        }
    }

    /// Convert pulse byte to frame count from cached LUT
    public short decode(byte val) {
        return this.framePoints[val & 0xFF];
    }

    /// Compute frame count from compressed pulse byte
    ///
    /// 8-bit compressed V1 codec:
    ///
    ///   xxmm'mmmm
    ///
    /// where
    /// * 'x' represents the exponent (2 MSBs)
    /// * 'm' represents the mantissa (6 LSBs)
    ///
    private static short decodeFrame(byte val) {
        int mantissa = val & 0b0011_1111;
        int exponent = (val & 0b1100_0000) >> 6;
        int exponentVal = 1 << exponent;
        return (short) (base * (exponentVal - 1) + exponentVal * mantissa);
    }
}
