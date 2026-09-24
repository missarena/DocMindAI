package com.example.docmind.config;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ProjectConfig {

    /**
     * ChatClient configuration
     *
     * Spring AI automatically connects this ChatClient
     * to the configured Gemini model.
     */
//    @Bean
//    public ChatClient chatClient(
//            ChatClient.Builder builder,
//            ChatMemory memory
//    ) {
//
//        log.info(
//                "ChatMemory class name: {}",
//                memory.getClass().getName()
//        );
//
//        return builder
//
//                .defaultSystem("""
//                        You are DocMind, an intelligent, versatile,
//                        and friendly AI document intelligence assistant.
//
//                        Your Capabilities:
//
//                        1. Document-Grounded Q&A:
//                           When context from the user's uploaded documents
//                           is provided, prioritize and base your answer
//                           directly on that context.
//
//                           Cite document names and page numbers when
//                           they are available.
//
//                        2. General Knowledge & Conversation:
//                           If the user engages in general conversation,
//                           programming questions, mathematics,
//                           explanations, summaries, or general knowledge
//                           that may not be present in the uploaded documents,
//                           answer helpfully, accurately, and naturally.
//
//                        3. Hybrid Synthesis:
//                           If the document context partially covers a topic,
//                           synthesize the document facts with your broader
//                           knowledge to give a complete and useful answer.
//
//                        4. Handling Missing Context:
//                           If the provided document context does not contain
//                           enough information to answer a document-specific
//                           question, clearly say that the available documents
//                           do not provide enough information.
//
//                           Do not invent document facts.
//
//                        5. Tone & Format:
//                           Always be warm, professional, clear, and structured.
//
//                           Use Markdown:
//                           - Headings
//                           - Bullet points
//                           - Numbered lists
//                           - Bold text
//                           - Code blocks when appropriate
//
//                        Document Context:
//
//                        {doc_context}
//
//                        Produce markdown output.
//                        """)
//
//                .defaultAdvisors(
//                        new SimpleLoggerAdvisor(),
//                        MessageChatMemoryAdvisor
//                                .builder(memory)
//                                .build()
//                )
//
//                .build();
//    }


    /**
     * Swagger / OpenAPI configuration
     */
    @Bean
    public OpenAPI openAPI() {

        String securitySchemeName = "bearerAuth";

        return new OpenAPI()

                .info(
                        new Info()
                                .title(
                                        "DocMind — AI Document Intelligence & RAG Backend"
                                )
                                .description(
                                        "REST API for DocMind: " +
                                        "Multi-format document ingestion, " +
                                        "vector embeddings with PostgreSQL pgvector, " +
                                        "and hybrid conversational Q&A with Google Gemini."
                                )
                                .version("1.0.0")

                                .contact(
                                        new Contact()
                                                .name("Sadiya Technologies")
                                                .email(
                                                        "support@sadiyatechnologies.com"
                                                )

                                )
                )

                .components(
                        new Components()

                                .addSecuritySchemes(
                                        securitySchemeName,

                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(
                                                        SecurityScheme.Type.HTTP
                                                )
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                )

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(securitySchemeName)
                );
    }


    /**
     * ModelMapper configuration
     */
//    @Bean
//    public ModelMapper modelMapper() {
//        return new ModelMapper();
//    }
}