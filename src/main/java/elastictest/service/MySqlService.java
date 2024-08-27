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
public class MySqlService {

    private final ObjectMapper objectMapper;
    private final MySqlMapper mySqlMapper;

    @Autowired
    public MySqlService(ObjectMapper objectMapper, MySqlMapper mySqlMapper) {
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
                JsonNode jsonNode1 = jsonNode.get("metrics");
                int nodeOneSize = jsonNode1.size();
                Object a = jsonNode1.get(0).get("child").get(0).get("child").get(0);

                for (int i = 0; i < nodeOneSize; i++) {
                    JsonNode jsonNode2 = jsonNode1.get(i);
                    int nodeTwoSize = jsonNode2.size();

                    for (int j = 0; j < nodeTwoSize; j++) {
                        JsonNode jsonNode3 = jsonNode2.get("child");
                        int nodeThreeSize = jsonNode3.size();

                        for (int k = 0; k < nodeThreeSize; k++) {
                            JsonNode jsonNode4 = jsonNode3.get(k).get("child");
                            int nodeFourSize = jsonNode4.size();

                            for (int l = 0; l < nodeFourSize; l++) {
                                JsonNode jsonNode5 = jsonNode4.get(l);
                                String kcCode = jsonNode5.get("code").textValue();

                                // 임시 Parsing. 실데이터 사용시는 필요 X.
                                if (kcCode.startsWith("MATH")) {
                                    kcCode = kcCode.replaceFirst("^MATH", "MAT");
                                    String numberPart = kcCode.substring(3);
                                    int number = Integer.parseInt(numberPart);
                                    if (number > 350) {
                                        kcCode = kcCode.replaceFirst("^MAT0", "MAT");
                                    }
                                }


                                StdntDto dbData = mySqlMapper.findByKcCode(kcCode);

                                int crsNo;
                                int chptNo;
                                String achvCd = null;
                                String achvCn = null;
                                List<String> lctrCds = new ArrayList<>();

                                if (dbData == null) {
                                    crsNo = 0;
                                    chptNo = 0;
                                } else {
                                    crsNo = dbData.getStd_crs_no();
                                    chptNo = dbData.getStd_chpt_no();
                                    achvCd = dbData.getStd_achv_cd();
                                    achvCn = dbData.getAchv_stdr_cn();
                                    Map<String, Object> params = new HashMap<>();
//                                    params.put("accountName", "ffef89ce-60d2-4022-989e-cbccd9bb92f4");
//                                    params.put("chptNo", 1);
                                    params.put("accountName", accountName);
                                    params.put("chptNo", chptNo);
                                    lctrCds = mySqlMapper.findLectureCode(params);
                                }


                                StdntDto stdntDto = StdntDto.builder()
                                        .stdnt_id(accountName)
                                        .std_crs_no(crsNo)
                                        .std_chpt_no(chptNo)
                                        .std_achv_cd(achvCd)
                                        .achv_stdr_cn(achvCn)
                                        .kc_cd(kcCode)
                                        .kc_avg(0)
                                        .ymd(todayYmd)
                                        .build();

                                if (!lctrCds.isEmpty()) {
                                    for (String code : lctrCds) {
                                        stdntDto.setLctr_cd(code);
                                        stdntDtos.add(stdntDto);
                                        count++;
                                    }
                                } else {
                                    stdntDto.setLctr_cd("empty_lecture");
                                    stdntDtos.add(stdntDto);
                                    count++;
                                }

                                if (count % size == 0) {
                                    mySqlMapper.insertAll(stdntDtos);
                                    stdntDtos.clear();
                                }
                            }
                        }
                    }
                }
                if (!stdntDtos.isEmpty()) {
                    mySqlMapper.insertAll(stdntDtos);
                }
            }
        }
    }
}
