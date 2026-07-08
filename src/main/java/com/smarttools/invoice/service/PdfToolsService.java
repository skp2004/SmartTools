package com.smarttools.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfToolsService {

    public byte[] mergePdfs(MultipartFile[] files) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        merger.setDestinationStream(baos);

        for (MultipartFile file : files) {
            merger.addSource(new RandomAccessReadBuffer(file.getInputStream()));
        }

        // PDFBox 3.x merge
        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());
        return baos.toByteArray();
    }

    public byte[] splitPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            Splitter splitter = new Splitter();
            List<PDDocument> pages = splitter.split(document);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                int count = 1;
                for (PDDocument pageDoc : pages) {
                    ZipEntry entry = new ZipEntry("page_" + count + ".pdf");
                    zos.putNextEntry(entry);
                    ByteArrayOutputStream pageBaos = new ByteArrayOutputStream();
                    pageDoc.save(pageBaos);
                    zos.write(pageBaos.toByteArray());
                    zos.closeEntry();
                    pageDoc.close();
                    count++;
                }
            }
            return baos.toByteArray();
        }
    }

    public byte[] compressPdf(MultipartFile file) throws IOException {
        // PDFBox 3.x automatically compresses streams during save.
        // Reloading and saving removes unused metadata and optimizes page resource streams.
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] pdfToImages(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
            PDFRenderer renderer = new PDFRenderer(document);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, 150); // Render at 150 DPI for web quality
                    ZipEntry entry = new ZipEntry("page_" + (i + 1) + ".png");
                    zos.putNextEntry(entry);

                    ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
                    ImageIO.write(image, "png", imgBaos);
                    zos.write(imgBaos.toByteArray());
                    zos.closeEntry();
                }
            }
            return baos.toByteArray();
        }
    }

    public byte[] imagesToPdf(MultipartFile[] files) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (MultipartFile file : files) {
                byte[] imgBytes = file.getBytes();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                if (img == null) {
                    log.warn("Skipping file {} as it could not be read as an image.", file.getOriginalFilename());
                    continue;
                }

                PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
                doc.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, img);
                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    contentStream.drawImage(pdImage, 0, 0);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
