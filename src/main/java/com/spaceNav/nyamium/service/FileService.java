package com.spaceNav.nyamium.service;

import com.spaceNav.nyamium.apiPayLoad.code.status.ErrorStatus;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.FileHandler;
import com.spaceNav.nyamium.aws.s3.AmazonS3Manager;
import com.spaceNav.nyamium.domain.FileData;
import com.spaceNav.nyamium.repository.FileDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FileService {

    private final FileDataRepository fileDataRepository;
    private final AmazonS3Manager amazonS3Manager;

    @Transactional(readOnly = true)
    public List<FileData> getAllFile() {
        return fileDataRepository.findAll();
    }

    public FileData deleteFile(Long fileId) {
        FileData fileData = fileDataRepository.findById(fileId)
                .orElseThrow(() -> new FileHandler(ErrorStatus.FILE_NOT_FOUND));

        // S3에서 파일 삭제
        amazonS3Manager.deleteFileFromS3(fileData.getKeyName());
        // fileKey = S3에 저장할 때 사용한 keyName (DB에 컬럼으로 저장해둬야 함)

        // DB에서 메타데이터 삭제
        fileDataRepository.delete(fileData);

        return fileData; // 삭제된 데이터 반환
    }
}
