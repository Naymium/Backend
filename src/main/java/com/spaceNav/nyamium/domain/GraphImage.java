package com.spaceNav.nyamium.domain;

import com.spaceNav.nyamium.domain.common.BaseEntity;
import com.spaceNav.nyamium.domain.enums.Save;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String normalDataUrl;
    private String abnormalDataUrl;

    private Long normalDataNum;
    private Long abnormalDataNum;
    private Save save; // 그래프 저장 여부(디비에 저장 후 저장 안하면 삭제하기 위함)

    public void changeSaveStatus(Save save) {
        this.save = save;
    }
}
