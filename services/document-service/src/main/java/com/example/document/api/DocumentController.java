package com.example.document.api;

import com.example.document.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping({"/api/documents", "/internal/documents"})
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.create(request);
    }

    @GetMapping({"/api/documents", "/internal/documents"})
    public List<DocumentResponse> list(
            @RequestParam(value = "documentType", required = false) String documentType
    ) {
        return documentService.list(documentType);
    }

    @GetMapping("/internal/documents/by-process/{processInstanceId}")
    public DocumentResponse byProcess(@PathVariable("processInstanceId") String processInstanceId) {
        return documentService.getByProcessInstanceId(processInstanceId);
    }

    @GetMapping({"/api/documents/{drn}", "/internal/documents/{drn}"})
    public DocumentResponse get(@PathVariable("drn") String drn) {
        return documentService.getByDrn(drn);
    }

    @PatchMapping({"/api/documents/{drn}", "/internal/documents/{drn}"})
    public DocumentResponse update(@PathVariable("drn") String drn, @RequestBody UpdateDocumentRequest request) {
        return documentService.update(drn, request);
    }

    @GetMapping({"/api/documents/{drn}/history", "/internal/documents/{drn}/history"})
    public List<DocumentEventResponse> history(@PathVariable("drn") String drn) {
        return documentService.history(drn);
    }
}
