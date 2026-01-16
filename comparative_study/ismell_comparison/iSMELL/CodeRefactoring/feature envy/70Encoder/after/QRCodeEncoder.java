package com.itextpdf.text.pdf.qrcode;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.ArrayList;

public class QRCodeEncoder {
    private String encoding;
    private Mode mode;
    private BitVector dataBits;
    private QRCode qrCode;

    public QRCodeEncoder(String encoding, Mode mode, BitVector dataBits, QRCode qrCode) {
        this.encoding = encoding;
        this.mode = mode;
        this.dataBits = dataBits;
        this.qrCode = qrCode;
    }

    public void encodeContent(String content, ErrorCorrectionLevel ecLevel, Map<EncodeHintType,Object> hints) throws WriterException {
        // Step 1: Choose the mode (encoding).
        mode = chooseMode(content, encoding);

        // Step 2: Append "bytes" into "dataBits" in appropriate encoding.
        appendBytes(content, mode, dataBits, encoding);

        // Step 3: Initialize QR code that can contain "dataBits".
        int numInputBytes = dataBits.sizeInBytes();
        initQRCode(numInputBytes, ecLevel, mode, qrCode);

        // Step 4: Build another bit vector that contains header and data.
        BitVector headerAndDataBits = new BitVector();

        // Step 4.5: Append ECI message if applicable
        appendECIIfApplicable(mode, encoding, headerAndDataBits);

        appendModeInfo(mode, headerAndDataBits);

        int numLetters = mode.equals(Mode.BYTE) ? dataBits.sizeInBytes() : content.length();
        appendLengthInfo(numLetters, qrCode.getVersion(), mode, headerAndDataBits);
        headerAndDataBits.appendBitVector(dataBits);

        // Step 5: Terminate the bits properly.
        terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);

        // Step 6: Interleave data bits with error correction code.
        BitVector finalBits = new BitVector();
        interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(),
            qrCode.getNumRSBlocks(), finalBits);

        // Step 7: Choose the mask pattern and set to "qrCode".
        ByteMatrix matrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
        qrCode.setMaskPattern(chooseMaskPattern(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
            matrix));

        // Step 8.  Build the matrix and set it to "qrCode".
        MatrixUtil.buildMatrix(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
            qrCode.getMaskPattern(), matrix);
        qrCode.setMatrix(matrix);

        // Step 9.  Make sure we have a valid QR Code.
        if (!qrCode.isValid()) {
            throw new WriterException("Invalid QR code: " + qrCode.toString());
        }
    }

    private void appendECIIfApplicable(Mode mode, String encoding, BitVector headerAndDataBits) {
        if (mode == Mode.BYTE && !DEFAULT_BYTE_MODE_ENCODING.equals(encoding)) {
            CharacterSetECI eci = CharacterSetECI.getCharacterSetECIByName(encoding);
            if (eci != null) {
                appendECI(eci, headerAndDataBits);
            }
        }
    }

    private void validateQRCode(QRCode qrCode) throws WriterException {
        if (!qrCode.isValid()) {
            throw new WriterException("Invalid QR code: " + qrCode.toString());
        }
    }
}
