package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.web.dto.GraphResponseDTO;

public class GraphConverter {

    public static GraphResponseDTO.ResultResponseDTO toNormalGraphResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.ResultResponseDTO.builder()
                .id(graphImage.getId())
                .imageUrl(graphImage.getNormalDataUrl())
                .dataNum(graphImage.getNormalDataNum())
                .createdAt(graphImage.getCreatedAt())
                .build();
    }

    public static GraphResponseDTO.ResultResponseDTO toAbnormalGraphResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.ResultResponseDTO.builder()
                .id(graphImage.getId())
                .imageUrl(graphImage.getAbnormalDataUrl())
                .dataNum(graphImage.getAbnormalDataNum())
                .createdAt(graphImage.getCreatedAt())
                .build();
    }

    public static GraphResponseDTO.SaveResponseDTO toSaveResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.SaveResponseDTO.builder()
                .id(graphImage.getId())
                .message("현재 Normal/Abnormal 데이터 그래프를 저장하였습니다.")
                .build();
    }
}
