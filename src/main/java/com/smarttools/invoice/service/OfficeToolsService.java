package com.smarttools.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfficeToolsService {

    private static class RenderState {
        PDDocument document;
        PDPage currentPage;
        PDPageContentStream contentStream;
        float yPosition;
        float margin = 50;
        float width;
        float height;
        PDType1Font fontRegular;
        PDType1Font fontBold;

        RenderState(PDDocument document, boolean landscape) throws IOException {
            this.document = document;
            PDRectangle rect = landscape ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()) : PDRectangle.A4;
            this.currentPage = new PDPage(rect);
            this.document.addPage(this.currentPage);
            this.contentStream = new PDPageContentStream(document, this.currentPage);
            this.width = rect.getWidth();
            this.height = rect.getHeight();
            this.yPosition = this.height - margin;
            
            // Standard fonts initialization in PDFBox 3.x
            this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }

        void checkPageBreak(float neededHeight) throws IOException {
            if (this.yPosition - neededHeight < margin) {
                this.contentStream.close();
                PDRectangle rect = this.currentPage.getMediaBox();
                this.currentPage = new PDPage(rect);
                this.document.addPage(this.currentPage);
                this.contentStream = new PDPageContentStream(this.document, this.currentPage);
                this.yPosition = this.height - margin;
            }
        }

        void drawText(String text, float fontSize, boolean bold) throws IOException {
            if (text == null || text.trim().isEmpty()) {
                yPosition -= fontSize * 0.5f; // Small spacing for empty lines
                return;
            }

            PDType1Font font = bold ? fontBold : fontRegular;
            float leading = 1.4f * fontSize;
            List<String> lines = wrapText(text, width - 2 * margin, font, fontSize);

            for (String line : lines) {
                checkPageBreak(leading);
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(cleanText(line));
                contentStream.endText();
                yPosition -= leading;
            }
        }

        private String cleanText(String text) {
            if (text == null) return "";
            return text.replace("“", "\"")
                       .replace("”", "\"")
                       .replace("‘", "'")
                       .replace("’", "'")
                       .replace("–", "-")
                       .replace("—", "-")
                       .replaceAll("[^\\p{Print}]", " ")
                       .replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        }

        private List<String> wrapText(String text, float maxWidth, PDType1Font font, float fontSize) throws IOException {
            List<String> result = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) continue;
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                float lineWidth = font.getStringWidth(cleanText(testLine)) / 1000f * fontSize;
                if (lineWidth > maxWidth) {
                    if (currentLine.length() > 0) {
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        result.add(word);
                    }
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
            if (currentLine.length() > 0) {
                result.add(currentLine.toString());
            }
            return result;
        }

        void close() throws IOException {
            if (this.contentStream != null) {
                this.contentStream.close();
            }
        }
    }

    public byte[] wordToPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = new PDDocument();
             XWPFDocument document = new XWPFDocument(file.getInputStream())) {

            RenderState state = new RenderState(doc, false);

            for (XWPFParagraph para : document.getParagraphs()) {
                String text = para.getText();
                boolean isHeading = para.getStyle() != null && para.getStyle().toLowerCase().contains("heading");
                float fontSize = isHeading ? 16 : 11;
                state.drawText(text, fontSize, isHeading);
            }
            state.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] excelToPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = new PDDocument();
             XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                XSSFSheet sheet = workbook.getSheetAt(s);
                RenderState state = new RenderState(doc, false);

                state.drawText("Sheet: " + sheet.getSheetName(), 18, true);
                state.yPosition -= 15;

                for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                    XSSFRow row = sheet.getRow(r);
                    if (row == null) continue;

                    StringBuilder rowText = new StringBuilder();
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        XSSFCell cell = row.getCell(c);
                        if (cell == null) continue;

                        String cellVal = cell.toString().trim();
                        if (!cellVal.isEmpty()) {
                            rowText.append(cellVal).append("   |   ");
                        }
                    }
                    if (rowText.length() > 0) {
                        String cleanRow = rowText.toString();
                        if (cleanRow.endsWith("   |   ")) {
                            cleanRow = cleanRow.substring(0, cleanRow.length() - 7);
                        }
                        state.drawText(cleanRow, 10, false);
                    }
                }
                state.close();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] pptToPdf(MultipartFile file) throws IOException {
        try (PDDocument doc = new PDDocument();
             XMLSlideShow slideShow = new XMLSlideShow(file.getInputStream())) {

            for (XSLFSlide slide : slideShow.getSlides()) {
                RenderState state = new RenderState(doc, true);

                String slideTitle = slide.getTitle();
                if (slideTitle != null && !slideTitle.isEmpty()) {
                    state.drawText("Slide " + slide.getSlideNumber() + ": " + slideTitle, 18, true);
                    state.yPosition -= 20;
                } else {
                    state.drawText("Slide " + slide.getSlideNumber(), 18, true);
                    state.yPosition -= 20;
                }

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty() && !text.equals(slideTitle)) {
                            state.drawText(text, 12, false);
                            state.yPosition -= 10;
                        }
                    }
                }
                state.close();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }
}
