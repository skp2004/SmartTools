package com.smarttools.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageToolsService {

    public byte[] jpgToPng(byte[] inputBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (image == null) {
            throw new IllegalArgumentException("Invalid JPEG image file");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    public byte[] pngToJpg(byte[] inputBytes) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid PNG image file");
        }
        
        // Remove transparency for JPEG format
        BufferedImage newImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = newImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, originalImage.getWidth(), originalImage.getHeight());
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(newImage, "jpg", baos);
        return baos.toByteArray();
    }

    public byte[] convertToWebp(byte[] inputBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (image == null) {
            throw new IllegalArgumentException("Invalid source image file");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "webp", baos);
        return baos.toByteArray();
    }

    public byte[] compressImage(byte[] inputBytes, String format) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid source image file");
        }
        
        String targetFormat = format.toLowerCase().contains("png") ? "png" : 
                             (format.toLowerCase().contains("webp") ? "webp" : "jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        if ("png".equals(targetFormat)) {
            // PNG does not support lossy compression, scale to 90% or rewrite using scale
            Thumbnails.of(originalImage)
                    .scale(1.0)
                    .toOutputStream(baos);
        } else {
            // Lossy compression for JPG and WEBP
            Thumbnails.of(originalImage)
                    .scale(1.0)
                    .outputQuality(0.75)
                    .outputFormat(targetFormat)
                    .toOutputStream(baos);
        }
        return baos.toByteArray();
    }

    public byte[] resizeImage(byte[] inputBytes, String format) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
        if (originalImage == null) {
            throw new IllegalArgumentException("Invalid source image file");
        }
        
        String targetFormat = format.toLowerCase().contains("png") ? "png" : 
                             (format.toLowerCase().contains("webp") ? "webp" : "jpg");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(originalImage)
                .scale(0.5) // resize to 50% scale
                .outputFormat(targetFormat)
                .toOutputStream(baos);
        
        return baos.toByteArray();
    }
}
