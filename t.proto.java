package my_package;

import org.springframework.web.bind.annotation.*;
import my_package.*;

interface b_Service {
        Pong function1 ( Ping ping );

        Pong function2 ( Ping ping );

        Pong function3 ( int aaa, Ping ping );

}

@ResponseBody
@RestController
public class tController {
    @Autowired
    b_Service;

    @GetMapping("/ping11")
    @PutMapping("/ping22")
    @DeleteMapping("/{aaa}")

}