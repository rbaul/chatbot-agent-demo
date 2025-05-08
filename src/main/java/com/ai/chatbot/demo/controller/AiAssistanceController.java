package com.ai.chatbot.demo.controller;

import com.ai.chatbot.demo.tools.DemoTools;
import com.ai.chatbot.demo.tools.ToolsRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/private/ai-assistance")
@Slf4j
public class AiAssistanceController {

    private final ChatClient chatClient;
    private final ChatClient chatClientSpringDoc;
    private final ChatClient chatClientTools;

    private final VectorStore vectorStore;
    private final ChatClient chatClientToolsDynamic;
    private final VectorStore toolsStore;

    private final ToolCallbackResolver toolCallbackResolver;

    public AiAssistanceController(BedrockProxyChatModel chatModel, @Qualifier("docStore") VectorStore vectorStore, @Qualifier("toolsStore") VectorStore toolsStore,
                                  DemoTools demoTools,ToolCallbackResolver toolCallbackResolver) {
        this.vectorStore = vectorStore;
        this.toolsStore = toolsStore;
        this.toolCallbackResolver = toolCallbackResolver;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.chatClientSpringDoc = ChatClient.builder(chatModel)
                .defaultSystem("""
                                Answer the user's question using the documentation that provided.
                                If the documentation above doesn’t contain the facts to answer the question, return with human sorry message.
                        """)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().similarityThreshold(0.3).build()), // RAG
                        new SimpleLoggerAdvisor()
                ).build();

        this.chatClientTools = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a helpful agent.
                        Your goal is to help the user with tools
                        You have tools to help you retrieve the relevant information.
                        You should choose the proper tool to use for each question.
                        """)
                .defaultOptions(ToolCallingChatOptions.builder()
                        .toolCallbacks(ToolCallbacks.from(demoTools))
//                        .internalToolExecutionEnabled(false)
                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.chatClientToolsDynamic = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are a helpful agent.
                        Your goal is to help the user with tools
                        You have tools to help you retrieve the relevant information.
                        You should choose the proper tool to use for each question.
                        """)
//                .defaultOptions(ToolCallingChatOptions.builder()
//                        .internalToolExecutionEnabled(false)
//                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @PostMapping
    public String generate(@RequestBody InternalMessageRequest internalMessageRequest) {
        ChatClient.ChatClientRequestSpec prompt = chatClient
                .prompt();
        if (StringUtils.hasText(internalMessageRequest.system())) {
            prompt.system(internalMessageRequest.system());
        }
        return prompt
                .user(internalMessageRequest.user())
                .call().content();
    }


    @PostMapping("/spring-boot-doc")
    public String generateFromDoc(@RequestBody String user) {
        return chatClientSpringDoc.prompt()
                .user(user)
                .call().content();
    }

    @PostMapping("/tool-agent")
    public String toolAgent(@RequestBody String user) {
        return chatClientTools.prompt()
                .user(user)
                .toolContext(Map.of("some_data", "data"))
                .call().content();
    }

    @PostMapping("/tool-agent-dynamic")
    public String toolAgentDynamic(@RequestBody String user) {
        Set<ToolCallback> names = toolsStore.similaritySearch(SearchRequest.builder().similarityThreshold(0.5).query(user).build()).stream()
                .map(document -> toolCallbackResolver.resolve(document.getMetadata().get("name").toString())).collect(Collectors.toSet());

        List<ToolCallback> tools = new ArrayList<>(names);
        tools.add(toolCallbackResolver.resolve(ToolsRetriever.INFO_COMPLETION));
        return chatClientToolsDynamic.prompt()
                .user(user)
                .toolCallbacks(tools)
                .toolContext(Map.of("some_data", "data"))
                .call().content();
    }


    public record InternalMessageRequest(String system, String user) {
    }
}
