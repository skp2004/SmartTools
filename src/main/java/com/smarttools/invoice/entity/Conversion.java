package com.smarttools.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * One record per tool invocation.
 * user_id is nullable so guests can use tools without registering.
 */
@Entity
@Table(name = "conversions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable — guests don't have an account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;

    /** Tool identifier, e.g. "pdf-merge", "image-compress", "hash-generator". */
    @Column(nullable = false, length = 50)
    private String tool;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ConversionStatus status = ConversionStatus.PENDING;

    @Column(name = "input_filename", length = 500)
    private String inputFilename;

    @Column(name = "output_filename", length = 500)
    private String outputFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Temporary output files are deleted after this timestamp. */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
