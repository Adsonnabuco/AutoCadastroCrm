package com.emilio.automacaocrm;

import com.emilio.service.Automacao;
import com.emilio.service.ConversorTxtParaCsvService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class AutomacaocrmApplication {

	public static void main(String[] args) throws InterruptedException, IOException {
		SpringApplication.run(AutomacaocrmApplication.class, args);

		Automacao automacao = new Automacao();
		automacao.iniciandowebDriver();
		automacao.descompactarZipsNaPasta("C:\\AutoCadastroCrm\\downloads", "C:\\AutoCadastroCrm\\descompactado");

		ConversorTxtParaCsvService conversor = new ConversorTxtParaCsvService();
		conversor.processarArquivosTxt();

	}



}
