package com.rag.local.demo.controller;

import com.rag.local.demo.service.RagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Resource
    private RagService ragService;

    @PostMapping("/query")
    public String query(@RequestBody QueryRequest request) {
        return ragService.ragQa(request.getQuestion());
    }

    @PostMapping("/init")
    public boolean initKnowledgeBase() {
        return ragService.initKnowledgeBase();
    }

    public static class QueryRequest {
        private String question;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }
    }
}