package com.emilio.service;

import com.emilio.util.TextoUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;
import java.util.*;

public class ConversorTxtParaCsvService {

    private static final int LIMITE_LINHAS_POR_ARQUIVO = 60000;
    private static final String PASTA_ORIGEM = "C:\\AutoCadastroCrm\\descompactado";
    private static final String PASTA_DESTINO = "C:\\AutoCadastroCrm\\Processados";

    private static final String CABECALHO = "ID;ID LIS ANTIGO;NOME;DATA NASCIMENTO;SEXO (M/F/I/N);RG;CPF;STATUS (A/I);NÚMERO DO CRM;ESTADO DO CRM;CONSELHO (SIGLA);COD. ESPECIALIDADE;CEP;TIPO ENDEREÇO(R/C);ESTADO;PAÍS;ENDEREÇO;NÚMERO;BAIRRO;CIDADE;COMPLEMENTO;EMAIL;DDD;TELEFONE;DDD;TELEFONE CELULAR;OBSERVAÇÃO;NOTIFICA QUANDO SOLICITANTE (S/N);ENVIO DE LAUDO POR E-MAIL (S/N);TIPO DO ENVIO DO LAUDO (D/P/S/O);NACIONALIDADE\n";
//  private static final String CABECALHO = "ID,ID LIS ANTIGO,NOME,DATA NASCIMENTO,SEXO (M/F/I/N),RG,CPF,STATUS (A/I),NÚMERO DO CRM,ESTADO DO CRM,CONSELHO (SIGLA),COD. ESPECIALIDADE,CEP,TIPO ENDEREÇO(R/C),ESTADO,PAÍS,ENDEREÇO,NÚMERO,BAIRRO,CIDADE,COMPLEMENTO,EMAIL,DDD,TELEFONE,DDD,TELEFONE CELULAR,OBSERVAÇÃO,NOTIFICA QUANDO SOLICITANTE (S/N),ENVIO DE LAUDO POR E-MAIL (S/N),TIPO DO ENVIO DO LAUDO (D/P/S/O),NACIONALIDADE";

    public void processarArquivosTxt() throws IOException {
        File pasta = new File(PASTA_ORIGEM);
        File[] arquivosTxt = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivosTxt == null || arquivosTxt.length == 0) {
            System.out.println("Nenhum arquivo .txt encontrado.");
            return;
        }

        Map<String, List<String>> linhasPorUF = new HashMap<>();
        Map<String, Integer> totalPorUF = new HashMap<>();

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

                    boolean tipoValido =
                            tipoInscricao.equals("PRINCIPAL") ||
                                    tipoInscricao.equals("SECUNDARIA") ||
                                    (tipoInscricao.equals("PROVISORIA") && situacao.equals("ATIVO"));
                    if (!tipoValido) continue;

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
                    linhasPorUF.computeIfAbsent(uf, k -> new ArrayList<>()).add(linhaCsv);
                    totalPorUF.put(uf, totalPorUF.getOrDefault(uf, 0) + 1);
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : linhasPorUF.entrySet()) {
            String uf = entry.getKey();
            List<String> linhas = entry.getValue();
            int totalLinhas = linhas.size();
            int partes = (int) Math.ceil((double) totalLinhas / LIMITE_LINHAS_POR_ARQUIVO);

            for (int fromIndex = 0, parte = 1; fromIndex < totalLinhas; fromIndex += LIMITE_LINHAS_POR_ARQUIVO, parte++) {
                int toIndex = Math.min(fromIndex + LIMITE_LINHAS_POR_ARQUIVO, totalLinhas);
                List<String> subLista = linhas.subList(fromIndex, toIndex);
                salvarCsv(subLista, uf, parte);
            }

        }

        System.out.println("Total geral processado por UF:");
        totalPorUF.forEach((uf, total) -> System.out.printf("%s: %d linhas%n", uf, total));
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

                System.out.println("Arquivo CSV gerado: " + nomeArquivo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

}
