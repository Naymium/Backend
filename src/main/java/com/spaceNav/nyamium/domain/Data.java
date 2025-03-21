package com.spaceNav.nyamium.domain;

import com.spaceNav.nyamium.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Data extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Float e1;
    private Float e2;
    private Float e3;
    private Float e4;
    private Float l1;
    private Float l2;
    private Float l3;
    private Float l4;

    private Boolean prediction;
    private Float probability;

    // OptionalValues와 양방향 매핑
    @OneToOne(mappedBy = "data", cascade = CascadeType.ALL)
    private OptionalValues optionalValues;

    // AbnormalImage와 양방향 매핑
    @OneToOne(mappedBy = "data", cascade = CascadeType.ALL)
    private AbnormalImage abnormalImage;
}
