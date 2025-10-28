package com.spaceNav.nyamium.apiPayLoad.code.status;

import com.spaceNav.nyamium.apiPayLoad.code.BaseErrorCode;
import com.spaceNav.nyamium.apiPayLoad.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 가장 일반적인 응답
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),

    // FileType 관련
    FILE_TYPE_NOT_CORRECT(HttpStatus.BAD_REQUEST, "FILEDATA4001", "Json 파일만 업로드 가능합니다."),

    //그래프 이미지 관련
    GRAPH_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "GRAPH4001", "그래프 이미지를 찾을 수 없습니다."),

    //데이터 관련
    DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "DATA4001", "데이터를 찾을 수 없습니다."),

    //파일 관련
    FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "FILE4001", "파일을 찾을 수 없습니다.")
    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}
