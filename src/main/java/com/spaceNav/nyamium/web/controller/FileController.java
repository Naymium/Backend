package com.spaceNav.nyamium.web.controller;

import com.spaceNav.nyamium.apiPayLoad.ApiResponse;
import com.spaceNav.nyamium.converter.FileConverter;
import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.service.FileService;
import com.spaceNav.nyamium.web.dto.FileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "전체 파일 데이터 확인", description = "전체 파일 데이터를 불러옵니다.")
    @GetMapping(value = "/get")
    public ApiResponse<FileResponseDTO> getAllFile() {

        List<FileData> fileDataList = fileService.getAllFile();

        return ApiResponse.onSuccess(FileConverter.toFileResponseDTO(fileDataList));
    }

    @Operation(summary = "파일 삭제", description = "선택한 파일을 삭제합니다.")
    @DeleteMapping("/{fileId}")
    public ApiResponse<FileResponseDTO.DeleteFileResponseDTO> deleteFile(
            @PathVariable Long fileId) {

        FileData fileData = fileService.deleteFile(fileId);

        return ApiResponse.onSuccess(FileConverter.toDeleteFileResponseDTO(fileData));
    }
}
