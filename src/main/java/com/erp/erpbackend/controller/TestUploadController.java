package com.erp.erpbackend.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin
public class TestUploadController {

    // You already had something like this:
    @PostMapping("/upload")
    public Map<String, Object> testUpload(@RequestParam("file") MultipartFile file) throws IOException {
        return Map.of(
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "contentType", file.getContentType()
        );
    }

    // NEW: upload2 that matches the question endpoint pattern exactly
    @PostMapping("/upload2")
    public Map<String, Object> testUpload2(@RequestParam("assignmentId") String assignmentId,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        return Map.of(
                "assignmentId", assignmentId,
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "contentType", file.getContentType()
        );
    }
}
