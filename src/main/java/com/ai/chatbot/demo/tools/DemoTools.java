package com.ai.chatbot.demo.tools;

import com.ai.chatbot.demo.AiTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DemoTools implements AiTools {

    @Tool(description = "Verify connection to ssh", returnDirect = false)
    public String verifyConnectionToSsh(@ToolParam(required = true, description = "SSH connection info") SshCredential sshCredential,
                                        ToolContext toolContext) {
        if(!StringUtils.hasText(sshCredential.user)) {
            throw new RuntimeException("Missed user name");
        }
        return "Connect to %s SSH is success".formatted(sshCredential.ip());
    }

//    @Tool(description = "Get SSH credential by IP", returnDirect = false)
//    public SshCredential getSshCredentialByIp(@ToolParam(description = "IPv4 address") String ip,
//                                              ToolContext toolContext) {
//        return new SshCredential(ip, "admin", "admin", "22");
//    }

    public record SshCredential(String ip, String user, String password, String port) {
    }

}
