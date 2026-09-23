package com.artevia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.artevia.security.UserDetailsImpl;


@Controller
public class PageController {

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/auth/register")
    public String register() { return "auth/register"; }

    @GetMapping("/auth/login")
    public String login() { return "auth/login"; }

    @GetMapping("/home/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetailsImpl principal) {
        model.addAttribute("username", principal.getUser().getUsername());
        return "home/profile";
    }

    @GetMapping("/home/shop")
    public String shop() { return "home/shop"; }
}