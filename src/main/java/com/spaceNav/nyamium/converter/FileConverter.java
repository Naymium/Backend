package com.spaceNav.nyamium.converter;

import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.web.dto.FileResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class FileConverter {

    public static FileResponseDTO.GetOneFileResponseDTO toOneFileResponseDTO(FileData fileData) {

        return FileResponseDTO.GetOneFileResponseDTO.builder()
                .id(fileData.getId())
                .fileName(fileData.getFileName())
                .fileUrl(fileData.getFileUrl())
                .dataNum(fileData.getDataNum())
                .createdAt(fileData.getCreatedAt())
                .build();
    }

    public static FileResponseDTO toFileResponseDTO(List<FileData> fileDataList) {
        return FileResponseDTO.builder()
                .getAllFileResponseDTO(fileDataList.stream()
                        .map(FileConverter::toOneFileResponseDTO)
                        .collect(Collectors.toList())).
                build();
    }

    public static FileResponseDTO.DeleteFileResponseDTO toDeleteFileResponseDTO(FileData fileData) {
        return FileResponseDTO.DeleteFileResponseDTO.builder()
                .id(fileData.getId())
                .dataNum(fileData.getDataNum())
                .build();
    }
}
