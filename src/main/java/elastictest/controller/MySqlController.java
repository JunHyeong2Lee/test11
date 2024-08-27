package elastictest.controller;

import elastictest.service.MySqlService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController // 모든 메소드가 ResponseBody가 되는 컨트롤러. view X!
@RequestMapping("/tmst")
public class MySqlController {

    private final MySqlService2 mySqlService;

    @Autowired
    public MySqlController(MySqlService2 mySqlService) {
        this.mySqlService = mySqlService;
    }

    @GetMapping("/test")
    public String test() throws IOException {
        String file = "C:\\Users\\BLUE\\Documents\\20240621_02.jsonl";
        mySqlService.insertJson(file, 1000);
        return "Hello!";
    }
}
