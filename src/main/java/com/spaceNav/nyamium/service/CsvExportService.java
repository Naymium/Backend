package com.spaceNav.nyamium.service;

import com.spaceNav.nyamium.web.dto.PredictResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@Transactional
@RequiredArgsConstructor
public class CsvExportService {

    private static final String[] HEADERS = new String[]{
            "id",
            "e1","e2","e3","e4",
            "l1","l2","l3","l4",
            "rangingError","delta","fd","sigma",
            "prediction","probability"
    };

    /** 단일 결과(ResultResponseDTO) → CSV (메모리, UTF-8 BOM 포함) */
    public Resource buildCsvForSingle(PredictResponseDTO.ResultResponseDTO r) {
        StringBuilder sb = new StringBuilder();
        appendBom(sb);
        appendHeader(sb);

        appendRow(sb, r);

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(bytes);
    }

    /** 파일 결과(ResultResponseFileDTO) → CSV (메모리, UTF-8 BOM 포함) */
    public Resource buildCsvForFile(PredictResponseDTO.ResultResponseFileDTO fileDto) {
        StringBuilder sb = new StringBuilder();
        appendBom(sb);
        // 메타 한 줄(선택): 파일명/데이터수
        if (fileDto.getFileName() != null || fileDto.getDataNum() != null) {
            sb.append("# fileName=").append(s(fileDto.getFileName()))
                    .append(", dataNum=").append(s(fileDto.getDataNum()))
                    .append("\n");
        }
        appendHeader(sb);

        if (fileDto.getResults() != null && fileDto.getResults().getResultList() != null) {
            for (PredictResponseDTO.ResultResponseDTO r : fileDto.getResults().getResultList()) {
                appendRow(sb, r);
            }
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(bytes);
    }

    /* ===================== 내부 유틸 ===================== */

    private void appendBom(StringBuilder sb) {
        // UTF-8 BOM for Excel
        sb.append('\uFEFF');
    }

    private void appendHeader(StringBuilder sb) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(HEADERS[i]));
        }
        sb.append('\n');
    }

    private void appendRow(StringBuilder sb, PredictResponseDTO.ResultResponseDTO r) {
        // id
        sb.append(escape(s(r.getId()))).append(',');

        // e1..e4
        sb.append(escape(n(r.getE1()))).append(',');
        sb.append(escape(n(r.getE2()))).append(',');
        sb.append(escape(n(r.getE3()))).append(',');
        sb.append(escape(n(r.getE4()))).append(',');

        // l1..l4
        sb.append(escape(n(r.getL1()))).append(',');
        sb.append(escape(n(r.getL2()))).append(',');
        sb.append(escape(n(r.getL3()))).append(',');
        sb.append(escape(n(r.getL4()))).append(',');

        // optionals
        sb.append(escape(n(r.getRangingError()))).append(',');
        sb.append(escape(n(r.getDelta()))).append(',');
        sb.append(escape(n(r.getFd()))).append(',');
        sb.append(escape(n(r.getSigma()))).append(',');

        // prediction, probability
        sb.append(escape(r.getPrediction() == null ? "" : r.getPrediction().name())).append(',');
        sb.append(escape(n(r.getProbability()))).append('\n');
    }

    private String s(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private String n(Float f) {
        return f == null ? "" : String.valueOf(f);
    }

//    // 보기 좋게 1.230000 -> 1.23 / 2.0 -> 2
//    private String stripTrailingZeros(Float f) {
//        String str = String.valueOf(f);
//        if (str.contains(".")) {
//            // remove trailing zeros
//            str = str.replaceAll("0+$", "").replaceAll("\\.$", "");
//        }
//        return str;
//    }

    /** RFC4180 스타일 이스케이프: 따옴표/콤마/개행 포함 시 전체를 "..."로 감싸고 내부 " -> "" */
    private String escape(String field) {
        if (field == null) return "";
        boolean mustQuote = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (!mustQuote) return field;
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }
}
