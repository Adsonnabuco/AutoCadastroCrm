//package com.emilio.automacaocrm;
//
//import com.emilio.service.Automacao;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ExecucaoManualRunner implements CommandLineRunner {
//
//    private final Automacao automacao;
//
//    public ExecucaoManualRunner(Automacao automacao) {
//        this.automacao = automacao;
//    }
//
//    @Override
//    public void run(String... args) {
//        try {
//            System.out.println("==== INICIANDO EXECUÇÃO MANUAL ====");
//            automacao.executarManualmente();
//            System.out.println("==== EXECUÇÃO MANUAL FINALIZADA ====");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // Encerra a aplicação automaticamente após rodar
//        System.exit(0);
//    }
//}
