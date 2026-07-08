package com.smarttools.invoice.controller;

import com.smarttools.invoice.entity.Conversion;
import com.smarttools.invoice.security.UserPrincipal;
import com.smarttools.invoice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
@Slf4j
public class ToolsController {

    private final ConversionTrackerService trackerService;
    private final TextToolsService textToolsService;
    private final ImageToolsService imageToolsService;
    private final ArchiveToolsService archiveToolsService;
    private final PdfToolsService pdfToolsService;
    private final OfficeToolsService officeToolsService;

    // Helper for Text Conversions
    private ResponseEntity<?> processTextTool(
            UserPrincipal userPrincipal,
            String toolName,
            String text,
            TextToolExecutor executor) {

        long startTime = System.currentTimeMillis();
        Conversion conversion = trackerService.startConversion(
            userPrincipal,
            toolName,
            "text_input",
            (long) text.length()
        );

        try {
            Map<String, String> result = executor.execute(text);
            long duration = System.currentTimeMillis() - startTime;
            trackerService.completeConversion(conversion, "text_output", duration);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Internal error processing text: " + e.getMessage()));
        }
    }

    // Helper for Single File Conversions
    private ResponseEntity<?> processSingleFileTool(
            UserPrincipal userPrincipal,
            String toolName,
            MultipartFile file,
            String targetFilename,
            String contentType,
            FileToolExecutor executor) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded."));
        }

        long startTime = System.currentTimeMillis();
        Conversion conversion = trackerService.startConversion(
            userPrincipal,
            toolName,
            file.getOriginalFilename(),
            file.getSize()
        );

        try {
            byte[] outputBytes = executor.execute(file);
            long duration = System.currentTimeMillis() - startTime;
            trackerService.completeConversion(conversion, targetFilename, duration);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + targetFilename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(outputBytes);
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "File processing failed: " + e.getMessage()));
        }
    }

    // Helper for Multiple File Conversions
    private ResponseEntity<?> processMultipleFilesTool(
            UserPrincipal userPrincipal,
            String toolName,
            MultipartFile[] files,
            String targetFilename,
            String contentType,
            MultiFileToolExecutor executor) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "No files uploaded."));
        }

        long totalSize = 0;
        StringBuilder originalNames = new StringBuilder();
        for (MultipartFile file : files) {
            totalSize += file.getSize();
            originalNames.append(file.getOriginalFilename()).append(", ");
        }
        String inputNamesStr = originalNames.toString();
        if (inputNamesStr.endsWith(", ")) {
            inputNamesStr = inputNamesStr.substring(0, inputNamesStr.length() - 2);
        }

        long startTime = System.currentTimeMillis();
        Conversion conversion = trackerService.startConversion(
            userPrincipal,
            toolName,
            inputNamesStr,
            totalSize
        );

        try {
            byte[] outputBytes = executor.execute(files);
            long duration = System.currentTimeMillis() - startTime;
            trackerService.completeConversion(conversion, targetFilename, duration);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + targetFilename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(outputBytes);
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "File processing failed: " + e.getMessage()));
        }
    }

    // Functional interfaces for executors
    @FunctionalInterface
    interface TextToolExecutor {
        Map<String, String> execute(String text) throws Exception;
    }

    @FunctionalInterface
    interface FileToolExecutor {
        byte[] execute(MultipartFile file) throws Exception;
    }

    @FunctionalInterface
    interface MultiFileToolExecutor {
        byte[] execute(MultipartFile[] files) throws Exception;
    }

    // PDF Tools
    @PostMapping("/pdf/merge")
    public ResponseEntity<?> pdfMerge(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("files") MultipartFile[] files) {
        return processMultipleFilesTool(user, "pdf-merge", files, "merged.pdf", "application/pdf",
                pdfToolsService::mergePdfs);
    }

    @PostMapping("/pdf/split")
    public ResponseEntity<?> pdfSplit(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "pdf-split", file, "split-pages.zip", "application/zip",
                pdfToolsService::splitPdf);
    }

    @PostMapping("/pdf/compress")
    public ResponseEntity<?> pdfCompress(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "pdf-compress", file, "compressed.pdf", "application/pdf",
                pdfToolsService::compressPdf);
    }

    @PostMapping("/pdf/to-images")
    public ResponseEntity<?> pdfToImages(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "pdf-to-images", file, "pdf-images.zip", "application/zip",
                pdfToolsService::pdfToImages);
    }

    @PostMapping("/images/to-pdf")
    public ResponseEntity<?> imagesToPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("files") MultipartFile[] files) {
        return processMultipleFilesTool(user, "images-to-pdf", files, "converted.pdf", "application/pdf",
                pdfToolsService::imagesToPdf);
    }

    // Image Tools
    @PostMapping("/image/jpg-to-png")
    public ResponseEntity<?> jpgToPng(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "jpg-to-png", file, "converted.png", "image/png",
                f -> imageToolsService.jpgToPng(f.getBytes()));
    }

    @PostMapping("/image/png-to-jpg")
    public ResponseEntity<?> pngToJpg(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "png-to-jpg", file, "converted.jpg", "image/jpeg",
                f -> imageToolsService.pngToJpg(f.getBytes()));
    }

    @PostMapping("/image/webp-converter")
    public ResponseEntity<?> webpConverter(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        boolean isWebp = originalName != null && originalName.toLowerCase().endsWith(".webp");
        String ext = isWebp ? "jpg" : "webp";
        String mime = isWebp ? "image/jpeg" : "image/webp";
        return processSingleFileTool(user, "webp-converter", file, "converted." + ext, mime,
                f -> isWebp ? imageToolsService.pngToJpg(f.getBytes()) : imageToolsService.convertToWebp(f.getBytes()));
    }

    @PostMapping("/image/compress")
    public ResponseEntity<?> imageCompress(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        String format = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().contains("png") ? "png" : "jpg";
        return processSingleFileTool(user, "image-compress", file, "compressed." + format, "image/" + format,
                f -> imageToolsService.compressImage(f.getBytes(), format));
    }

    @PostMapping("/image/resize")
    public ResponseEntity<?> imageResize(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        String format = file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().contains("png") ? "png" : "jpg";
        return processSingleFileTool(user, "image-resize", file, "resized." + format, "image/" + format,
                f -> imageToolsService.resizeImage(f.getBytes(), format));
    }

    // Office Tools
    @PostMapping("/office/excel-to-pdf")
    public ResponseEntity<?> excelToPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "excel-to-pdf", file, "excel-converted.pdf", "application/pdf",
                officeToolsService::excelToPdf);
    }

    @PostMapping("/office/word-to-pdf")
    public ResponseEntity<?> wordToPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "word-to-pdf", file, "word-converted.pdf", "application/pdf",
                officeToolsService::wordToPdf);
    }

    @PostMapping("/office/ppt-to-pdf")
    public ResponseEntity<?> pptToPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        return processSingleFileTool(user, "ppt-to-pdf", file, "ppt-converted.pdf", "application/pdf",
                officeToolsService::pptToPdf);
    }

    // Text Tools
    @PostMapping("/text/base64-encode")
    public ResponseEntity<?> base64Encode(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody String text) {
        return processTextTool(user, "base64-encode", text, textToolsService::base64Encode);
    }

    @PostMapping("/text/base64-decode")
    public ResponseEntity<?> base64Decode(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody String text) {
        return processTextTool(user, "base64-decode", text, textToolsService::base64Decode);
    }

    @PostMapping("/text/json-formatter")
    public ResponseEntity<?> jsonFormatter(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody String text) {
        return processTextTool(user, "json-formatter", text, textToolsService::jsonFormatter);
    }

    @PostMapping("/text/xml-formatter")
    public ResponseEntity<?> xmlFormatter(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody String text) {
        return processTextTool(user, "xml-formatter", text, textToolsService::xmlFormatter);
    }

    // Archive Tools
    @PostMapping("/archive/zip-extractor")
    public ResponseEntity<?> zipExtractor(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded."));
        }

        long startTime = System.currentTimeMillis();
        Conversion conversion = trackerService.startConversion(
            user,
            "zip-extractor",
            file.getOriginalFilename(),
            file.getSize()
        );

        try {
            ArchiveToolsService.ExtractedFileInfo info = new ArchiveToolsService.ExtractedFileInfo();
            byte[] outputBytes = archiveToolsService.extractZip(file, info);
            long duration = System.currentTimeMillis() - startTime;
            trackerService.completeConversion(conversion, info.filename, duration);

            String mimeType = info.filename.endsWith(".txt") ? "text/plain" : "application/octet-stream";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + info.filename + "\"")
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(outputBytes);
        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            trackerService.failConversion(conversion, e.getMessage(), duration);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "ZIP extraction failed: " + e.getMessage()));
        }
    }

    @PostMapping("/archive/zip-creator")
    public ResponseEntity<?> zipCreator(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("files") MultipartFile[] files) {
        return processMultipleFilesTool(user, "zip-creator", files, "archive.zip", "application/zip",
                archiveToolsService::createZip);
    }

    // Developer Tools
    @PostMapping("/dev/hash-generator")
    public ResponseEntity<?> hashGenerator(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody String text) {
        return processTextTool(user, "hash-generator", text, textToolsService::hashGenerator);
    }
}
