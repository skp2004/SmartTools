package com.smarttools.invoice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveToolsService {

    public static class ExtractedFileInfo {
        public String filename;
    }

    private static class ExtractedFile {
        String name;
        byte[] content;

        ExtractedFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }
    }

    public byte[] createZip(MultipartFile[] files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (MultipartFile file : files) {
                String filename = file.getOriginalFilename();
                if (filename == null || filename.isEmpty()) {
                    filename = "file_" + System.currentTimeMillis();
                }
                ZipEntry zipEntry = new ZipEntry(filename);
                zos.putNextEntry(zipEntry);
                zos.write(file.getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    public byte[] extractZip(MultipartFile file, ExtractedFileInfo outInfo) throws IOException {
        List<ExtractedFile> extractedFiles = new ArrayList<>();
        byte[] buffer = new byte[4096];

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(file.getBytes()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fileContent.write(buffer, 0, len);
                }
                extractedFiles.add(new ExtractedFile(entry.getName(), fileContent.toByteArray()));
                zis.closeEntry();
            }
        }

        if (extractedFiles.isEmpty()) {
            throw new IllegalArgumentException("The ZIP archive is empty or invalid.");
        }

        if (extractedFiles.size() == 1) {
            ExtractedFile single = extractedFiles.get(0);
            outInfo.filename = single.name;
            return single.content;
        } else {
            // Return a summary of the ZIP contents
            StringBuilder sb = new StringBuilder();
            sb.append("ZIP EXTRACTOR SUMMARY\n");
            sb.append("=====================\n\n");
            sb.append(String.format("%-60s %s\n", "File Path", "Size (bytes)"));
            sb.append(String.format("%-60s %s\n", "---------", "------------"));
            for (ExtractedFile ef : extractedFiles) {
                sb.append(String.format("%-60s %d\n", ef.name, ef.content.length));
            }
            outInfo.filename = "extracted-files-list.txt";
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}
