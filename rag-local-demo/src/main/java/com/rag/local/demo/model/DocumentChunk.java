package com.rag.local.demo.model;

import lombok.Data;

@Data
public class DocumentChunk {
    private String content;
    private float[] embedding;
    private String sourceFile;
    private int position;

    public DocumentChunk() {}

    public DocumentChunk(String content, String sourceFile, int position) {
        this.content = content;
        this.sourceFile = sourceFile;
        this.position = position;
    }
}