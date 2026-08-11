package com.sentinelcore.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, APPLIED, FAILED

    private String targetOs;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    private LocalDateTime releaseDate;

    private LocalDateTime appliedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.releaseDate == null) {
            this.releaseDate = LocalDateTime.now();
        }
    }
}
