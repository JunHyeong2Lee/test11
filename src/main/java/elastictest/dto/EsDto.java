package elastictest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.List;

@Document(indexName = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Mapping(mappingPath = "static/students.json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EsDto {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "account_name")
    @JsonProperty("account_name") // JSON 직렬/역직렬화 시
    private String accountName;

    @Field(type = FieldType.Nested, includeInParent = true)
    private List<Metric> metrics;

    @Field(type = FieldType.Date)
    private String YMD;

    @Data
    public static class Metric {

        @Field(type = FieldType.Keyword)
        private String code;

        @Field(type = FieldType.Keyword)
        private String name;

        @JsonInclude(JsonInclude.Include.NON_NULL) // value가 null일 때 JSON에서 제외
        @Field(type = FieldType.Double)
        private Double value;

        @JsonInclude(JsonInclude.Include.NON_NULL) // value가 null일 때 JSON에서 제외
        @Field(type = FieldType.Nested, includeInParent = true)
        private List<Metric> child;

    }
}