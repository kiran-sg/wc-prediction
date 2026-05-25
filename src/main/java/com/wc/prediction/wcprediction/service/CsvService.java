package com.wc.prediction.wcprediction.service;

import com.wc.prediction.wcprediction.model.WcMatchCsv;
import com.wc.prediction.wcprediction.model.WcPlayerCsv;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

@Service
public class CsvService {

    public List<WcMatchCsv> readMatchesFromCsv() throws IOException {
        ClassPathResource resource = new ClassPathResource("wc-2026-schedule.csv");
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            HeaderColumnNameMappingStrategy<WcMatchCsv> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(WcMatchCsv.class);
            CsvToBean<WcMatchCsv> csvToBean = new CsvToBeanBuilder<WcMatchCsv>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csvToBean.parse();
        }
    }

    public List<WcPlayerCsv> readPlayersFromCsv() throws IOException {
        ClassPathResource resource = new ClassPathResource("wc-2026-players.csv");
        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            HeaderColumnNameMappingStrategy<WcPlayerCsv> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(WcPlayerCsv.class);
            CsvToBean<WcPlayerCsv> csvToBean = new CsvToBeanBuilder<WcPlayerCsv>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();
            return csvToBean.parse();
        }
    }
}
