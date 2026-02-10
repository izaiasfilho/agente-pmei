package zse.softease.agente_pmei.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({
        "/",
        "/dashboard",
        "/logs",
        "/config"
    })
    public String index() {
        return "forward:/index.html";
    }
}
