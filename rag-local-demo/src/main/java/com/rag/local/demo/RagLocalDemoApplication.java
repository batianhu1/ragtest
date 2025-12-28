package com.rag.local.demo;

import com.rag.local.demo.service.RagService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.util.Scanner;

@SpringBootApplication
public class RagLocalDemoApplication implements CommandLineRunner {
    @Resource
    private RagService ragService;

    public static void main(String[] args) {
        SpringApplication.run(RagLocalDemoApplication.class, args);
    }

    // 项目启动后执行：初始化知识库并启动交互问答
    @Override
    public void run(String... args) throws Exception {
        System.out.println("开始初始化RAG本地知识库...");
        boolean initSuccess = ragService.initKnowledgeBase();
        if (!initSuccess) {
            System.err.println("RAG本地知识库初始化失败，程序退出");
            return;
        }
        System.out.println("\nRAG本地知识库初始化成功，输入问题即可查询（输入'quit'退出）：");
        // 控制台交互
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n请输入问题：");
            String query = scanner.nextLine();
            if ("quit".equalsIgnoreCase(query)) {
                System.out.println("退出问答");
                break;
            }
            if (query.trim().isEmpty()) {
                System.out.println("请输入有效的问题");
                continue;
            }
            // 执行问答
            String answer = ragService.ragQa(query);
            System.out.println("\n回答：" + answer);
        }
        scanner.close();
    }
}