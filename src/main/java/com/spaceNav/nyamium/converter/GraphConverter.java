package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.web.dto.GraphResponseDTO;
import org.hibernate.graph.Graph;

import java.util.Base64;

public class GraphConverter {

    public static GraphResponseDTO.GraphResultResponseDTO toNormalGraphResultResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.GraphResultResponseDTO.builder()
                .id(graphImage.getId())
                .imageUrl(graphImage.getNormalDataUrl())
                .imageKeyName(graphImage.getNormalDataKeyName())
                .dataCount(graphImage.getNormalDataNum())
                .build();
    }

    public static GraphResponseDTO.GraphResultResponseDTO toAbnormalGraphResultResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.GraphResultResponseDTO.builder()
                .id(graphImage.getId())
                .imageUrl(graphImage.getAbnormalDataUrl())
                .imageKeyName(graphImage.getAbnormalDataKeyName())
                .dataCount(graphImage.getAbnormalDataNum())
                .build();
    }

    public static GraphResponseDTO.SaveResponseDTO toSaveResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.SaveResponseDTO.builder()
                .id(graphImage.getId())
                .message("현재 Normal/Abnormal 데이터 그래프를 저장하였습니다.")
                .build();
    }
}
