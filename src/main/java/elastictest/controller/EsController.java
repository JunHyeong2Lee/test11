package elastictest.controller;

import elastictest.dto.EsDto;
import elastictest.service.EsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@RestController // 모든 메소드가 ResponseBody가 되는 컨트롤러. view X!
@RequestMapping("/test")
public class EsController {

    private final EsService esService;

    @Autowired
    public EsController(EsService esService) {
        this.esService = esService;
    }

    @PostMapping("/saveNewData")
    public ResponseEntity<?> insertFile() throws IOException {

        File successFile = new File("src/main/resources/success.txt");

//        if(successFile.exists()) { // 파일 존재할 경우 메소드 진행 X
//
//            return new ResponseEntity<>("이미 데이터가 존재합니다.", HttpStatus.FORBIDDEN);
//
//        } else { // 파일 없을 경우 데이터 삽입 후 파일 생성.

            String file = "C:\\Users\\BLUE\\Documents\\20240621_02.jsonl";
            esService.insertJson(file,100);

            try (FileWriter fileWriter = new FileWriter(successFile)) {
                fileWriter.write("데이터가 성공적으로 입력되었습니다.");

            } catch (Exception e) {
                return new ResponseEntity<>("데이터 삽입 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>("", HttpStatus.OK);

//        }
    }

    //

    @GetMapping("/123123")
    public ResponseEntity<?> getEsData() throws IOException {
        return ResponseEntity.ok(esService.testMethod());
    }


    //

    // ====================== 특정 조건으로 정보값 찾기 ======================

    // id로 계정 찾기
    @GetMapping("/findById")
    public ResponseEntity<?> findById(@RequestParam(value = "id") String id) {
        Optional<EsDto> studentsOpts = esService.findById(id);
        EsDto students = studentsOpts.orElse(null);
        return ResponseEntity.ok(Objects.requireNonNullElse(students, "아이디가 존재하지 않습니다."));
    }

    // account_name으로 계정 찾기
    @GetMapping("/findByAccount")
    public ResponseEntity<?> findByAccountName(@RequestParam(value = "accountName") String accountName) {
        Optional<EsDto> studentsOpts = esService.findByAccountName(accountName);
        EsDto students = studentsOpts.orElse(null);
        return ResponseEntity.ok(Objects.requireNonNullElse(students, "아이디가 존재하지 않습니다."));
    }

    // 단순 조회하기

    @GetMapping("/allAccount")
    public ResponseEntity<?> allAccount() throws IOException {
        return  ResponseEntity.ok(Objects.requireNonNullElse(esService.getAllAccount(), "존재하지 않는 코드입니다."));
    }

    @GetMapping("/allCodeScore") // 특정 코드의 점수 쭉 긁어오기
    public ResponseEntity<?> getValuesByCode(@RequestParam(value = "code") String code) throws IOException {
        return ResponseEntity.ok(Objects.requireNonNullElse(esService.getValuesByCode(code), "존재하지 않는 코드입니다."));
    }

    @GetMapping("/findByCodeAndValue") // 특정 코드의 점수가 특정 점수인 학생 정보 긁어오기
    public ResponseEntity<?> findByCodeAndValue(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "value", required = true) Double value
    ) throws IOException {
        return ResponseEntity.ok(esService.findByCodeAndValue(code, value));
    }
    // ====================== 평균값 계산 ======================

    @GetMapping("/oneCodeAvg") // 특정 코드(child.child.code)의 평균
    public ResponseEntity<?> findSpecificAverage(@RequestParam(value = "code") String code) throws IOException {
        double specificAverage = esService.getCodeAverage(code);
        return ResponseEntity.ok(specificAverage);
    }

    @GetMapping("/allCodeAvg") // 특정 코드(child.code)의 평균
    public ResponseEntity<?> findSpecificCodeAverage(@RequestParam(value = "code") String code) throws IOException {
        double specificAverage = esService.getWideCodeAverage(code);
        return ResponseEntity.ok(specificAverage);
    }

    @GetMapping("/oneStudentAvg") // 특정 학생의 평균
    public ResponseEntity<?> findStudentAverage(@RequestParam(value = "account") String account) throws IOException {
        double avgScore = esService.getStudentAverage(account);
        return ResponseEntity.ok(avgScore);
    }

}
