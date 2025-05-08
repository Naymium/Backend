package com.spaceNav.nyamium.web.controller;

import com.spaceNav.nyamium.apiPayLoad.ApiResponse;
import com.spaceNav.nyamium.converter.PredictConverter;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.service.PredictService;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import com.spaceNav.nyamium.web.dto.PredictResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/predict")
@RequiredArgsConstructor
public class PredictController {

    private final PredictService predictService;

    @Operation(summary = "입력값 입력 후 결과 확인", description = "입력값을 입력 후 결과를 확인합니다.")
    @PostMapping(value = "/data")
    public ApiResponse<PredictResponseDTO.ResultResponseDTO> getDataPredictResult(
            @RequestBody PredictRequestDTO predictRequestDTO) {

        Data inputData = predictService.predictByData(predictRequestDTO);
        return ApiResponse.onSuccess(PredictConverter.toResultResponseDTO(inputData));
    }

    @Operation(summary = "파일 데이터 입력 후 결과 확인",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "multipart/form-data")),
            description = "파일 데이터의 입력값을 통해 결과를 확인합니다.")
    @PostMapping(value = "/file-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PredictResponseDTO.ResultResponseFileDTO> getFilePredictResult(
            @RequestPart(name = "file")
            MultipartFile file) {


        return null;
    }

    @Operation(summary = "예측 결과 파일로 내려받기", description = "예측 결과를 csv 파일로 다운로드 할 수 있습니다.")
    @GetMapping(value = "/download")
    public ResponseEntity<Resource> getPredictResultToCSV(
            @RequestParam("filename") String filename) {

        Resource file = predictService.getPredictionFile(filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @Operation(summary = "데이터 저장하기", description = "입력한 데이터를 DB 에 저장합니다. (NOT_SAVE 데이터를 SAVE 로 수정)")
    @PatchMapping(value = "/save")
    public ApiResponse<PredictResponseDTO.SaveResponseDTO> saveData() {

        List<Data> dataList = predictService.saveData();
        return ApiResponse.onSuccess(PredictConverter.toSaveResponseDTO(dataList));
    }

    @Operation(summary = "데이터 저장하지 않기", description = "save 가 NOT_SAVE 인 데이터를 삭제합니다.")
    @DeleteMapping(value = "/not-save")
    public ApiResponse<PredictResponseDTO.SaveResponseDTO> notSaveData() {

        List<Data> dataList = predictService.notSaveData();
        return ApiResponse.onSuccess(PredictConverter.toNotSaveResponseDTO(dataList));
    }
}
