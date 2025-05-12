package com.spaceNav.nyamium.web.controller;

import com.spaceNav.nyamium.apiPayLoad.ApiResponse;
import com.spaceNav.nyamium.converter.GraphConverter;
import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.service.GraphService;
import com.spaceNav.nyamium.web.dto.GraphResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    @Operation(summary = "정상 데이터 그래프 확인", description = "S3 로부터 현재 데이터(최신)를 이용한 정상 데이터 그래프를 가져옵니다.")
    @GetMapping(value = "/normal")
    public ApiResponse<GraphResponseDTO.ResultResponseDTO> getNormalGraph() {

        GraphImage graphImage = graphService.getRecentGraph();
        return ApiResponse.onSuccess(GraphConverter.toNormalGraphResponseDTO(graphImage));
    }

    @Operation(summary = "비정상 데이터 그래프 확인", description = "S3 로부터 현재 데이터(최신)를 이용한 비정상 데이터 그래프를 가져옵니다.")
    @GetMapping(value = "/abnormal")
    public ApiResponse<GraphResponseDTO.ResultResponseDTO> getAbnormalGraph() {

        GraphImage graphImage = graphService.getRecentGraph();
        return ApiResponse.onSuccess(GraphConverter.toAbnormalGraphResponseDTO(graphImage));
    }

    @Operation(summary = "그래프 저장하기", description = "현재 보여지는 그래프를 DB 에 저장합니다."
            + "(Normal/Abnormal 그래프 중 하나라도 저장할 시 둘 다 저장)")
    @PatchMapping(value = "/{graphId}/save")
    public ApiResponse<GraphResponseDTO.SaveResponseDTO> saveData(
            @PathVariable Long graphId) {

        GraphImage graphImage = graphService.saveGraph(graphId);
        return ApiResponse.onSuccess(GraphConverter.toSaveResponseDTO(graphImage));
    }
}
