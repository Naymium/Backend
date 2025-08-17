package com.spaceNav.nyamium.web.controller;

import com.spaceNav.nyamium.apiPayLoad.ApiResponse;
import com.spaceNav.nyamium.converter.DataConverter;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.service.DataService;
import com.spaceNav.nyamium.web.dto.DataResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @Operation(summary = "전체 데이터 확인", description = "DB 내의 전체 데이터를 불러옵니다.")
    @GetMapping(value = "/get")
    public ApiResponse<DataResponseDTO> getAllData() {

        List<Data> dataList = dataService.getAllData();
        return ApiResponse.onSuccess(DataConverter.toDataResponseDTO(dataList));
    }

    @Operation(summary = "전체 데이터 파일 다운로드", description = "DB 내의 전체 데이터를 CSV 파일로 다운로드합니다.")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadDataCsv() throws Exception {
        byte[] csvData = dataService.downloadDataCSV();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data_export.csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csvData);
    }

    @Operation(summary = "데이터 삭제", description = "선택한 데이터를 삭제합니다.")
    @DeleteMapping("/{dataId}")
    public ApiResponse<DataResponseDTO.DeleteDataResponseDTO> deleteData(
            @PathVariable Long dataId) {

        Map<String, Object> result = dataService.deleteData(dataId);
        Data data = (Data) result.get("data");
        Long totalCount = (Long) result.get("count");

        return ApiResponse.onSuccess(DataConverter.toDeleteDataResponseDTO(data, totalCount));
    }
}
