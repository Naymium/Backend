package com.spaceNav.nyamium.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class FileResponseDTO {

    List<FileResponseDTO.GetOneFileResponseDTO> getAllFileResponseDTO;

    /* 1개의 데이터 */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class GetOneFileResponseDTO{
        private Long id;

        private String fileName;
        private String fileUrl;
        private Long dataNum;
        private LocalDateTime createdAt;
    }

    /* 데이터 삭제 */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class DeleteFileResponseDTO{
        private Long id;
        private Long dataNum;
    }
}
