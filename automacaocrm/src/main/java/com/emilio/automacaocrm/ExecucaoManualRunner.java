package com.emilio.automacaocrm;

import com.emilio.service.Automacao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class ExecucaoManualRunner implements CommandLineRunner {

    private final Automacao automacao;

    public ExecucaoManualRunner(Automacao automacao) {
        this.automacao = automacao;
    }

    @Override
    public void run(String... args) {
        // Só executa se o argumento --executar=true estiver presente
        boolean executar = Arrays.asList(args).contains("--executar=true");
        if (!executar) {
            return; // ignora durante build
        }

        try {
            System.out.println("==== INICIANDO EXECUÇÃO MANUAL ====");
            automacao.executarManualmente();
            System.out.println("==== EXECUÇÃO MANUAL FINALIZADA ====");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
