package com.spaceNav.nyamium.domain;

import com.spaceNav.nyamium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbnormalImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "data_id", nullable = false)
    private Data data; // Data 엔터티와 양방향 매핑

    private String imageUrl;
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
