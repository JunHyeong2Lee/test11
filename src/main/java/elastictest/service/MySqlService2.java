package elastictest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elastictest.dao.MySqlMapper;
import elastictest.dto.StdntDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MySqlService2 {

    private final ObjectMapper objectMapper;
    private final MySqlMapper mySqlMapper;

    @Autowired
    public MySqlService2(ObjectMapper objectMapper, MySqlMapper mySqlMapper) {
        this.objectMapper = objectMapper;
        this.mySqlMapper = mySqlMapper;
    }

    public void insertJson(String filePath, int size) throws IOException {
        List<StdntDto> stdntDtos = new ArrayList<>();
        int count = 0;
        String todayYmd = new SimpleDateFormat("yyyyMMdd").format(new Date());

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = br.readLine()) != null) {
                JsonNode jsonNode = objectMapper.readTree(line);
                String accountName = jsonNode.get("account_name").asText();
                JsonNode metricsCode = jsonNode.get("metrics");

                for (JsonNode metric : metricsCode) {
                    processMetricNode(metric, accountName, todayYmd, stdntDtos);
                }

                if (stdntDtos.size() >= size) {
                    mySqlMapper.insertAll(stdntDtos);
                    stdntDtos.clear();
                }
            }
        }

        if (!stdntDtos.isEmpty()) {
            mySqlMapper.insertAll(stdntDtos);
            stdntDtos.clear();
        }
    }

    private void processMetricNode(JsonNode metric, String accountName, String todayYmd, List<StdntDto> stdntDtos) {
        JsonNode childNode = metric.get("child");
        if (childNode == null) return;
        for (JsonNode child : childNode) {
            JsonNode grandChildNode = child.get("child");
            if (grandChildNode == null) return;

            for (JsonNode grandChild : grandChildNode) {
                if (grandChild == null) return;
                processEntry(grandChild, accountName, todayYmd, stdntDtos);
            }
        }
    }

    private void processEntry(JsonNode entry, String accountName, String todayYmd, List<StdntDto> stdntDtos) {
        String kcCode = formatKcCode(entry.get("code").textValue());
        StdntDto dbData = mySqlMapper.findByKcCode(kcCode);

        int crsNo = 0;
        int chptNo = 0;
        String achvCd = null;
        String achvCn = null;

        List<String> lctrCds = Collections.emptyList();

        if (dbData != null) {
            crsNo = dbData.getStd_crs_no();
            chptNo = dbData.getStd_chpt_no();
            achvCd = dbData.getStd_achv_cd();
            achvCn = dbData.getAchv_stdr_cn();

            Map<String, Object> params = new HashMap<>();
            params.put("accountName", accountName);
            params.put("chptNo", chptNo);

            lctrCds = mySqlMapper.findLectureCode(params);
        }

        StdntDto stdntDtoTemplate = StdntDto.builder()
                .stdnt_id(accountName)
                .std_crs_no(crsNo)
                .std_chpt_no(chptNo)
                .std_achv_cd(achvCd)
                .achv_stdr_cn(achvCn)
                .kc_cd(kcCode)
                .kc_avg(entry.get("value").asDouble())
                .ymd(todayYmd)
                .build();

        if (lctrCds.isEmpty()) {
            stdntDtoTemplate.setLctr_cd("empty_lecture");
            stdntDtos.add(stdntDtoTemplate);
        } else {
            for (String code : lctrCds) {
                stdntDtoTemplate.setLctr_cd(code);
                stdntDtos.add(stdntDtoTemplate);
            }
        }
    }

    private String formatKcCode(String kcCode) {
        if (kcCode.startsWith("MATH")) {
            kcCode = kcCode.replaceFirst("^MATH", "MAT");
            String numberPart = kcCode.substring(3);
            int number = Integer.parseInt(numberPart);
            if (number > 350) {
                kcCode = kcCode.replaceFirst("^MAT0", "MAT");
            }
        }
        return kcCode;
    }
}
