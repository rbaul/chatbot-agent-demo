package com.ai.chatbot.demo.controller;

import com.ai.chatbot.demo.tools.DemoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/private/ai-assistance")
@Slf4j
public class AiAssistanceController {

    private final ChatClient chatClient;
    private final ChatClient chatClientSpringDoc;
    private final ChatClient chatClientTools;

    private final VectorStore vectorStore;

    public AiAssistanceController(BedrockProxyChatModel chatModel, VectorStore vectorStore, DemoTools demoTools) {
        this.vectorStore = vectorStore;
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


    public record InternalMessageRequest(String system, String user) {
    }
}
