package com.spaceNav.nyamium.domain;

import com.spaceNav.nyamium.domain.common.BaseEntity;
import com.spaceNav.nyamium.domain.enums.Prediction;
import com.spaceNav.nyamium.domain.enums.Save;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prediction prediction;

    private Float probability;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Save save; // 데이터 저장 여부(디비에 저장 후 저장 안하면 삭제하기 위함)

    // FileData 와 양방향 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileData fileData;

    // OptionalValues와 양방향 매핑
    @OneToOne(mappedBy = "data", cascade = CascadeType.ALL)
    private OptionalValues optionalValues;

    /* 추가 로직 */

    // Data 에 OptionalValues 매핑
    public void addOptionalValues(OptionalValues optionalValues) {
        this.optionalValues = optionalValues;
    }
    // Save Status 수정
    public void changeSaveStatus(Save save) {
        this.save = save;
    }
}

