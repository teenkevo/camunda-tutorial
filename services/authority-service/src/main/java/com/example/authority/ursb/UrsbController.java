package com.example.authority.ursb;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ursb/entities")
public class UrsbController {

    private final UrsbDirectory directory;

    public UrsbController(UrsbDirectory directory) {
        this.directory = directory;
    }

    @PostMapping("/lookup")
    public UrsbEntityResponse lookup(@Valid @RequestBody UrsbLookupRequest request) {
        return directory.find(request).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No URSB entity matches this registration number and registration date"
        ));
    }
}