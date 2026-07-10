package com.smarttools.invoice.service;

import com.smarttools.invoice.entity.Conversion;
import com.smarttools.invoice.entity.ConversionStatus;
import com.smarttools.invoice.entity.User;
import com.smarttools.invoice.repository.ConversionRepository;
import com.smarttools.invoice.repository.UserRepository;
import com.smarttools.invoice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversionTrackerService {

    private final ConversionRepository conversionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Conversion startConversion(UserPrincipal userPrincipal, String tool, String inputFilename, Long fileSizeBytes) {
        try {
            User user = null;
            if (userPrincipal != null) {
                user = userRepository.findById(userPrincipal.getId()).orElse(null);
            }

            Conversion conversion = Conversion.builder()
                    .user(user)
                    .tool(tool)
                    .status(ConversionStatus.PROCESSING)
                    .inputFilename(truncate(inputFilename, 495))
                    .fileSizeBytes(fileSizeBytes)
                    .expiresAt(OffsetDateTime.now().plusDays(1)) // Files auto-expire in 24 hours
                    .build();

            return conversionRepository.save(conversion);
        } catch (Exception e) {
            // Tracking failure must NEVER block the actual conversion — log and return unsaved stub
            log.warn("[ConversionTracker] Could not persist conversion start record for tool [{}]: {}", tool, e.getMessage());
            return Conversion.builder()
                    .tool(tool)
                    .status(ConversionStatus.PROCESSING)
                    .inputFilename(truncate(inputFilename, 495))
                    .fileSizeBytes(fileSizeBytes)
                    .build();
        }
    }

    @Transactional
    public void completeConversion(Conversion conversion, String outputFilename, long processingTimeMs) {
        try {
            conversion.setStatus(ConversionStatus.DONE);
            conversion.setOutputFilename(truncate(outputFilename, 495));
            conversion.setProcessingTimeMs(processingTimeMs);
            conversionRepository.save(conversion);
            log.info("Conversion [{}] for tool [{}] completed successfully in {}ms", conversion.getId(), conversion.getTool(), processingTimeMs);
        } catch (Exception e) {
            log.warn("[ConversionTracker] Could not persist conversion completion for tool [{}]: {}", conversion.getTool(), e.getMessage());
        }
    }

    @Transactional
    public void failConversion(Conversion conversion, String errorMessage, long processingTimeMs) {
        try {
            conversion.setStatus(ConversionStatus.FAILED);
            conversion.setErrorMessage(errorMessage);
            conversion.setProcessingTimeMs(processingTimeMs);
            conversionRepository.save(conversion);
            log.error("Conversion [{}] for tool [{}] failed: {} after {}ms", conversion.getId(), conversion.getTool(), errorMessage, processingTimeMs);
        } catch (Exception e) {
            log.warn("[ConversionTracker] Could not persist conversion failure for tool [{}]: {}", conversion.getTool(), e.getMessage());
        }
    }

    private String truncate(String val, int maxLen) {
        if (val == null) {
            return null;
        }
        if (val.length() <= maxLen) {
            return val;
        }
        return val.substring(0, maxLen - 3) + "...";
    }
}
