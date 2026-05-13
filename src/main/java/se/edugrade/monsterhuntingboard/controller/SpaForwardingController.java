package se.edugrade.monsterhuntingboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping("/")
    public String forwardRootToIndex() {
        return "forward:/index.html";
    }

    @GetMapping({
            "/battle",
            "/battle/group/{huntId}",
            "/hunts/{huntId}/lobby"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
