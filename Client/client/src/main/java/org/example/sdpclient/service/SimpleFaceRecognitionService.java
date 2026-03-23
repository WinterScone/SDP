package org.example.sdpclient.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class SimpleFaceRecognitionService implements FaceRecognitionService {

    private static final int SIZE = 100;
    private static final double MATCH_THRESHOLD = 0.92;

    @Override
    public byte[] prepareFaceData(byte[] imageBytes) {
        try {
            BufferedImage input = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (input == null) {
                throw new IllegalArgumentException("Invalid image file");
            }

            BufferedImage resized = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = resized.createGraphics();
            g.drawImage(input, 0, 0, SIZE, SIZE, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not process image");
        }
    }

    @Override
    public boolean verify(byte[] enrolledFaceData, byte[] candidateImageBytes) {
        try {
            BufferedImage enrolled = ImageIO.read(new ByteArrayInputStream(enrolledFaceData));
            BufferedImage candidateOriginal = ImageIO.read(new ByteArrayInputStream(candidateImageBytes));

            if (enrolled == null || candidateOriginal == null) {
                return false;
            }

            BufferedImage candidate = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = candidate.createGraphics();
            g.drawImage(candidateOriginal, 0, 0, SIZE, SIZE, null);
            g.dispose();

            double similarity = compareImages(enrolled, candidate);
            return similarity >= MATCH_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    private double compareImages(BufferedImage img1, BufferedImage img2) {
        long diff = 0;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int rgb1 = img1.getRGB(x, y) & 0xFF;
                int rgb2 = img2.getRGB(x, y) & 0xFF;
                diff += Math.abs(rgb1 - rgb2);
            }
        }

        double maxDiff = (double) SIZE * SIZE * 255;
        return 1.0 - (diff / maxDiff);
    }
}