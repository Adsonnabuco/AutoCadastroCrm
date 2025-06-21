package com.emilio.automacaocrm;

import com.emilio.service.Automacao;
import com.emilio.service.ConversorTxtParaCsvService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class AutomacaocrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomacaocrmApplication.class, args);

		try {
			// Instancia a classe de automação
			Automacao automacao = new Automacao();
//
			// Etapa 1: Acessa CFM, faz download e aguarda finalização
			System.out.println("=== INICIANDO ETAPA 1: DOWNLOAD DO CFM ===");
//			automacao.iniciandowebDriver();
//
			// Etapa 2: Descompacta os arquivos ZIP baixados
			System.out.println("=== INICIANDO ETAPA 2: DESCOMPACTAÇÃO ===");
//			automacao.descompactarZipsNaPasta("C:\\AutoCadastroCrm\\downloads", "C:\\AutoCadastroCrm\\descompactado");

			// Etapa 3: Converte arquivos TXT para CSV
			System.out.println("=== INICIANDO ETAPA 3: CONVERSÃO TXT PARA CSV ===");
//			ConversorTxtParaCsvService conversor = new ConversorTxtParaCsvService();
//			conversor.processarArquivosTxt();

			// Etapa 4: Acessa Shift e importa as tabelas
			System.out.println("=== INICIANDO ETAPA 4: IMPORTAÇÃO NO SHIFT ===");
			automacao.acessaShift();

			System.out.println("=== PROCESSO CONCLUÍDO COM SUCESSO! ===");

		} catch (InterruptedException e) {
			System.err.println("Erro de interrupção durante a execução: " + e.getMessage());
			e.printStackTrace();
//		} catch (IOException e) {
			System.err.println("Erro de I/O durante a execução: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Erro geral durante a execução: " + e.getMessage());
			e.printStackTrace();
		}
	}
}