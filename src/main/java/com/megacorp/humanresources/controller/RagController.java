package com.megacorp.humanresources.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.megacorp.humanresources.service.RagService;

//Based on: https://github.com/xeraa/rag-with-java-springai-elasticsearch
@RestController
class RagController {

     private final RagService rag;

     RagController(RagService rag) {
          this.rag = rag;
     }

     @PostMapping("/rag/ingest")
     ResponseEntity<?> ingestPDF(@RequestBody MultipartFile path) {
          rag.ingest(path.getResource());
          return ResponseEntity.ok().body("Done!");
     }

     @GetMapping("/rag/query")
     ResponseEntity<?> query(@RequestParam String question) {
          String response = rag.directRag(question);
          return ResponseEntity.ok().body(response);
     }

     @GetMapping("/rag/advised")
     ResponseEntity<?> advised(@RequestParam String question) {
          String response = rag.advisedRag(question);
          return ResponseEntity.ok().body(response);
     }
}
