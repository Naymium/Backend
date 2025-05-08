package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.OptionalValues;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import com.spaceNav.nyamium.web.dto.PredictResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class PredictConverter {

    public static PredictResponseDTO.ResultResponseDTO toResultResponseDTO(Data data){

        return PredictResponseDTO.ResultResponseDTO.builder()
                .id(data.getId())
                .prediction(data.getPrediction())
                .probability(data.getProbability())
                .rangingError(data.getOptionalValues().getRangingError())
                .build();
    }

    public static PredictResponseDTO.SaveResponseDTO toSaveResponseDTO(List<Data> dataList){

        List<Long> idList = dataList.stream()
                .map(Data::getId)
                .collect(Collectors.toList());

        return PredictResponseDTO.SaveResponseDTO.builder()
                .id(idList)
                .message("해당 데이터들 DB에 저장 완료")
                .build();
    }

    public static PredictResponseDTO.SaveResponseDTO toNotSaveResponseDTO(List<Data> dataList) {

        List<Long> idList = dataList.stream()
                .map(Data::getId)
                .collect(Collectors.toList());

        return PredictResponseDTO.SaveResponseDTO.builder()
                .id(idList)
                .message("해당 데이터들을 DB에 저장 안함")
                .build();
    }
}
