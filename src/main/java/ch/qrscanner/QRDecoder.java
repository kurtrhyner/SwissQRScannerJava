package ch.qrscanner;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Robuste QR-Dekodierung mit mehreren Vorverarbeitungsvarianten.
 * Versucht Standard → Kontrast → Invertiert → 2x-Skaliert → Adaptiv.
 */
public class QRDecoder {

    private static final QRCodeReader READER = new QRCodeReader();

    public static String decode(BufferedImage image) {
        for (BufferedImage variant : buildVariants(image)) {
            String result = tryDecode(variant);
            if (result != null) return result;
        }
        return null;
    }

    private static String tryDecode(BufferedImage img) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result result = READER.decode(bitmap, hints);
            return result.getText();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<BufferedImage> buildVariants(BufferedImage src) {
        List<BufferedImage> variants = new ArrayList<>();

        // 1. Original Graustufen
        BufferedImage gray = toGray(src);
        variants.add(gray);

        // 2. Kontrast erhöht
        variants.add(stretchContrast(gray));

        // 3. Invertiert
        variants.add(invert(gray));

        // 4. 2× hochskaliert
        BufferedImage up2 = scale(gray, 2.0);
        variants.add(up2);

        // 5. 2× hochskaliert + Kontrast
        variants.add(stretchContrast(up2));

        // 6. 2× hochskaliert + invertiert
        variants.add(invert(up2));

        // 7. Binarisiert (Otsu-ähnlich)
        variants.add(binarize(gray));

        // 8. Binarisiert + 2×
        variants.add(binarize(up2));

        return variants;
    }

    private static BufferedImage toGray(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static BufferedImage scale(BufferedImage src, double factor) {
        int w = (int)(src.getWidth()  * factor);
        int h = (int)(src.getHeight() * factor);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static BufferedImage stretchContrast(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getRaster().getPixels(0, 0, w, h, pixels);

        int min = 255, max = 0;
        for (int p : pixels) { min = Math.min(min, p); max = Math.max(max, p); }
        if (max == min) return src;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[] stretched = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++)
            stretched[i] = (pixels[i] - min) * 255 / (max - min);
        out.getRaster().setPixels(0, 0, w, h, stretched);
        return out;
    }

    private static BufferedImage invert(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getRaster().getPixels(0, 0, w, h, pixels);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[] inv = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) inv[i] = 255 - pixels[i];
        out.getRaster().setPixels(0, 0, w, h, inv);
        return out;
    }

    private static BufferedImage binarize(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getRaster().getPixels(0, 0, w, h, pixels);

        // Otsu-Schwellwert
        int[] hist = new int[256];
        for (int p : pixels) hist[p]++;
        int total = pixels.length;
        double sum = 0;
        for (int i = 0; i < 256; i++) sum += i * hist[i];
        double sumB = 0; int wB = 0;
        double maxVar = 0; int threshold = 128;
        for (int i = 0; i < 256; i++) {
            wB += hist[i]; if (wB == 0) continue;
            int wF = total - wB; if (wF == 0) break;
            sumB += i * hist[i];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double var = (double) wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) { maxVar = var; threshold = i; }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[] bin = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++)
            bin[i] = pixels[i] > threshold ? 255 : 0;
        out.getRaster().setPixels(0, 0, w, h, bin);
        return out;
    }
}
