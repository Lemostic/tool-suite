package io.github.lemostic.toolsuite.modules.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QRCodeService {

    private static final int DEFAULT_SIZE = 400;

    public Image generateQRCode(String text, int size, Color foreground, Color background) {
        return generateQRCode(text, size, foreground, background, ErrorCorrectionLevel.M);
    }

    public Image generateQRCode(String text, int size, Color foreground, Color background,
                                ErrorCorrectionLevel ecc) {
        int qrSize = size <= 0 ? DEFAULT_SIZE : size;

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ecc);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 2);

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, qrSize, qrSize, hints);
            return toFxImage(bitMatrix, qrSize, qrSize, foreground, background);
        } catch (WriterException e) {
            return createErrorImage();
        }
    }

    private Image toFxImage(BitMatrix matrix, int width, int height, Color fg, Color bg) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();

        int fgArgb = argbFromColor(fg);
        int bgArgb = argbFromColor(bg);

        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = matrix.get(x, y) ? fgArgb : bgArgb;
            }
        }

        writer.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return image;
    }

    private int argbFromColor(Color color) {
        int a = (int) Math.round(color.getOpacity() * 255);
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private Image createErrorImage() {
        int w = 200, h = 60;
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, 0xFFFFFFFF);
            }
        }
        return img;
    }
}
