package com.spaceNav.nyamium.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.domain.enums.Prediction;
import com.spaceNav.nyamium.domain.enums.Save;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class PredictResponseDTO {

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ResultResponseListDTO{
        List<ResultResponseDTO> resultList;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ResultResponseDTO{
        private Long id;
        private Float e1;
        private Float e2;
        private Float e3;
        private Float e4;
        private Float l1;
        private Float l2;
        private Float l3;
        private Float l4;

        private Float rangingError;
        private Float delta;
        private Float fd;
        private Float sigma;

        private Prediction prediction;
        private Float probability;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ResultResponseFileDTO{
        private ResultResponseListDTO results;
        private Long dataNum;
        private String fileName;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ForResultResponseFileDTO{
        private List<Data> dataList;
        private FileData fileData;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class SaveResponseDTO{
        private List<Long> id;
        private String message; //저장 관련 메시지
    }
}
