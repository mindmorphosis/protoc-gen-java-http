package my_package;

import org.springframework.web.bind.annotation.*;
import my_package.*;

interface b_Service {
        Pong function1 ( Ping ping );

        Pong function2 ( int aaa,Ping ping );

        Pong function3 ( int aaa,int bbb,Ping ping );

}

@ResponseBody
@RestController
public class tController {
    @Autowired
    b_Service b_service;

    @GetMapping("/ping11")
    public Pong function1 ( @RequestBody Ping ping ) {
        return b_service.function1( ping );
    }
    @PutMapping("/{aaa}")
    public Pong function2 ( @PathVariable int aaa,@RequestBody Ping ping ) {
        return b_service.function2( aaa,ping );
    }
    @DeleteMapping("/{aaa}/{bbb}")
    public Pong function3 ( @PathVariable int aaa,@PathVariable int bbb,@RequestBody Ping ping ) {
        return b_service.function3( aaa,bbb,ping );
    }

}