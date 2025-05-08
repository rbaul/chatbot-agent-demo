package com.ai.chatbot.demo.tools;

import com.ai.chatbot.demo.AiTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class ToolsRetriever implements AiTools {

    public static final String INFO_COMPLETION = "info-completion";

    @Qualifier("toolsStore")
    private final VectorStore toolsStore;

    @Tool(name = INFO_COMPLETION, description = """
            Retrieves missing or incomplete information required by other tools.
                    Provide a contextual description of the missing tool
                    and this tool will return the necessary details to proceed with the operation.
            """, returnDirect = false)
    public List<String> toolsRetrieval(@ToolParam(description = "Contextual information about the missing tool like `get ssh credential`") String context, ToolContext toolContext) {
        return toolsStore.similaritySearch(SearchRequest.builder().query(context).similarityThreshold(0.5).build())
                .stream()
                .map(document -> {
                    try {
                        return new ObjectMapper().writeValueAsString(document.getMetadata().get("definition"));
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).toList();
    }
}
