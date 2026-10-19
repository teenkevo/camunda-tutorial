package com.example.authority.nira;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/nira/persons")
public class NiraController {

    private final NiraDirectory directory;

    public NiraController(NiraDirectory directory) {
        this.directory = directory;
    }

    @PostMapping("/lookup")
    public NiraPersonResponse lookup(@Valid @RequestBody NiraLookupRequest request) {
        return directory.find(request).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No NIRA person matches this national identification number and date of birth"
        ));
    }
}
