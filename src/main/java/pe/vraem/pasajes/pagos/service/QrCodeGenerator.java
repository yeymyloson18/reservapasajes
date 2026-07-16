package pe.vraem.pasajes.pagos.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Genera codigos QR simples como imagen PNG en base64, sin depender de un
 * servicio externo (no hay integracion real con la API de Yape/Plin).
 */
@Service
public class QrCodeGenerator {

    private static final int TAMANO_PX = 220;

    public String generarPngBase64(String contenido) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matriz = writer.encode(contenido, BarcodeFormat.QR_CODE, TAMANO_PX, TAMANO_PX);

            BufferedImage imagen = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matriz.getWidth(); x++) {
                for (int y = 0; y < matriz.getHeight(); y++) {
                    imagen.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return Base64.getEncoder().encodeToString(salida.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el codigo QR", ex);
        }
    }
}
