package com.smarttools.invoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextToolsService {

    private final ObjectMapper objectMapper;

    public Map<String, String> base64Encode(String text) {
        String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return Map.of("result", encoded);
    }

    public Map<String, String> base64Decode(String text) {
        try {
            // Trim whitespace and newlines for decoding safety
            byte[] decodedBytes = Base64.getDecoder().decode(text.trim());
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            return Map.of("result", decoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 format: " + e.getMessage());
        }
    }

    public Map<String, String> jsonFormatter(String text) {
        try {
            Object json = objectMapper.readValue(text, Object.class);
            String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            return Map.of("result", formattedJson);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                // Strip Jackson's long source location details to keep it clean and user-friendly
                int idx = msg.indexOf(" at [Source:");
                if (idx != -1) {
                    msg = msg.substring(0, idx);
                }
                idx = msg.indexOf("\n at [Source:");
                if (idx != -1) {
                    msg = msg.substring(0, idx);
                }
            } else {
                msg = "Malformed JSON input";
            }
            throw new IllegalArgumentException("Invalid JSON: " + msg.trim());
        }
    }

    public Map<String, String> xmlFormatter(String text) {
        try {
            Source xmlInput = new StreamSource(new StringReader(text.trim()));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            transformer.transform(xmlInput, xmlOutput);
            String formattedXml = xmlOutput.getWriter().toString();
            return Map.of("result", formattedXml);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                // Keep only SAX/Parser error message part
                int idx = msg.indexOf("org.xml.sax.");
                if (idx != -1) {
                    msg = msg.substring(idx);
                }
            } else {
                msg = "Malformed XML input";
            }
            throw new IllegalArgumentException("Invalid XML: " + msg.trim());
        }
    }

    public Map<String, String> hashGenerator(String text) {
        String md5 = DigestUtils.md5Hex(text);
        String sha1 = DigestUtils.sha1Hex(text);
        String sha256 = DigestUtils.sha256Hex(text);
        String sha512 = DigestUtils.sha512Hex(text);

        String result = "MD5:\n" + md5 + "\n\n" +
                        "SHA-1:\n" + sha1 + "\n\n" +
                        "SHA-256:\n" + sha256 + "\n\n" +
                        "SHA-512:\n" + sha512;

        return Map.of("result", result);
    }
}
