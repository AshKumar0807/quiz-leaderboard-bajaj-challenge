package com.srm.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the frontend single-page application.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}
