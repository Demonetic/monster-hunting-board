package se.edugrade.monsterhuntingboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping({
            "/{path:^(?!api$|assets$|actuator$|v3$)[^.]*}",
            "/{path:^(?!api$|assets$|actuator$|v3$)[^.]*}/**/{subpath:[^.]*}"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
