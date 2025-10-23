package com.spaceNav.nyamium.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import com.spaceNav.nyamium.web.controller.DataController;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import com.spaceNav.nyamium.web.dto.PredictResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static java.lang.reflect.Array.getFloat;

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

        // 1) 파이썬 호출(기존처럼 숫자 인자 나열) → 확률만 1줄 반환
        float probability = runPythonModelForSingle(request);
        Prediction prediction = probability > 0.75f ? Prediction.NORMAL : Prediction.ABNORMAL;

        // 2) Data 저장
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

        // 3) OptionalValues 저장
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
    }

    public PredictResponseDTO.ForResultResponseFileDTO predictByFileData(MultipartFile file) {

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".mat")) {
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
            Path tmp = Files.createTempFile("mat_", ".mat");
            Files.write(tmp, bytes, StandardOpenOption.TRUNCATE_EXISTING);

            // dataKey/threshold는 환경에 맞게 조정
            String json = runPythonForMatAndGetJson(tmp.toString(), "ACF_TMA", "0.75");

            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}

            // 3) JSON 파싱
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(json);
            ArrayNode arr = (ArrayNode) root.withArray("results");
            int numSamples = (root.has("count") ? root.get("count").asInt() : arr.size());

            // 4) FileData 저장
            FileData fileData = FileData.builder()
                    .fileName(filename)
                    .fileUrl(fileUrl)
                    .keyName(keyName)
                    .dataNum((long) numSamples)
                    .dataList(null)
                    .build();
            fileData = fileDataRepository.save(fileData);

            // 5) 각 결과를 DB 저장 → DTO 반환 목록 구성
            List<Data> dataList = new ArrayList<>();
            for (JsonNode r : arr) {
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

                Prediction prediction;
                if (r.hasNonNull("prediction")) {
                    // 문자열 NORMAL/ABNORMAL → Enum
                    prediction = Prediction.valueOf(r.get("prediction").asText().toUpperCase());
                } else {
                    // 혹시 pred=0/1만 준다면 호환
                    prediction = r.path("pred").asInt() == 1 ? Prediction.ABNORMAL : Prediction.NORMAL;
                }

                Float probability = getNum(r.has("probability") ? r.get("probability") : r.get("prob"));

                // DB 저장
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

            log.info("파일 데이터 {}건 저장 완료", dataList.size());

            return PredictConverter.toForResultResponseFileDTO(dataList,fileData);

        } catch (Exception e) {
            log.error("파일 처리 실패", e);
            throw new RuntimeException("MAT 파일 처리 중 오류: " + e.getMessage(), e);
        }
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

    private Float getNum(JsonNode n) {
        return (n == null || n.isNull()) ? null : (float) n.asDouble();
    }

    /**
     * 파이썬을 호출하여 .mat 경로, dataKey, threshold를 전달하고
     * STDOUT(JSON 문자열)을 모두 읽어 반환합니다.
     */
    private String runPythonForMatAndGetJson(String matPath, String dataKey, String threshold) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "predict.py",
                "--file", matPath,
                "--data-key", dataKey,
                "--threshold", threshold,
                "--scaler", "/app/ml/scaler.pkl",
                "--model",  "/app/ml/model.pkl"
        );
        // stdout(JSON)과 stderr(로그) 분리 권장
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

    /**
     * 단일 입력(PredictRequestDTO)을 파이썬 모델에 전달하여 확률(float) 1줄을 받아옵니다.
     * 파이썬 스펙(예시):
     *   python3 predict.py --single
     *     <e1> <e2> <e3> <e4> <l1> <l2> <l3> <l4> <rangingError>
     *     [--delta <v>] [--fD <v>] [--sigma <v>]
     * stdout: 확률(float) 1줄
     */
    private float runPythonModelForSingle(PredictRequestDTO req) {
        // 0) 필수값 검증 (e1..l4 + rangingError)
        Float[] required = {
                req.getE1(), req.getE2(), req.getE3(), req.getE4(),
                req.getL1(), req.getL2(), req.getL3(), req.getL4(),
                req.getRangingError()
        };
        String[] names = {"e1","e2","e3","e4","l1","l2","l3","l4","rangingError"};
        for (int i = 0; i < required.length; i++) {
            if (required[i] == null) {
                throw new IllegalArgumentException("필수 입력 누락: " + names[i]);
            }
        }

        try {
            // 1) 명령 구성
            List<String> cmd = new ArrayList<>();
            cmd.add("python3");          // 환경에 맞게 "python"으로 변경 가능
            cmd.add("predict.py");
            cmd.add("--single");

            // 기본 9개 인자 (순서 고정)
            cmd.add(String.valueOf(req.getE1()));
            cmd.add(String.valueOf(req.getE2()));
            cmd.add(String.valueOf(req.getE3()));
            cmd.add(String.valueOf(req.getE4()));
            cmd.add(String.valueOf(req.getL1()));
            cmd.add(String.valueOf(req.getL2()));
            cmd.add(String.valueOf(req.getL3()));
            cmd.add(String.valueOf(req.getL4()));
            cmd.add(String.valueOf(req.getRangingError()));

            // 선택 인자 (존재 시만 플래그와 함께 전달)
            appendIfPresent(cmd, "--delta", req.getDelta());
            appendIfPresent(cmd, "--fd",    req.getFd());
            appendIfPresent(cmd, "--sigma", req.getSigma());

            // 2) 실행
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process p = pb.start();

            // 3) stdout(확률 1줄) 읽기
            String line;
            try (BufferedReader out = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                line = out.readLine();
            }

            // 4) stderr 로그 수집(에러 분석용)
            StringBuilder err = new StringBuilder();
            try (BufferedReader er = new BufferedReader(
                    new InputStreamReader(p.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String eLine;
                while ((eLine = er.readLine()) != null) err.append(eLine).append('\n');
            }

            int exit = p.waitFor();
            if (exit != 0) throw new RuntimeException("python exit=" + exit + " stderr=" + err);
            if (line == null || line.isBlank()) throw new RuntimeException("빈 모델 출력(stdout). stderr=" + err);

            // 5) float 파싱 및 반환
            return Float.parseFloat(line.trim());

        } catch (Exception e) {
            throw new RuntimeException("단일 샘플 모델 실행 실패: " + e.getMessage(), e);
        }
    }

    /** 선택 인자(존재 시) 플래그와 함께 추가 */
    private void appendIfPresent(List<String> cmd, String flag, Float val) {
        if (val != null) {
            cmd.add(flag);
            cmd.add(String.valueOf(val));
        }
    }
}

