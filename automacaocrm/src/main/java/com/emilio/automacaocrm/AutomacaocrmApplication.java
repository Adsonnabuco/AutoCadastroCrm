package com.emilio.automacaocrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita o suporte ao @Scheduled
@ComponentScan(basePackages = "com.emilio")
public class AutomacaocrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomacaocrmApplication.class, args);
	}
}