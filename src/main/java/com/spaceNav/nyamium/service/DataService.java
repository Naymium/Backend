package com.spaceNav.nyamium.service;

import com.spaceNav.nyamium.apiPayLoad.code.status.ErrorStatus;
import com.spaceNav.nyamium.apiPayLoad.exception.handler.DataHandler;
import com.spaceNav.nyamium.converter.DataConverter;
import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.repository.DataRepository;
import com.spaceNav.nyamium.web.dto.DataResponseDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class DataService {

    private final DataRepository dataRepository;

    @Transactional(readOnly = true)
    public List<Data> getAllData() {
        return dataRepository.findAll();
    }

    public byte[] downloadDataCSV() throws IOException {
        List<Data> dataList = dataRepository.findAll();
        DataResponseDTO dtoList =  DataConverter.toDataResponseDTO(dataList);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "id", "e1", "e2", "e3", "e4", "l1", "l2", "l3", "l4",
                "prediction", "probability",
                "rangingError", "delta", "fD", "sigma"
        ));

        for (DataResponseDTO.GetOneDataResponseDTO dto : dtoList.getGetAllDataResponseDTO()) {
            csvPrinter.printRecord(
                    dto.getId(), dto.getE1(), dto.getE2(), dto.getE3(), dto.getE4(),
                    dto.getL1(), dto.getL2(), dto.getL3(), dto.getL4(),
                    dto.getPrediction(), dto.getProbability(),
                    dto.getRangingError(), dto.getDelta(), dto.getFD(), dto.getSigma()
            );
        }

        csvPrinter.flush();
        return out.toByteArray();
    }

    public Map<String, Object> deleteData(Long dataId) {
        Data data = dataRepository.findById(dataId)
                .orElseThrow(() -> new DataHandler(ErrorStatus.DATA_NOT_FOUND));

        dataRepository.delete(data);
        Long totalCount = dataRepository.count();

        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        result.put("count", totalCount);
        return result;
    }
}
