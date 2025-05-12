package com.ai.chatbot.demo.tools;

import com.ai.chatbot.demo.AiTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ToolsRetriever implements AiTools {

    public static final String INFO_COMPLETION = "info-completion";

    public static final String DEFINITION = "definition";
    public static final String NAME = "name";

    @Qualifier("toolsStore")
    private final VectorStore toolsStore;

    @Lazy
    @Autowired
    private ToolCallbackResolver toolCallbackResolver;

    @Tool(name = INFO_COMPLETION, description = """
            Retrieves missing or incomplete information required by other tools.
                    Provide a contextual description of the missing data
                    and this tool will return the necessary details to proceed with the operation.
            """, returnDirect = false)
    public List<ToolDefinition> toolsRetrieval(@ToolParam(description = "Contextual information about the missing information") String context, ToolContext toolContext) {
        return getToolNamesByQuestion(context).stream().map(toolName -> {
                    try {
                        return toolCallbackResolver.resolve(toolName).getToolDefinition();
                    } catch (Exception e) {
                        log.error("Unable to resolve tool: {}", toolName, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();
    }

    public Set<String> getToolNamesByQuestion(String question) {
        return toolsStore.similaritySearch(SearchRequest.builder()
                        .query(question)
                        .similarityThreshold(0.5).build())
                .stream()
                .map(document -> document.getMetadata().get(NAME).toString())
                .collect(Collectors.toSet());
    }
}
