package elastictest.dao;

import elastictest.dto.StdntDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface MySqlMapper {

    void insertAll(List<StdntDto> list);

    StdntDto findByKcCode(String kcCode);

    List<String> findLectureCode(Map<String, Object> params);

}
