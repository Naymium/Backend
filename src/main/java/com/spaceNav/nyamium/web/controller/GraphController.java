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

    @Operation(summary = "정상/비정상 데이터 그래프 확인", description = "S3 로부터 현재 데이터(최신)를 이용한 정상/비정상 데이터 그래프를 가져옵니다.")
    @GetMapping()
    public ApiResponse<GraphResponseDTO.GraphResultResponseDTO> getGraph() {

        GraphImage GraphImage = graphService.buildGraphsFromDb();
        return ApiResponse.onSuccess(GraphConverter.toGraphResultResponseDTO(GraphImage));
    }
}
