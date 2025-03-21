package com.spaceNav.nyamium.domain;

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
public class OptionalValues {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "data_id", nullable = false)
    private Data data; // Data 엔터티와 양방향 매핑

    private Float rangingError;
    private Float delta;
    private Float fD;
    private Float sigma;
}
