package com.hsf302.bookingtour.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/", "/index", "/index.html"})
public class HomeController {

    @GetMapping
    public String home() {
        return "role-select";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/";
    }
}
