package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.domain.OptionalValues;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.web.dto.DataResponseDTO;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import com.spaceNav.nyamium.web.dto.PredictResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class PredictConverter {

    public static PredictResponseDTO.ResultResponseDTO toResultResponseDTO(Data data){

        return PredictResponseDTO.ResultResponseDTO.builder()
                .id(data.getId())
                .e1(data.getE1())
                .e2(data.getE2())
                .e3(data.getE3())
                .e4(data.getE4())
                .l1(data.getL1())
                .l2(data.getL2())
                .l3(data.getL3())
                .l4(data.getL4())

                .rangingError(data.getOptionalValues().getRangingError())
                .delta(data.getOptionalValues().getDelta())
                .fd(data.getOptionalValues().getFd())
                .sigma(data.getOptionalValues().getSigma())
                .prediction(data.getPrediction())
                .probability(data.getProbability())
                .build();
    }

    public static PredictResponseDTO.ResultResponseFileDTO toResultResponseFileDTO(PredictResponseDTO.ForResultResponseFileDTO requestDTO){
        PredictResponseDTO.ResultResponseListDTO resultList = PredictResponseDTO.ResultResponseListDTO.builder()
                .resultList(requestDTO.getDataList().stream()
                        .map(PredictConverter::toResultResponseDTO)
                        .collect(Collectors.toList())).
                build();

        return PredictResponseDTO.ResultResponseFileDTO.builder()
                .results(resultList)
                .dataNum(requestDTO.getFileData().getDataNum())
                .fileName(requestDTO.getFileData().getFileName())
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

    public static PredictResponseDTO.ForResultResponseFileDTO toForResultResponseFileDTO(List<Data> dataList, FileData fileData){
        return PredictResponseDTO.ForResultResponseFileDTO.builder()
                .dataList(dataList)
                .fileData(fileData)
                .build();
    }
}
