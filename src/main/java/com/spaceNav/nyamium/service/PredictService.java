package com.spaceNav.nyamium.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spaceNav.nyamium.apiPayLoad.code.status.ErrorStatus;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.PredictHandler;
import com.spaceNav.nyamium.aws.s3.AmazonS3Manager;
import com.spaceNav.nyamium.converter.PredictConverter;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.domain.OptionalValues;
import com.spaceNav.nyamium.domain.enums.Prediction;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.repository.DataRepository;
import com.spaceNav.nyamium.repository.FileDataRepository;
import com.spaceNav.nyamium.repository.OptionalValuesRepository;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import com.spaceNav.nyamium.web.dto.PredictResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PredictService {

    private final AmazonS3Manager amazonS3Manager;

    private final DataRepository dataRepository;
    private final FileDataRepository fileDataRepository;
    private final OptionalValuesRepository optionalValuesRepository;

    public Data predictByData(PredictRequestDTO request) {
        // 0) 필수값 검증 (e1..l4 + rangingError)
        Float[] required = {
                request.getE1(), request.getE2(), request.getE3(), request.getE4(),
                request.getL1(), request.getL2(), request.getL3(), request.getL4(),
                request.getRangingError()
        };
        String[] names = {"e1","e2","e3","e4","l1","l2","l3","l4","rangingError"};
        for (int i = 0; i < required.length; i++) {
            if (required[i] == null) {
                throw new IllegalArgumentException("필수 입력 누락: " + names[i]);
            }
        }

        Path tmp = null;
        try {
            // 1) 입력 JSON 구성 (파일 X 입력 형식)
            ObjectMapper om = new ObjectMapper();
            ObjectNode root = om.createObjectNode();

            ObjectNode results = om.createObjectNode();
            ArrayNode resultList = om.createArrayNode();
            ObjectNode item = om.createObjectNode();

            item.put("id", 0);
            item.put("e1",  request.getE1());
            item.put("e2",  request.getE2());
            item.put("e3",  request.getE3());
            item.put("e4",  request.getE4());
            item.put("l1",  request.getL1());
            item.put("l2",  request.getL2());
            item.put("l3",  request.getL3());
            item.put("l4",  request.getL4());
            item.put("rangingError", request.getRangingError());
            item.put("delta",  request.getDelta()  == null ? 0.0f : request.getDelta());
            item.put("fd",     request.getFd()     == null ? 0.0f : request.getFd());
            item.put("sigma",  request.getSigma()  == null ? 0.0f : request.getSigma());


            resultList.add(item);
            results.set("resultList", resultList);
            root.set("results", results);
            root.put("dataNum", 1);
            // 파일 단일 입력이므로 fileName은 null 유지
            root.putNull("fileName");

            // 2) 임시 JSON 파일로 저장 → 파이썬 모델 호출
            tmp = Files.createTempFile("single_input_", ".json");
            try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                w.write(om.writeValueAsString(root));
            }

            String modelOutputJson = runPythonWithInputJson(tmp.toString());

            // 3) 출력 JSON 파싱 (파일 X 출력 형식)
            JsonNode outRoot = om.readTree(modelOutputJson);
            JsonNode outResults = outRoot.path("results");
            ArrayNode outList = (ArrayNode) outResults.path("resultList");
            if (outList == null || outList.isEmpty()) {
                throw new RuntimeException("모델 출력에 resultList가 비어 있습니다.");
            }
            JsonNode outItem = outList.get(0);

            // prediction / probability 추출 (방어적으로 처리)
            Prediction prediction;
            if (outItem.hasNonNull("prediction")) {
                prediction = Prediction.valueOf(outItem.get("prediction").asText().toUpperCase());
            } else {
                // 없으면 확률로 판정
                float prob = getNum(outItem.get("probability")) != null ? getNum(outItem.get("probability")) : 0f;
                prediction = prob > 0.75f ? Prediction.ABNORMAL : Prediction.NORMAL;
            }
            Float probability = getNum(outItem.get("probability"));
            if (probability == null) probability = getNum(outItem.get("prob")); // 호환 키

            // 4) Data 저장
            Data data = Data.builder()
                    .e1(request.getE1()).e2(request.getE2()).e3(request.getE3()).e4(request.getE4())
                    .l1(request.getL1()).l2(request.getL2()).l3(request.getL3()).l4(request.getL4())
                    .prediction(prediction)
                    .probability(probability)
                    .save(Save.NOT_SAVE)
                    .fileData(null)
                    .optionalValues(null)
                    .build();
            Data savedData = dataRepository.save(data);

            // 5) OptionalValues 저장
            OptionalValues ov = OptionalValues.builder()
                    .data(savedData)
                    .rangingError(request.getRangingError())
                    .delta(request.getDelta())
                    .fd(request.getFd())
                    .sigma(request.getSigma())
                    .build();
            optionalValuesRepository.save(ov);
            savedData.addOptionalValues(ov);

            return savedData;

        } catch (Exception e) {
            throw new RuntimeException("단일 샘플(JSON) 예측 처리 실패: " + e.getMessage(), e);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
            }
        }
    }

    /** null-safe 숫자 추출 유틸 (기존에 사용 중이면 그대로 활용) */
    private Float getNum(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isNumber()) return n.floatValue();
        try { return Float.parseFloat(n.asText()); } catch (Exception e) { return null; }
    }


    public PredictResponseDTO.ForResultResponseFileDTO predictByFileData(MultipartFile file) {

        // 0) 업로드 파일 검증: JSON만 허용
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".json")) {
            throw new PredictHandler(ErrorStatus.FILE_TYPE_NOT_CORRECT);
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("업로드 파일 읽기 실패", e);
        }

        try {
            // 1) S3 업로드(선 저장)
            String keyName = amazonS3Manager.generateFileDataKeyName();
            String fileUrl = amazonS3Manager.uploadFile(keyName, file);

            // 2) 임시 파일 저장 → 파이썬 실행(JSON 수신)
            Path tmp = Files.createTempFile("json_", ".json");
            Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING);

            // predict.py의 CLI 규약에 맞게 인자 구성:
            //  - 기존에 --file 로 경로만 받는 스크립트였다면 그대로 --file에 JSON 경로를 주면 됩니다.
            //  - 스크립트가 --input-json 등을 요구하면 runPythonForJsonAndGetJson 내부 인자를 조정하세요.
            String modelOutputJson = runPythonWithInputJson(tmp.toString());

            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}

            // 3) JSON 파싱 (모델의 "출력 형식" 구조에 맞춰 파싱)
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(modelOutputJson);

            // results.resultList 배열
            JsonNode resultsNode = root.path("results");
            ArrayNode resultList = (resultsNode.isMissingNode())
                    ? om.createArrayNode()
                    : (ArrayNode) resultsNode.path("resultList");

            // dataNum은 없으면 resultList.size() 사용
            int numSamples = root.has("dataNum") ? root.get("dataNum").asInt() : resultList.size();

            // 4) FileData 저장
            FileData fileData = FileData.builder()
                    .fileName(root.hasNonNull("fileName") ? root.get("fileName").asText() : filename) // 모델 출력에 fileName이 있으면 우선 사용
                    .fileUrl(fileUrl)
                    .keyName(keyName)
                    .dataNum((long) numSamples)
                    .dataList(null)
                    .build();
            fileData = fileDataRepository.save(fileData);

            // 5) 각 결과 → Data/OptionalValues 저장
            List<Data> dataList = new ArrayList<>();
            for (JsonNode r : resultList) {
                Float e1 = getNum(r.get("e1"));
                Float e2 = getNum(r.get("e2"));
                Float e3 = getNum(r.get("e3"));
                Float e4 = getNum(r.get("e4"));
                Float l1 = getNum(r.get("l1"));
                Float l2 = getNum(r.get("l2"));
                Float l3 = getNum(r.get("l3"));
                Float l4 = getNum(r.get("l4"));

                Float rangingError = getNum(r.get("rangingError"));
                Float delta        = getNum(r.get("delta"));
                Float fd           = getNum(r.get("fd"));
                Float sigma        = getNum(r.get("sigma"));

                // optional value 가 없을 경우 0으로 처리
                if (delta == null) delta = 0.0f;
                if (fd    == null) fd    = 0.0f;
                if (sigma == null) sigma = 0.0f;

                // 모델 출력 형식(파일 X)에선 prediction/probability가 포함됨. 없을 수도 있으니 방어적으로 처리
                Prediction prediction;
                if (r.hasNonNull("prediction")) {
                    prediction = Prediction.valueOf(r.get("prediction").asText().toUpperCase());
                } else {
                    // pred=0/1 호환
                    prediction = r.path("pred").asInt() == 1 ? Prediction.ABNORMAL : Prediction.NORMAL;
                }

                Float probability = getNum(r.has("probability") ? r.get("probability") : r.get("prob"));

                Data data = Data.builder()
                        .e1(e1).e2(e2).e3(e3).e4(e4)
                        .l1(l1).l2(l2).l3(l3).l4(l4)
                        .prediction(prediction)
                        .probability(probability)
                        .save(Save.NOT_SAVE)
                        .fileData(fileData)
                        .build();
                Data savedData = dataRepository.save(data);

                OptionalValues ov = OptionalValues.builder()
                        .data(savedData)
                        .rangingError(rangingError)
                        .delta(delta)
                        .fd(fd)
                        .sigma(sigma)
                        .build();
                optionalValuesRepository.save(ov);
                savedData.addOptionalValues(ov);

                dataList.add(savedData);
            }

            fileData.addDataList(dataList);
            fileDataRepository.save(fileData);

            log.info("JSON 파일 기반 데이터 {}건 저장 완료", dataList.size());

            return PredictConverter.toForResultResponseFileDTO(dataList, fileData);

        } catch (Exception e) {
            log.error("JSON 파일 처리 실패", e);
            throw new RuntimeException("JSON 파일 처리 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 파이썬을 호출하여 입력 JSON 경로를 전달하고
     * STDOUT(JSON 문자열)을 모두 읽어 반환합니다.
     * predict.py가 --file 로 경로를 받도록 되어 있다면 그대로 사용하세요.
     * (만약 --input-json 같은 인자를 요구하면 아래 인자를 교체)
     */
    private String runPythonWithInputJson(String jsonPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "predict.py",
                "--file", jsonPath
        );
        pb.redirectErrorStream(false);

        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) out.append(line);
        }

        StringBuilder err = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) err.append(line).append('\n');
        }

        int exit = p.waitFor();
        if (exit != 0) throw new RuntimeException("python exit=" + exit + " stderr=" + err);
        if (out.length() == 0) throw new RuntimeException("파이썬 STDOUT이 비었습니다. stderr=" + err);

        return out.toString();
    }


    public List<Data> saveData(){
        // DB 내의 모든 NOT_SAVE 에 대해 SAVE 로 바꿈

        // 1️⃣ NOT_SAVE 데이터 조회
        List<Data> notSavedList = dataRepository.findAllBySave(Save.NOT_SAVE);
        if (notSavedList.isEmpty()) return List.of();

        // 2️⃣ 상태 변경
        notSavedList.forEach(d -> d.changeSaveStatus(Save.SAVE));

        // 3️⃣ 저장 (update)
        return dataRepository.saveAll(notSavedList);
    }

    public List<Data> notSaveData(){
        // DB 내의 모든 NOT_SAVE 데이터들 삭제 + 해당 데이터 리스트 반환

        // 1️⃣ 삭제 대상 조회
        List<Data> notSavedList = dataRepository.findAllBySave(Save.NOT_SAVE);
        if (notSavedList.isEmpty()) return List.of();

        // 2️⃣ 삭제
        dataRepository.deleteAll(notSavedList);

        // 3️⃣ 삭제된 데이터 목록 반환
        return notSavedList;
    }
}

