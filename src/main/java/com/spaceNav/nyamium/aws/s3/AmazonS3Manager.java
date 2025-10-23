package com.spaceNav.nyamium.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.spaceNav.nyamium.config.AmazonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Manager{

    private final AmazonS3 amazonS3;

    private final AmazonConfig amazonConfig;

    public String uploadFile(String keyName, MultipartFile file){
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        try {
            amazonS3.putObject(
                    new PutObjectRequest(
                            amazonConfig.getBucket(),
                            keyName,
                            file.getInputStream(),
                            metadata));
        } catch (IOException e){
            log.error("error at AmazonS3Manager uploadFile : {}", (Object) e.getStackTrace());
        }

        return getFileUrl(keyName);
    }

    public String putGraphImageToS3(String keyName, byte[] bytes, String contentType) {
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.setContentType(contentType);
        metaData.setContentLength(bytes.length);

        try (java.io.InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            amazonS3.putObject(
                    amazonConfig.getBucket(),
                    keyName,
                    in,
                    metaData);
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + keyName, e);
        }
        return getGraphUrl(keyName);
    }

    // 파일 접근 URL 생성 메서드
    public String getFileUrl(String keyName) {
        return amazonS3.getUrl(amazonConfig.getBucket(), keyName).toString();
    }

    // 그래프 접근 URL 생성 메서드
    public String getGraphUrl(String keyName) {return amazonS3.getUrl(amazonConfig.getBucket(), keyName).toString();}

    // S3 에 있는 파일 삭제
    public void deleteFileFromS3(String keyName) {
        amazonS3.deleteObject(amazonConfig.getBucket(), keyName);
    }

    public String generateGraphKeyName() {
        return amazonConfig.getGraphPath() + '/' + UUID.randomUUID().toString();
    }

    public String generateFileDataKeyName() {
        return amazonConfig.getFileDataPath() + '/' + UUID.randomUUID().toString();
    }
}
