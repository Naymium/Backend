package com.spaceNav.nyamium.service;

import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.OptionalValues;
import com.spaceNav.nyamium.domain.enums.Save;
import com.spaceNav.nyamium.repository.DataRepository;
import com.spaceNav.nyamium.repository.FileDataRepository;
import com.spaceNav.nyamium.repository.OptionalValuesRepository;
import com.spaceNav.nyamium.web.dto.PredictRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PredictService {

    private final DataRepository dataRepository;
    private final FileDataRepository fileDataRepository;
    private final OptionalValuesRepository optionalValuesRepository;

    private final String pythonPath = "python3"; // Windows는 "python"
    private final String scriptPath = "/path/to/predict.py";
    private final String savePath = "/path/to/save/directory/";

    public Data predictByData(PredictRequestDTO predictRequestDTO) {

        /* 파이썬 모델 사용 부분 */
        return null;
    }

    public Data predictByFileData(MultipartFile file) {

        /* 파이썬 모델 사용 부분 */
        return null;
    }

    @Transactional(readOnly = true)
    public Resource getPredictionFile(String filename) {
        try {
            File file = new File(savePath + filename);
            if (!file.exists()) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + filename);
            }
            return new FileSystemResource(file);
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드 실패", e);
        }
    }

    public List<Data> saveData(){
        // DB 내의 모든 NOT_SAVE 에 대해 SAVE 로 바꿈

        return null;
    }

    public List<Data> notSaveData(){
        // DB 내의 모든 NOT_SAVE 데이터들 삭제 + 해당 데이터 리스트 반환

        return null;
    }


}

