package com.smarttools.invoice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "usage", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "tool", "date"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Usage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String tool;

    @Column(nullable = false)
    private LocalDate date;

    @Builder.Default
    @Column(nullable = false)
    private Integer count = 1;
}
