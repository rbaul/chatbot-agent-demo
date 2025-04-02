package com.ai.chatbot.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
public class ChatbotAgentDemoConfig {

    @Bean
    public VectorStore docStore(@Qualifier("embeddingModel") EmbeddingModel embeddingModel, TextSplitter textSplitter) throws IOException {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        ingestGeneral(vectorStore, textSplitter, new FileSystemResource("vector-store/spring.json"), new FileSystemResource("data/spring_boot_reference_guide.pdf"));
        return vectorStore;
    }

    @Bean
    public TextSplitter textSplitter() {
        return new TokenTextSplitter();
    }


    /**
     * General ingest to vector store
     */
    public static void ingestGeneral(VectorStore vectorStore, TextSplitter textSplitter, Resource saveLocation, Resource data) throws IOException {
        // Check if already loaded
        if (saveLocation.exists()) {
            File file = saveLocation.getFile();
            ((SimpleVectorStore) vectorStore).load(file);
            log.info("Vector store loaded from existing file: {}", file.getName());
            return;
        }

        // Ingest the document into the vector store if not created before
        List<Document> documents = textSplitter.transform(new PagePdfDocumentReader(data).read());
        vectorStore.write(documents);

        if (vectorStore instanceof SimpleVectorStore) {
            File vectorStoreFile = saveLocation.getFile();
            ((SimpleVectorStore) vectorStore).save(vectorStoreFile);
            log.info("Vector store contents written to {}", vectorStoreFile.getAbsolutePath());
        }

        log.info("Vector store loaded with {} documents", documents.size());
    }
}
