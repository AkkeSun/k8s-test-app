package com.sweettracker.k8stestapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProbeController {

    @GetMapping("/startup")
    public String startUp() {
        return "startUp";
    }

    @GetMapping("/readiness")
    public String readiness() {
        return "readiness";
    }

    @GetMapping("/liveness")
    public String liveness() {
        return "liveness";
    }
}