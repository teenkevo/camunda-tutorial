package com.example.authority;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authorities")
public class AuthorityHealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "nira", "up",
                "ursb", "up"
        );
    }
}
