package com.emilio.service;

import com.emilio.util.TextoUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;

public class ConversorTxtParaCsvService {

    private static final int LIMITE_LINHAS_POR_ARQUIVO = 60000;
    private static final String PASTA_ORIGEM = "C:\\AutoCadastroCrm\\descompactado";
    private static final String PASTA_DESTINO = "C:\\AutoCadastroCrm\\Processados";

    private static final String CABECALHO = "ID;ID LIS ANTIGO;NOME;DATA NASCIMENTO;SEXO (M/F/I/N);RG;CPF;STATUS (A/I);NÚMERO DO CRM;ESTADO DO CRM;CONSELHO (SIGLA);COD. ESPECIALIDADE;CEP;TIPO ENDEREÇO(R/C);ESTADO;PAÍS;ENDEREÇO;NÚMERO;BAIRRO;CIDADE;COMPLEMENTO;EMAIL;DDD;TELEFONE;DDD;TELEFONE CELULAR;OBSERVAÇÃO;NOTIFICA QUANDO SOLICITANTE (S/N);ENVIO DE LAUDO POR E-MAIL (S/N);TIPO DO ENVIO DO LAUDO (D/P/S/O);NACIONALIDADE";

    public void processarArquivosTxt() throws IOException {
        File pasta = new File(PASTA_ORIGEM);
        File[] arquivosTxt = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivosTxt == null || arquivosTxt.length == 0) {
            System.out.println("Nenhum arquivo .txt encontrado.");
            return;
        }

        List<String> linhasConvertidas = new ArrayList<>();
        int contadorArquivo = 1;
        int totalLinhas = 0;

        for(File arquivo : arquivosTxt) {
            try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                String linha;

                while ((linha = br.readLine()) != null) {
                    String[] campos = linha.split("!");

                    if (campos.length < 5) continue;

                    String crm = campos[0];
                    String uf = campos[1];
                    String nome = TextoUtils.removerAcentos(campos[2].trim());
                    String situacao = TextoUtils.removerAcentos(campos[4]).trim().toUpperCase();
                    String tipoInscricao = TextoUtils.removerAcentos(campos[3]).trim().toUpperCase();

                    if (!(situacao.equals("ATIVO") || situacao.equals("TRANSFERIDO"))) continue;
                    if (!(tipoInscricao.equals("PRINCIPAL") || tipoInscricao.equals("SECUNDARIA"))) continue;

                    String especialidades = campos.length >= 6 ? campos[5] :"";
                    String[] especialidadesSeparadas = especialidades.isBlank() ? new String[0] : especialidades.split(",");
                    String codigoEspecialidade = "1";
                    String observacoes = "";

                    if (especialidadesSeparadas.length > 0) {
                        codigoEspecialidade = EspecialidadeMapper.getCodigo(especialidadesSeparadas[0].trim());
                        observacoes = especialidades.trim(); // agora sempre usa todas
                    }

                    String linhaCsv = String.format(";;%s;;;;;A;%s;%s;CRM;%s;;;;;;;;;;;;;;;%s;N;N;P;BR",
                            nome, crm, uf, codigoEspecialidade, observacoes);
                    linhasConvertidas.add(linhaCsv);
                    totalLinhas++;

                    if (linhasConvertidas.size() >= LIMITE_LINHAS_POR_ARQUIVO) {
                        salvarCsv(linhasConvertidas, contadorArquivo++);
                        linhasConvertidas.clear();
                    }
                }
            }
        }

        if (!linhasConvertidas.isEmpty()) {
            salvarCsv(linhasConvertidas, contadorArquivo);
        }

        System.out.println("Total de linhas processadas: " + totalLinhas);
    }

    private void salvarCsv(List<String> linhas, int indice) throws IOException {
        File pastaDestino = new File(PASTA_DESTINO);
        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        String nomeArquivo = String.format("medicos_convertidos_parte%d.csv", indice);
        File arquivoCsv = new File(pastaDestino, nomeArquivo);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivoCsv), StandardCharsets.UTF_8))) {

            // ✅ Adiciona o BOM manualmente para evitar erros de leitura no Excel
            writer.write('\uFEFF');

            // ✅ Cabeçalho começa com ID (mantido conforme exigido)
            writer.write(CABECALHO);
            writer.newLine();

            for (String linha : linhas) {
                writer.write(linha);
                writer.newLine();
            }
        }

        System.out.println("Arquivo salvo: " + arquivoCsv.getAbsolutePath());
    }
}
