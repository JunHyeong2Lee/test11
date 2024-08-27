package elastictest.repository;

import elastictest.dto.EsDto;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsRepository extends ElasticsearchRepository<EsDto, String> {

    Optional<EsDto> findByAccountName(String account_name);

    //참고로 Query 어노테이션에서는 size 지정이 불가능.
    @Query("{\"nested\": {\"path\": \"metrics.child.child\", \"query\": {\"bool\": {\"must\": [{\"term\": {\"metrics.child.child.code\": \"?0\"}}, {\"term\": {\"metrics.child.child.value\": ?1}}]}}}}")
    List<EsDto> findByCodeAndValue(String code, double value);

}