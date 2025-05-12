package com.spaceNav.nyamium.service;

import com.spaceNav.nyamium.apiPayLoad.code.status.ErrorStatus;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.GraphHandler;
import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.repository.GraphImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GraphService {

    private final GraphImageRepository graphImageRepository;

    @Transactional(readOnly = true)
    public GraphImage getRecentGraph() {
        // 생성일 기준으로 정렬했을 때 가장 최신 GraphImage 반환
        return graphImageRepository.findTopByOrderByCreatedAtDesc();
    }

    @Transactional
    public GraphImage saveGraph(Long graphId) {
        // 그래프를 ID로 조회
        GraphImage graphImage = graphImageRepository.findById(graphId)
                .orElseThrow(() -> new GraphHandler(ErrorStatus.GRAPH_IMAGE_NOT_FOUND));

        // Save 상태를 변경
        graphImage.changeSaveStatus(Save.SAVE);

        // 변경된 객체를 저장
        return graphImageRepository.save(graphImage);
    }
}
