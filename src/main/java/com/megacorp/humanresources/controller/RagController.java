package com.megacorp.humanresources.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.megacorp.humanresources.service.RagService;

//Based on: https://github.com/xeraa/rag-with-java-springai-elasticsearch
@RestController
class RagController {

     private static final Logger log = LoggerFactory.getLogger(RagController.class);

     private final RagService rag;

     RagController(RagService rag) {
          this.rag = rag;
     }

     @PostMapping("/rag/ingest")
     ResponseEntity<?> ingestPDF(@RequestBody MultipartFile path) {
          log.debug("Entering ingestPDF with originalFilename={}", path.getOriginalFilename());
          rag.ingest(path.getResource());
          log.info("RAG ingest completed for originalFilename={}", path.getOriginalFilename());
          return ResponseEntity.ok().body("Done!");
     }

     @GetMapping("/rag/query")
     ResponseEntity<?> query(@RequestParam String question) {
          log.debug("Entering query with question={}", question);
          String response = rag.directRag(question);
          log.info("RAG direct query processed successfully");
          return ResponseEntity.ok().body(response);
     }

     @GetMapping("/rag/advised")
     ResponseEntity<?> advised(@RequestParam String question) {
          log.debug("Entering advised with question={}", question);
          String response = rag.advisedRag(question);
          log.info("RAG advised query processed successfully");
          return ResponseEntity.ok().body(response);
     }
}
