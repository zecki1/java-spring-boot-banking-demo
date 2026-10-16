package com.banking.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação Spring Boot
 * Equivalente a server.js no Node.js/Express
 * 
 * A anotação @SpringBootApplication:
 * - @Configuration: define esta como classe de configuração
 * - @EnableAutoConfiguration: ativa auto-configuração do Spring Boot
 * - @ComponentScan: escaneia pacotes procurando por beans (Component, Service, Repository)
 */
@SpringBootApplication
public class SpringBootDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class, args);
        System.out.println("🚀 Servidor Spring Boot rodando em http://localhost:3001");
        System.out.println("📚 Documentação de endpoints disponível em http://localhost:3001/actuator/health");
        System.out.println("🔧 Modo: " + System.getProperty("spring.profiles.active", "development"));
    }
}