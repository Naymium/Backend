package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.web.dto.DataResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class DataConverter {

    public static DataResponseDTO.GetOneDataResponseDTO toOneDataResponseDTO(Data data) {

        return DataResponseDTO.GetOneDataResponseDTO.builder()
                .id(data.getId())
                .e1(data.getE1())
                .e2(data.getE2())
                .e3(data.getE3())
                .e4(data.getE4())
                .l1(data.getL1())
                .l2(data.getL2())
                .l3(data.getL3())
                .l4(data.getL4())
                .prediction(data.getPrediction())
                .probability(data.getProbability())
                .rangingError(data.getOptionalValues() != null ? data.getOptionalValues().getRangingError() : null)
                .delta(data.getOptionalValues() != null ? data.getOptionalValues().getDelta() : null)
                .fD(data.getOptionalValues() != null ? data.getOptionalValues().getFD() : null)
                .sigma(data.getOptionalValues() != null ? data.getOptionalValues().getSigma() : null)
                .build();
    }

    public static DataResponseDTO toDataResponseDTO(List<Data> dataList) {
        return DataResponseDTO.builder()
                .getAllDataResponseDTO(dataList.stream()
                        .map(DataConverter::toOneDataResponseDTO)
                        .collect(Collectors.toList())).
                build();
    }

    public static DataResponseDTO.DeleteDataResponseDTO toDeleteDataResponseDTO(Data data, Long totalCount) {
        return DataResponseDTO.DeleteDataResponseDTO.builder()
                .id(data.getId())
                .dataNum(totalCount)
                .build();
    }
}
