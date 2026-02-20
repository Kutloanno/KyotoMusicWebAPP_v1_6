package com.kyoto.data.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "welcome";
    }


    @GetMapping("/userLogin")
    public String userLogin() {
        return "userLogin";
    }

    @GetMapping("/artistlogin")
    public String artistLoginPage() {
        return "artistlogin";
    }

    @GetMapping("/artistsignup")
    public String artistSignupPage() {
        return "artistsignup";
    }


    @GetMapping("/userSignup")
    public String showSignupPage() {
        return "userSignup";
    }






}
