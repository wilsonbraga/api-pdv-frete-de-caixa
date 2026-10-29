package com.pdv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class PdvApplication {

    public static void main(String[] args) {
    	
    	 // Gerar e mostrar senha Admin@123 ao iniciar wilson@mercadinhosojoao.com.br
        System.out.println("\n=========================================");
        System.out.println("🔑 HASH BCrypt: " + new BCryptPasswordEncoder(12).encode("Admin@123"));
        System.out.println("=========================================\n");
        
        SpringApplication.run(PdvApplication.class, args);
    }
    //https://claude.ai/chat/9f0699c6-1736-4051-bb46-cfab7adb5116
    // implemetar o pix mundovirtual https://claude.ai/chat/7955e874-34cb-4088-b98d-b3f3f3602870
}
