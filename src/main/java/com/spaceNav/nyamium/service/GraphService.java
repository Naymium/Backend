package com.spaceNav.nyamium.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaceNav.nyamium.apiPayLoad.code.status.ErrorStatus;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.DataHandler;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.GraphHandler;
import com.spaceNav.nyamium.aws.s3.AmazonS3Manager;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.GraphImage;
import com.spaceNav.nyamium.domain.OptionalValues;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.repository.DataRepository;
import com.spaceNav.nyamium.repository.GraphImageRepository;
import com.spaceNav.nyamium.repository.OptionalValuesRepository;
import com.spaceNav.nyamium.web.dto.GraphResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class GraphService {

    private final DataRepository dataRepository;
    private final OptionalValuesRepository optionalValuesRepository;
    private final GraphImageRepository graphImageRepository;

    private static final ObjectMapper OM = new ObjectMapper();
    private final AmazonS3Manager amazonS3Manager;

    @Transactional
    public GraphImage buildGraphsFromDb() {
        List<Data> all = dataRepository.findAll();
        if (all.isEmpty()) {
            throw new IllegalStateException("그래프 생성할 데이터가 없습니다.");
        }

        Map<Long, OptionalValues> ovMap = optionalValuesRepository.findByData_IdIn(
                all.stream().map(Data::getId).toList()
        ).stream().collect(Collectors.toMap(ov -> ov.getData().getId(), ov -> ov));

        byte[] csvBytes = buildCsvInMemory(all, ovMap);

        try {
            String json = runPythonAndGetBase64JsonWithCsv(csvBytes);

            JsonNode root = OM.readTree(json);
            String n64 = root.path("normal_png").asText(null);
            String a64 = root.path("abnormal_png").asText(null);
            long normalCount   = root.path("normal_count").asLong(0);
            long abnormalCount = root.path("abnormal_count").asLong(0);

            if (n64 == null || a64 == null)
                throw new IllegalStateException("파이썬 응답에 이미지 데이터가 없습니다.");

            byte[] normal   = Base64.getDecoder().decode(n64);
            byte[] abnormal = Base64.getDecoder().decode(a64);

            String graphKeyName = amazonS3Manager.generateGraphKeyName();

            String normalKey   = graphKeyName + "normal.png";
            String abnormalKey = graphKeyName + "abnormal.png";

            String normalUrl   = amazonS3Manager.putGraphImageToS3(normalKey, normal, "image/png");
            String abnormalUrl = amazonS3Manager.putGraphImageToS3(abnormalKey, abnormal, "image/png");

            GraphImage newGraph =  GraphImage.builder()
                    .normalDataUrl(normalUrl)
                    .abnormalDataUrl(abnormalUrl)
                    .normalDataKeyName(normalKey)
                    .abnormalDataKeyName(abnormalKey)
                    .normalDataNum(normalCount)
                    .abnormalDataNum(abnormalCount)
                    .save(Save.SAVE)
                    .build();

            graphImageRepository.save(newGraph);


            return newGraph;

        } catch (Exception e) {
            log.error("그래프 생성 실패", e);
            throw new RuntimeException("그래프 생성 실패: " + e.getMessage(), e);
        }
    }

    /* ===== 내부 유틸: CSV 빌드 & 파이썬 호출 (이전과 동일) ===== */

    private byte[] buildCsvInMemory(List<Data> all, Map<Long, OptionalValues> ovMap) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", "id",
                "e1","e2","e3","e4","l1","l2","l3","l4",
                "rangingError","delta","fd","sigma",
                "prediction","probability")).append("\n");

        for (Data d : all) {
            OptionalValues ov = ovMap.get(d.getId());
            String predStr = d.getPrediction() != null ? d.getPrediction().name() : "";
            String probStr = d.getProbability() != null ? d.getProbability().toString() : "";
            sb.append(String.join(",",
                    s(d.getId()),
                    s(d.getE1()), s(d.getE2()), s(d.getE3()), s(d.getE4()),
                    s(d.getL1()), s(d.getL2()), s(d.getL3()), s(d.getL4()),
                    s(ov != null ? ov.getRangingError() : null),
                    s(ov != null ? ov.getDelta()        : null),
                    s(ov != null ? ov.getFd()           : null),
                    s(ov != null ? ov.getSigma()        : null),
                    predStr, probStr)).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String runPythonAndGetBase64JsonWithCsv(byte[] csvBytes) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "predict.py",
                "--stdin-csv",
                "--plots-out-base64"
        );
        pb.redirectErrorStream(false);
        Process p = pb.start();

        try (OutputStream os = p.getOutputStream()) {
            os.write(csvBytes);
        }

        String json = readAll(p.getInputStream());
        String err  = readAll(p.getErrorStream());
        int exit = p.waitFor();
        if (exit != 0) throw new RuntimeException("python exit=" + exit + " stderr=" + err);
        if (json == null || json.isBlank()) throw new RuntimeException("파이썬 STDOUT 비어있음. stderr=" + err);
        return json;
    }

    private String readAll(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line; while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    @Transactional
    public GraphImage getGraphImageById(Long graphId) {
        return graphImageRepository.findById(graphId)
                .orElseThrow(() -> new GraphHandler(ErrorStatus.GRAPH_IMAGE_NOT_FOUND));
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

    private String s(Object v) { return v == null ? "" : String.valueOf(v); }
}
