package elastictest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StdntDto {

    private int id;
    private String stdnt_id;
    private String lctr_cd;
    private int std_crs_no;
    private int std_chpt_no;
    private String std_achv_cd;
    private String achv_stdr_cn;
    private String kc_cd;
    private double kc_avg;
    private String ymd;
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private String regDt;           /* 등록일 */
}
