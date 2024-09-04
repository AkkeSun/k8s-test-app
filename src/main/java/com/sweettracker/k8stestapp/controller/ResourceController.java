package com.sweettracker.k8stestapp.controller;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ResourceController {

    @Value("${const.username}")
    private String username;

    private volatile boolean stopCpuOverload = false;
    private volatile boolean stopMemoryOverload = false;

    @GetMapping("/resource")
    public void getData() {
        log.info("hello - " + username);
    }

    @GetMapping("/cpu-overload")
    public void cpuOverload(boolean stop) {
        log.info("cpu-overload: " + stop);
        stopCpuOverload = stop;
        while (!stopCpuOverload) {
            Math.sqrt(Math.random());
        }
    }

    @GetMapping("/memory-overload")
    public void memoryOverload(boolean stop) {
        log.info("memory-overload: " + stop);
        stopMemoryOverload = stop;
        List<byte[]> memoryLeak = new ArrayList<>();

        while (!stopMemoryOverload) {
            memoryLeak.add(new byte[100 * 1024 * 1024]);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
