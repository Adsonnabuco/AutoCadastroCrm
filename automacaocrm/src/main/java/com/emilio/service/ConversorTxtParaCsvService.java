package com.emilio.service;

import com.emilio.util.TextoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ConversorTxtParaCsvService {

    private static final Logger logger = LoggerFactory.getLogger(ConversorTxtParaCsvService.class);

    private static final int LIMITE_LINHAS_POR_ARQUIVO = 60000;
    private static final String PASTA_ORIGEM = "C:\\AutoCadastroCrm\\descompactado";
    private static final String PASTA_DESTINO = "C:\\AutoCadastroCrm\\processados";

    private static final String CABECALHO = "ID;ID LIS ANTIGO;NOME;DATA NASCIMENTO;SEXO (M/F/I/N);RG;CPF;STATUS (A/I);NÚMERO DO CRM;ESTADO DO CRM;CONSELHO (SIGLA);COD. ESPECIALIDADE;CEP;TIPO ENDEREÇO(R/C);ESTADO;PAÍS;ENDEREÇO;NÚMERO;BAIRRO;CIDADE;COMPLEMENTO;EMAIL;DDD;TELEFONE;DDD;TELEFONE CELULAR;OBSERVAÇÃO;NOTIFICA QUANDO SOLICITANTE (S/N);ENVIO DE LAUDO POR E-MAIL (S/N);TIPO DO ENVIO DO LAUDO (D/P/S/O);NACIONALIDADE\n";

    public void processarArquivosTxt() throws IOException {
        logger.info("=== INICIANDO PROCESSAMENTO DE ARQUIVOS TXT ===");

        File pasta = new File(PASTA_ORIGEM);

        // Verifica se a pasta existe
        if (!pasta.exists()) {
            logger.error("Pasta de origem não encontrada: {}", PASTA_ORIGEM);
            throw new IOException("Pasta de origem não encontrada: " + PASTA_ORIGEM);
        }

        // Cria pasta de destino se não existir
        File pastaDestino = new File(PASTA_DESTINO);
        if (!pastaDestino.exists()) {
            boolean criada = pastaDestino.mkdirs();
            logger.info("Pasta de destino criada: {} - Sucesso: {}", PASTA_DESTINO, criada);
        }

        File[] arquivosTxt = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivosTxt == null || arquivosTxt.length == 0) {
            logger.warn("Nenhum arquivo .txt encontrado na pasta: {}", PASTA_ORIGEM);
            return;
        }

        logger.info("Encontrados {} arquivos .txt para processamento", arquivosTxt.length);

        Map<String, List<String>> linhasPorUF = new HashMap<>();
        Map<String, Integer> totalPorUF = new HashMap<>();
        int totalLinhasProcessadas = 0;
        int totalArquivosProcessados = 0;

        for(File arquivo : arquivosTxt) {
            logger.info("Processando arquivo: {}", arquivo.getName());

            try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
                String linha;
                int linhasDoArquivo = 0;
                int linhasValidasDoArquivo = 0;

                while ((linha = br.readLine()) != null) {
                    linhasDoArquivo++;
                    String[] campos = linha.split("!");

                    if (campos.length < 5) {
                        logger.debug("Linha {} ignorada - campos insuficientes: {}", linhasDoArquivo, linha);
                        continue;
                    }

                    String crm = campos[0];
                    String uf = campos[1];
                    String nome = TextoUtils.removerAcentos(campos[2].trim());
                    String situacao = TextoUtils.removerAcentos(campos[4]).trim().toUpperCase();
                    String tipoInscricao = TextoUtils.removerAcentos(campos[3]).trim().toUpperCase();

                    if (!(situacao.equals("ATIVO") || situacao.equals("TRANSFERIDO"))) {
                        logger.debug("Linha {} ignorada - situação inválida: {}", linhasDoArquivo, situacao);
                        continue;
                    }

                    boolean tipoValido =
                            tipoInscricao.equals("PRINCIPAL") ||
                                    tipoInscricao.equals("SECUNDARIA") ||
                                    (tipoInscricao.equals("PROVISORIA") && situacao.equals("ATIVO"));

                    if (!tipoValido) {
                        logger.debug("Linha {} ignorada - tipo de inscrição inválido: {}", linhasDoArquivo, tipoInscricao);
                        continue;
                    }

                    String especialidades = campos.length >= 6 ? campos[5] :"";
                    String[] especialidadesSeparadas = especialidades.isBlank() ? new String[0] : especialidades.split(",");
                    String codigoEspecialidade = "1";
                    String observacoes = "";

                    if (especialidadesSeparadas.length > 0) {
                        codigoEspecialidade = EspecialidadeMapper.getCodigo(especialidadesSeparadas[0].trim());
                        observacoes = especialidades.trim();
                    }

                    String linhaCsv = String.format(";;%s;;;;;A;%s;%s;CRM;%s;;;;;;;;;;;;;;;%s;N;N;P;BR",
                            nome, crm, uf, codigoEspecialidade, observacoes);

                    linhasPorUF.computeIfAbsent(uf, k -> new ArrayList<>()).add(linhaCsv);
                    totalPorUF.put(uf, totalPorUF.getOrDefault(uf, 0) + 1);
                    linhasValidasDoArquivo++;
                    totalLinhasProcessadas++;
                }

                logger.info("Arquivo {} processado: {} linhas lidas, {} linhas válidas",
                        arquivo.getName(), linhasDoArquivo, linhasValidasDoArquivo);
                totalArquivosProcessados++;

            } catch (IOException e) {
                logger.error("Erro ao processar arquivo {}: {}", arquivo.getName(), e.getMessage(), e);
                throw e;
            }
        }

        logger.info("=== GERANDO ARQUIVOS CSV ===");
        int totalArquivosCsv = 0;

        for (Map.Entry<String, List<String>> entry : linhasPorUF.entrySet()) {
            String uf = entry.getKey();
            List<String> linhas = entry.getValue();
            int totalLinhas = linhas.size();
            int partes = (int) Math.ceil((double) totalLinhas / LIMITE_LINHAS_POR_ARQUIVO);

            logger.info("Gerando arquivos CSV para UF {}: {} registros em {} parte(s)", uf, totalLinhas, partes);

            for (int fromIndex = 0, parte = 1; fromIndex < totalLinhas; fromIndex += LIMITE_LINHAS_POR_ARQUIVO, parte++) {
                int toIndex = Math.min(fromIndex + LIMITE_LINHAS_POR_ARQUIVO, totalLinhas);
                List<String> subLista = linhas.subList(fromIndex, toIndex);
                salvarCsv(subLista, uf, parte);
                totalArquivosCsv++;
            }
        }

        // Log do resumo final
        logger.info("=== RESUMO DO PROCESSAMENTO ===");
        logger.info("Arquivos TXT processados: {}", totalArquivosProcessados);
        logger.info("Total de linhas processadas: {}", totalLinhasProcessadas);
        logger.info("Arquivos CSV gerados: {}", totalArquivosCsv);
        logger.info("Distribuição por UF:");

        totalPorUF.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> logger.info("  {}: {} registros", entry.getKey(), entry.getValue()));

        logger.info("=== PROCESSAMENTO CONCLUÍDO COM SUCESSO ===");
    }

    private void salvarCsv(List<String> linhas, String uf, int parte) {
        String nomeArquivo = String.format("%s\\medicos_%s_parte%d.csv", PASTA_DESTINO, uf, parte);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(nomeArquivo), Charset.forName("Windows-1252")))) {

            writer.write(CABECALHO);
            writer.newLine();

            for (String linha : linhas) {
                writer.write(linha);
                writer.newLine();
            }

            logger.info("Arquivo CSV gerado com sucesso: medicos_{}_parte{}.csv ({} registros)",
                    uf, parte, linhas.size());

        } catch (IOException e) {
            logger.error("Erro ao gerar arquivo CSV {}: {}", nomeArquivo, e.getMessage(), e);
            throw new RuntimeException("Erro ao salvar arquivo CSV: " + nomeArquivo, e);
        }
    }

    /**
     * Remove arquivos TXT da pasta descompactado após processamento bem-sucedido
     */
    public void limparArquivosTxtProcessados() {
        logger.info("Iniciando limpeza de arquivos TXT processados...");

        File pasta = new File(PASTA_ORIGEM);
        File[] arquivosTxt = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivosTxt == null || arquivosTxt.length == 0) {
            logger.info("Nenhum arquivo TXT encontrado para limpeza");
            return;
        }

        int arquivosRemovidos = 0;
        for (File arquivo : arquivosTxt) {
            try {
                if (arquivo.delete()) {
                    logger.info("Arquivo TXT removido: {}", arquivo.getName());
                    arquivosRemovidos++;
                } else {
                    logger.warn("Não foi possível remover o arquivo: {}", arquivo.getName());
                }
            } catch (Exception e) {
                logger.error("Erro ao remover arquivo {}: {}", arquivo.getName(), e.getMessage());
            }
        }

        logger.info("Limpeza concluída: {} arquivos TXT removidos", arquivosRemovidos);
    }
}