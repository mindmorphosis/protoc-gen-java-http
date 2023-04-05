package  my_package;

import org.springframework.web.bind.annotation.*;
import my_package;

interface a_Service {
    pong function1 ( ping ping );
    pong function2 ( ping ping );
    pong function3 ( ping ping );
}

interface b_Service {
    pong function1 ( ping ping );
    pong function2 ( ping ping );
    pong function3 ( ping ping );
}

@ResponseBody
@RestController
public class tController {
    @Autowired
    a_Service a;

    @Autowired
    b_Service b;

}
