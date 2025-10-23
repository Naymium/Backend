package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.web.dto.GraphResponseDTO;
import org.hibernate.graph.Graph;

import java.util.Base64;

public class GraphConverter {

    public static GraphResponseDTO.GraphResultResponseDTO toGraphResultResponseDTO(GraphImage graphImage) {

        return GraphResponseDTO.GraphResultResponseDTO.builder()
                .id(graphImage.getId())
                .normalDataUrl(graphImage.getNormalDataUrl())
                .abnormalDataUrl(graphImage.getAbnormalDataUrl())
                .normalDataKeyName(graphImage.getNormalDataKeyName())
                .abnormalDataKeyName(graphImage.getAbnormalDataKeyName())
                .normalDataNum(graphImage.getNormalDataNum())
                .abnormalDataNum(graphImage.getAbnormalDataNum())
                .build();
    }
}
