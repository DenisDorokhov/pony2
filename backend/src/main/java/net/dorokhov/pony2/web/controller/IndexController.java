package net.dorokhov.pony2.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping({
            "/",
            "/index.html",
            "/install/**",
            "/login/**",
            "/library/**",
    })
    public String index() {
        return "index.html";
    }
}
