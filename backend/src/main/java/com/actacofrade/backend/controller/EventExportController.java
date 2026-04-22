package com.actacofrade.backend.controller;

import com.actacofrade.backend.dto.ExportRequest;
import com.actacofrade.backend.service.EventExportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventExportController {

    private final EventExportService eventExportService;

    public EventExportController(EventExportService eventExportService) {
        this.eventExportService = eventExportService;
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Integer id,
            @Valid @RequestBody ExportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        byte[] content = eventExportService.export(id, request, userDetails.getUsername());

        boolean isPdf = "PDF".equalsIgnoreCase(request.format());
        MediaType mediaType = isPdf ? MediaType.APPLICATION_PDF : new MediaType("text", "csv");
        String filename = "acto-" + id + (isPdf ? ".pdf" : ".csv");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(content);
    }
}
