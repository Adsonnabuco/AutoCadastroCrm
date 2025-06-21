package com.emilio.automacaocrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita o suporte ao @Scheduled
public class AutomacaocrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomacaocrmApplication.class, args);
	}
}