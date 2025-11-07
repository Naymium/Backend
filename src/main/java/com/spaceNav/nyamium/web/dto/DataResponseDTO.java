package com.spaceNav.nyamium.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spaceNav.nyamium.domain.enums.Prediction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DataResponseDTO {

    List<GetOneDataResponseDTO> getAllDataResponseDTO;

    /* 1개의 데이터 */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class GetOneDataResponseDTO{
        private Long id;

        private Float e1, e2, e3, e4;
        private Float l1, l2, l3, l4;

        private Prediction prediction;
        private Float probability;

        private Float rangingError;
        private Float delta;
        private Float fd;
        private Float sigma;

        private LocalDateTime createdAt;
    }

    /* 데이터 삭제 */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class DeleteDataResponseDTO{
        private Long id;
        private Long dataNum;
    }
}
