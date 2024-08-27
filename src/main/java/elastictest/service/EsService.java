package elastictest.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.AvgAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FilterAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.NestedAggregate;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elastictest.dto.EsDto;
import elastictest.repository.EsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EsService {

    private final EsRepository esRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    public EsService(EsRepository esRepository, ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper, ElasticsearchTemplate elasticsearchTemplate) {
        this.esRepository = esRepository;
        this.elasticsearchClient = elasticsearchClient;
        this.objectMapper = objectMapper;
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    // ====================== 데이터 입력 ======================

    public void insertJson(String filePath, int size) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            List<EsDto> esDtoList = new ArrayList<>();
            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {
                EsDto esData = objectMapper.readValue(line, EsDto.class);
                if (esData.getYMD() == null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                    esData.setYMD(dateFormat.format(new Date()));
                }
                esDtoList.add(esData);
                count++;

                if (count % size == 0) {
                    saveAll(esDtoList);
                    esDtoList.clear();
                }
            }

            // 마지막에 남은 데이터가 있을 경우 저장
            if (!esDtoList.isEmpty()) {
                saveAll(esDtoList);
            }
        }
    }

    public void saveAll(List<EsDto> esDtoList) {
        esRepository.saveAll(esDtoList);
    }

    // ====================== 단순 조회 ======================

    public Iterable<EsDto> findAll() {
        return esRepository.findAll();
    }

    public Optional<EsDto> findById(String id) {
        return esRepository.findById(id);
    }

    public Optional<EsDto> findByAccountName(String account_name) {
        return esRepository.findByAccountName(account_name);
    }

    public List<EsDto> findByCodeAndValue(String code, double value) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(10)
                .query(q -> q
                        .nested(n -> n
                                .path("metrics.child.child")
                                .query(nq -> nq
                                        .bool(b -> b
                                                .must(mq -> mq
                                                        .term(t -> t
                                                                .field("metrics.child.child.code")
                                                                .value(code)
                                                        )
                                                )
                                                .must(mq -> mq
                                                        .term(t -> t
                                                                .field("metrics.child.child.value")
                                                                .value(value)
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .build();

        SearchResponse<EsDto> response = elasticsearchClient.search(searchRequest, EsDto.class);
        return response.hits().hits().stream()
                .map(hit -> {
                    EsDto dto = hit.source();
                    if (dto == null) {
                        throw new RuntimeException();
                    }
                    dto.setId(hit.id());  // elasticsearchClient 사용시 @id가 자동 맵핑되지 않음
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<String> getAllAccount() throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(100)
                .source(s -> s
                        .filter(f -> f
                                .includes("account_name")
                        )
                )
                .build();

        SearchResponse<EsDto> response = elasticsearchClient.search(searchRequest, EsDto.class);

        return response.hits().hits().stream()
                .map(Hit::source).filter(Objects::nonNull)
                .map(EsDto::getAccountName)
                .collect(Collectors.toList());
    }

    // account_name 값만 출력
    public List<EsDto> testMethod() throws IOException {
        String rawQuery = "{ \"match_all\": {} }";

        StringQuery searchQuery = new StringQuery(rawQuery, PageRequest.of(0, 50));
        searchQuery.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("account_name").build());
        return elasticsearchTemplate.search(searchQuery, EsDto.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }


    public List<Double> getValuesByCode(String code) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(10)
                .query(q -> q
                        .nested(n -> n
                                .path("metrics.child.child")
                                .query(nq -> nq
                                        .bool(b -> b
                                                .must(mq -> mq
                                                        .term(t -> t
                                                                .field("metrics.child.child.code")
                                                                .value(code)
                                                        )
                                                )
                                        )
                                ).innerHits(ih -> ih
                                        .name("values")
                                )
                        )
                )
                .source(s -> s
                        .filter(f -> f
                                .excludes("*")
                        )
                )
                .build();

        // inner_hits 값 사용할 거라 맵핑 불가. (Object 타입)
        SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Json 형식으로 변경 (SearchResponse 삭제)
            String jsonData = (response.toString().replaceFirst("^SearchResponse: ", ""));
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode hitsNode = rootNode.path("hits").path("hits");

            List<Double> values = new ArrayList<>();

            Iterator<JsonNode> elements = hitsNode.elements();
            while (elements.hasNext()) {
                JsonNode hitNode = elements.next();
                JsonNode innerHitsNode = hitNode.path("inner_hits").path("values").path("hits").path("hits");

                Iterator<JsonNode> innerHitsElements = innerHitsNode.elements();
                while (innerHitsElements.hasNext()) {
                    JsonNode innerHitNode = innerHitsElements.next();
                    JsonNode sourceNode = innerHitNode.path("_source");
                    double value = sourceNode.path("value").asDouble();
                    values.add(value);
                }
            }

            return values;
        } catch (IOException e) {
            throw new IOException();
        }
    }

    // ====================== 평균값 계산 ======================

    public double getStudentAverage(String accountName) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(0)
                .query(q -> q
                        .term(t -> t
                                .field("account_name")
                                .value(accountName)
                        )
                )
                .aggregations("metrics_child", a -> a
                        .nested(n -> n
                                .path("metrics.child.child")
                        )
                        .aggregations("average_value", a1 -> a1
                                .avg(avg -> avg
                                        .field("metrics.child.child.value")
                                )
                        )
                )
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        try {
            Aggregate metricsChildAggregation = response.aggregations().get("metrics_child");

            Aggregate avgAggregation = metricsChildAggregation.nested().aggregations().get("average_value");
            AvgAggregate avg = avgAggregation.avg();

            return avg.value();

        } catch (IllegalStateException e) {
            throw new IllegalStateException("오류 발생. 값이 존재하지 않거나 타입이 nested가 아닙니다.");
        }

    }

    public double getCodeAverage(String code) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(0)
                .aggregations("metrics_child", a -> a
                        .nested(n -> n
                                .path("metrics.child.child")
                        )
                        .aggregations("filtered_code", a1 -> a1
                                .filter(f -> f
                                        .term(t -> t
                                                .field("metrics.child.child.code.keyword")
                                                .value(code)
                                        )
                                )
                                .aggregations("average_value", a2 -> a2
                                        .avg(avg -> avg
                                                .field("metrics.child.child.value")
                                        )
                                )
                        )
                )
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        try {
            Aggregate metricsChildAggregation = response.aggregations().get("metrics_child");

            NestedAggregate nestedAgg = metricsChildAggregation.nested();
            Aggregate filteredAggregation = nestedAgg.aggregations().get("filtered_code");

            FilterAggregate filterAgg = filteredAggregation.filter();
            Aggregate avg = filterAgg.aggregations().get("average_value");

            return avg.avg().value();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("오류 발생. 값이 존재하지 않거나 타입이 nested가 아닙니다.");
        }
    }

    public double getWideCodeAverage(String code) throws IOException {
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index("students")
                .size(0)
                .aggregations("metrics_child", a -> a
                        .nested(n -> n
                                .path("metrics.child")
                        )
                        .aggregations("filter_code", a1 -> a1
                                .filter(f -> f
                                        .term(t -> t
                                                .field("metrics.child.code")
                                                .value(code)
                                        )
                                )
                                .aggregations("nested_child", a2 -> a2
                                        .nested(n2 -> n2
                                                .path("metrics.child.child")
                                        )
                                        .aggregations("average_value", a3 -> a3
                                                .avg(avg -> avg
                                                        .field("metrics.child.child.value")
                                                )
                                        )
                                )
                        )
                )
                .build();

        SearchResponse<Void> response = elasticsearchClient.search(searchRequest, Void.class);

        try {
            Aggregate metricsChildAggregation = response.aggregations().get("metrics_child");

            NestedAggregate nestedAgg = metricsChildAggregation.nested();
            Aggregate filterKcmathAggregation = nestedAgg.aggregations().get("filter_code");

            FilterAggregate filterAgg = filterKcmathAggregation.filter();
            Aggregate nestedChildAggregation = filterAgg.aggregations().get("nested_child");

            NestedAggregate nestedChildAgg = nestedChildAggregation.nested();
            int docCount = (int) nestedChildAgg.docCount(); // 평균 계산에 사용한 문서 수
            if (docCount == 0) { throw new IllegalStateException(); }
            Aggregate avg = nestedChildAgg.aggregations().get("average_value");

            return avg.avg().value();

        } catch (IllegalStateException e) {
            throw new IllegalStateException("오류 발생. 값이 존재하지 않거나 타입이 nested가 아닙니다.");
        }

    }

}
