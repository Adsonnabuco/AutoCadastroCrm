package com.emilio.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Automacao {

    private static final Logger logger = LoggerFactory.getLogger(Automacao.class);

    // Configurações
    private WebDriver driver;

    private static final String CHAVE_DE_ACESSO = "DB5TZFUO";
    private static final String URL_CFMLIST = "https://sistemas.cfm.org.br/listamedicos/";
    private static final String USUARIO = "adsonnabucoler";
    private static final String SENHA = "@Br1l";
    private static final String URL_SHIFTPRODUCAO = "https://sistemalis.atomosaude.com.br/main/auth/login";

    // Diretórios. VM
    private static final String BASE_PATH = "C:\\AutoCadastroCrm";
    private static final String DOWNLOADS_DIR = BASE_PATH + "\\downloads";
    private static final String DESCOMPACTADO_DIR = BASE_PATH + "\\descompactado";
    private static final String PROCESSADOS_DIR = BASE_PATH + "\\Processados";
    private static final String IMPORTADOS_DIR = BASE_PATH + "\\importados";
    private static final String LOGS_DIR = BASE_PATH + "\\logs";

    // Local.
//    private static final String BASE_PATH = "C:\\AutomacaoCadastroCrm";
//    private static final String DOWNLOADS_DIR = BASE_PATH + "\\downloads";
//    private static final String DESCOMPACTADO_DIR = BASE_PATH + "\\descompactado";
//    private static final String PROCESSADOS_DIR = BASE_PATH + "\\Processados";
//    private static final String IMPORTADOS_DIR = BASE_PATH + "\\importados";
//    private static final String LOGS_DIR = BASE_PATH + "\\logs";

    private static final String DRIVER_PATH = BASE_PATH + "\\automacaocrm\\Driver\\chromedriver.exe";

    @Scheduled(cron = "0 0 21 * * *")
    //@Scheduled(fixedRate = 10000)
    public void executarAutomacaoCompleta() {
        String dataHoraInicio = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        logger.info("========================================");
        logger.info("=== INICIANDO AUTOMAÇÃO DIÁRIA ===");
        logger.info("Data/Hora: {}", dataHoraInicio);
        logger.info("========================================");

        try {
            // Verificar e criar diretórios necessários
            criarDiretorios();

            // Etapa 1: Download
            logger.info("=== ETAPA 1: DOWNLOAD DO CFM ===");
            executarDownload();

            // Etapa 2: Descompactação
            logger.info("=== ETAPA 2: DESCOMPACTAÇÃO ===");
            executarDescompactacao();

            // Etapa 3: Conversão TXT para CSV
            logger.info("=== ETAPA 3: CONVERSÃO TXT PARA CSV ===");
            executarConversao();

            // Etapa 4: Importação no Shift
            logger.info("=== ETAPA 4: IMPORTAÇÃO NO SHIFT ===");
            executarImportacao();

            String dataHoraFim = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            logger.info("========================================");
            logger.info("=== AUTOMAÇÃO CONCLUÍDA COM SUCESSO ===");
            logger.info("Horário de conclusão: {}", dataHoraFim);
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("ERRO CRÍTICO na execução da automação: ", e);
            // Cleanup em caso de erro
            fecharDriverSeguro();
        }
    }

    private void criarDiretorios() {
        String[] diretorios = {DOWNLOADS_DIR, DESCOMPACTADO_DIR, PROCESSADOS_DIR, IMPORTADOS_DIR, LOGS_DIR};

        for (String dir : diretorios) {
            try {
                Path path = Paths.get(dir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    logger.info("Diretório criado: {}", dir);
                } else {
                    logger.debug("Diretório já existe: {}", dir);
                }
            } catch (Exception e) {
                logger.error("Erro ao criar diretório {}: ", dir, e);
            }
        }
    }

    private void executarDownload() throws InterruptedException {
        logger.info("Iniciando processo de download...");

        try {
            limparPastaDownloads();
            configurarChromeDriver();
            acessaSiteCfmPreencheEFazDownload();
            aguardarDownloadFinalizar(DOWNLOADS_DIR, 300);

            File dirDownloads = new File(DOWNLOADS_DIR);
            File[] arquivosZip = dirDownloads.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".zip"));

            if (arquivosZip != null && arquivosZip.length > 0) {
                logger.info("Download concluído com sucesso - {} arquivo(s) ZIP encontrado(s)", arquivosZip.length);
            } else {
                throw new RuntimeException("Nenhum arquivo ZIP foi baixado");
            }

        } catch (Exception e) {
            logger.error("Erro durante o download: ", e);
            throw e;
        } finally {
            fecharDriverSeguro();
        }
    }

    private void executarDescompactacao() throws IOException {
        logger.info("Iniciando processo de descompactação...");

        limparPastaDescompactado();

        File dirOrigem = new File(DOWNLOADS_DIR);
        File[] arquivosZip = dirOrigem.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".zip"));

        if (arquivosZip == null || arquivosZip.length == 0) {
            logger.warn("Nenhum arquivo .zip encontrado na pasta de downloads");
            return;
        }

        int arquivosProcessados = 0;
        for (File zipFile : arquivosZip) {
            try {
                logger.info("Descompactando arquivo: {}", zipFile.getName());
                descompactarZip(zipFile.getAbsolutePath(), DESCOMPACTADO_DIR);

                if (zipFile.delete()) {
                    logger.info("✓ Arquivo ZIP removido com sucesso: {}", zipFile.getName());
                } else {
                    logger.warn("⚠ Não foi possível remover o arquivo ZIP: {}", zipFile.getName());
                }

                arquivosProcessados++;

            } catch (Exception e) {
                logger.error("Erro ao descompactar arquivo {}: ", zipFile.getName(), e);
            }
        }

        File dirDescompactado = new File(DESCOMPACTADO_DIR);
        File[] arquivosDescompactados = dirDescompactado.listFiles();
        int totalArquivos = arquivosDescompactados != null ? arquivosDescompactados.length : 0;

        logger.info("Descompactação concluída - {} ZIP(s) processado(s), {} arquivo(s) extraído(s)",
                arquivosProcessados, totalArquivos);
    }

    private void executarConversao() throws IOException {
        logger.info("Iniciando conversão de TXT para CSV...");

        File pastaDescompactado = new File(DESCOMPACTADO_DIR);
        File[] arquivosTxt = pastaDescompactado.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (arquivosTxt == null || arquivosTxt.length == 0) {
            logger.warn("Nenhum arquivo TXT encontrado para conversão");
            return;
        }

        logger.info("Encontrados {} arquivos TXT para conversão", arquivosTxt.length);

        try {
            ConversorTxtParaCsvService conversor = new ConversorTxtParaCsvService();
            conversor.processarArquivosTxt();

            File pastaProcessados = new File(PROCESSADOS_DIR);
            File[] arquivosCsv = pastaProcessados.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
            int csvGerados = arquivosCsv != null ? arquivosCsv.length : 0;

            if (csvGerados > 0) {
                logger.info("✓ Conversão bem-sucedida - {} arquivo(s) CSV gerado(s)", csvGerados);

                // Limpar pasta descompactado após conversão bem-sucedida
                limparPastaDescompactado();
            } else {
                logger.warn("⚠ Nenhum arquivo CSV foi gerado na conversão");
            }

        } catch (Exception e) {
            logger.error("Erro durante a conversão TXT para CSV: ", e);
            throw e;
        }
    }

    private void executarImportacao() throws InterruptedException {
        logger.info("Iniciando processo de importação no Shift...");

        // Verificar se há arquivos CSV para importar
        File pastaProcessados = new File(PROCESSADOS_DIR);
        File[] arquivosCsv = pastaProcessados.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (arquivosCsv == null || arquivosCsv.length == 0) {
            logger.warn("Nenhum arquivo CSV encontrado para importação");
            return;
        }

        logger.info("Encontrados {} arquivos CSV para importação", arquivosCsv.length);

        try {
            acessaShift();
            logger.info("✓ Importação no Shift concluída com sucesso");

        } catch (Exception e) {
            logger.error("Erro durante a importação no Shift: ", e);
            throw e;
        } finally {
            fecharDriverSeguro();
        }
    }

    private void limparPastaDownloads() {
        logger.debug("Limpando pasta de downloads...");
        limparPasta(DOWNLOADS_DIR, "downloads");
    }

    private void limparPastaDescompactado() {
        logger.debug("Limpando pasta descompactado...");
        limparPasta(DESCOMPACTADO_DIR, "descompactado");
    }

    private void limparPasta(String caminhoPasta, String nomePasta) {
        try {
            File pasta = new File(caminhoPasta);
            File[] arquivos = pasta.listFiles();

            if (arquivos != null && arquivos.length > 0) {
                int arquivosRemovidos = 0;
                int arquivosNaoRemovidos = 0;

                for (File arquivo : arquivos) {
                    if (arquivo.isFile()) {
                        if (arquivo.delete()) {
                            arquivosRemovidos++;
                            logger.debug("Arquivo removido da pasta {}: {}", nomePasta, arquivo.getName());
                        } else {
                            arquivosNaoRemovidos++;
                            logger.warn("Não foi possível remover arquivo da pasta {}: {}", nomePasta, arquivo.getName());
                        }
                    }
                }

                if (arquivosRemovidos > 0) {
                    logger.info("✓ Limpeza da pasta {} concluída - {} arquivo(s) removido(s)",
                            nomePasta, arquivosRemovidos);
                }

                if (arquivosNaoRemovidos > 0) {
                    logger.warn("⚠ {} arquivo(s) não puderam ser removidos da pasta {}",
                            arquivosNaoRemovidos, nomePasta);
                }
            } else {
                logger.debug("Pasta {} já está vazia", nomePasta);
            }
        } catch (Exception e) {
            logger.error("Erro ao limpar pasta {}: ", nomePasta, e);
        }
    }

    private void fecharDriverSeguro() {
        if (driver != null) {
            try {
                driver.quit();
                logger.debug("Driver fechado com sucesso");
            } catch (Exception e) {
                logger.warn("Erro ao fechar driver: ", e);
            } finally {
                driver = null;
            }
        }
    }

    public void configurarChromeDriver() {
        logger.debug("Configurando Chrome Driver...");

        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", DOWNLOADS_DIR);
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.prompt_for_download", false);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--disable-extensions");

        options.addArguments("--headless");

        System.setProperty("webdriver.chrome.driver", DRIVER_PATH);
        driver = new ChromeDriver(options);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        driver.manage().window().maximize();

        logger.debug("Chrome Driver configurado com sucesso (modo headless)");
    }

    public void acessaSiteCfmPreencheEFazDownload() throws InterruptedException {
        logger.info("Acessando site do CFM: {}", URL_CFMLIST);

        driver.navigate().to(URL_CFMLIST);
        Thread.sleep(500);

        try {
            Alert alert = driver.switchTo().alert();
            alert.accept();
            logger.debug("Alerta aceito no site do CFM");
        } catch (NoAlertPresentException e) {
            logger.debug("Nenhum alerta encontrado no site do CFM");
        }

        logger.debug("Preenchendo chave de acesso: {}", CHAVE_DE_ACESSO);
        driver.findElement(By.id("codigoAcesso")).sendKeys(CHAVE_DE_ACESSO);
        driver.findElement(By.className("loginBoxSubmit")).click();

        logger.info("✓ Download iniciado no site do CFM");
    }

    public static void aguardarDownloadFinalizar(String pastaDownload, int timeoutSegundos) throws InterruptedException {
        logger.info("Aguardando finalização do download... (timeout: {}s)", timeoutSegundos);

        File dir = new File(pastaDownload);
        int tempo = 0;
        boolean downloadIniciado = false;

        while (tempo < timeoutSegundos) {
            File[] arquivos = dir.listFiles();
            boolean temCrdownload = false;
            boolean temZip = false;

            if (arquivos != null) {
                for (File arquivo : arquivos) {
                    String nome = arquivo.getName().toLowerCase();
                    if (nome.endsWith(".crdownload")) {
                        temCrdownload = true;
                        downloadIniciado = true;
                    }
                    if (nome.endsWith(".zip")) {
                        temZip = true;
                    }
                }
            }

            if (downloadIniciado && temZip && !temCrdownload) {
                logger.info("✓ Download finalizado com sucesso");
                return;
            }

            if (tempo % 10 == 0 && tempo > 0) {
                logger.debug("Aguardando download... {}s transcorridos", tempo);
            }

            Thread.sleep(1000);
            tempo++;
        }

        logger.error("❌ TIMEOUT: Download não finalizou dentro do tempo esperado ({}s)", timeoutSegundos);
        throw new RuntimeException("Timeout no download");
    }
    public static void descompactarZip(String caminhoZip, String pastaDestino) throws IOException {
        File destDir = new File(pastaDestino);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        byte[] buffer = new byte[1024];
        int arquivosExtraidos = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(caminhoZip))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File novoArquivo = new File(destDir, zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    novoArquivo.mkdirs();
                } else {
                    // Criar diretórios pai se necessário
                    new File(novoArquivo.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(novoArquivo)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    arquivosExtraidos++;
                }
                zipEntry = zis.getNextEntry();
            }
        }
        logger.debug("✓ Extraídos {} arquivos do ZIP: {}", arquivosExtraidos, new File(caminhoZip).getName());
    }

    public void acessaShift() throws InterruptedException {
        logger.info("Acessando sistema Shift: {}", URL_SHIFTPRODUCAO);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--disable-extensions");

        options.addArguments("--headless");

        driver = new ChromeDriver(options);
        driver.navigate().to(URL_SHIFTPRODUCAO);
        driver.manage().window().maximize();

        try {
            Thread.sleep(1000);
            logger.debug("Realizando login no Shift...");

            driver.findElement(By.xpath("//input[@placeholder='Escreva seu usuário']")).sendKeys(USUARIO);
            driver.findElement(By.xpath("//input[@placeholder='Escreva sua senha']")).sendKeys(SENHA);
            Thread.sleep(700);
            driver.findElement(By.cssSelector("button.ant-btn.ant-btn-primary")).click();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("frmNotificationsZen")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#messages a span.fas.fa-envelope")));
            driver.switchTo().defaultContent();

            logger.info("✓ Login no Shift realizado com sucesso");

        } catch (Exception e) {
            logger.error("Erro ao fazer login no Shift: ", e);
            throw e;
        }

        acessandoPaginaImportacaoDeTabela();
        automacaoImportacaoDeTabelas();
    }

    public void acessandoPaginaImportacaoDeTabela() {
        logger.debug("Acessando página de importação de tabelas...");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement botaoMenu = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.ant-btn.ant-btn-primary.ant-btn-circle")));
        botaoMenu.click();

        WebElement inputAcessoRapido = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@placeholder='Acesso Rápido']")));

        try {
            Thread.sleep(500);
            new Actions(driver).moveToElement(inputAcessoRapido).click().sendKeys("Importação de tabelas").build().perform();
            WebElement opcaoImportacao = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(), 'Importação de tabelas')]")));
            opcaoImportacao.click();
            Thread.sleep(500);

            logger.debug("✓ Página de importação acessada com sucesso");

        } catch (InterruptedException e) {
            logger.error("Erro ao acessar página de importação: ", e);
            throw new RuntimeException(e);
        }
    }

    public void automacaoImportacaoDeTabelas() {
        logger.info("Iniciando automação de importação de tabelas...");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(30));
        driver.switchTo().frame("frmContentZen");
        driver.findElement(By.id("btn_17")).click();
        WebElement opcao = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Solicitantes']")));
        opcao.click();

        File pasta = new File(PROCESSADOS_DIR);
        File pastaImportados = new File(IMPORTADOS_DIR);
        if (!pastaImportados.exists()) {
            pastaImportados.mkdirs();
            logger.info("Pasta de importados criada: {}", IMPORTADOS_DIR);
        }

        File[] arquivosCsv = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (arquivosCsv == null || arquivosCsv.length == 0) {
            logger.warn("Nenhum arquivo CSV encontrado na pasta Processados");
            return;
        }

        logger.info("Iniciando importação de {} arquivos CSV", arquivosCsv.length);

        int totalImportados = 0;
        int arquivosProcessados = 0;
        int arquivosComErro = 0;

        for (File csv : arquivosCsv) {
            try {
                logger.info("Processando arquivo ({}/{}): {}", ++arquivosProcessados, arquivosCsv.length, csv.getName());

                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("iframe_18")));

                WebElement inputUpload = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("control_6")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block'; arguments[0].style.visibility='visible'; arguments[0].style.opacity=1;", inputUpload);
                wait.until(ExpectedConditions.elementToBeClickable(inputUpload));

                logger.debug("Fazendo upload do arquivo: {}", csv.getAbsolutePath());
                inputUpload.sendKeys(csv.getAbsolutePath());
                Thread.sleep(1000);

                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");
                driver.findElement(By.id("control_8")).click();

                logger.debug("Aguardando processamento do arquivo...");
                wait.until(d -> {
                    try {
                        WebElement popup = d.findElement(By.id("grpPopupProgresso"));
                        return popup.getAttribute("style").contains("display: none");
                    } catch (Exception e) {
                        return false;
                    }
                });

                Thread.sleep(500);
                String textoImportados = driver.findElement(By.id("control_24")).getText();
                int qtd = extrairNumero(textoImportados);
                totalImportados += qtd;

                logger.info("✓ Arquivo {} importado: {} registros", csv.getName(), qtd);

                File destino = new File(pastaImportados, csv.getName());
                if (csv.renameTo(destino)) {
                    logger.debug("Arquivo movido para pasta importados: {}", csv.getName());
                } else {
                    logger.error("Erro ao mover arquivo {} para pasta importados", csv.getName());
                }
                Thread.sleep(2000);
            } catch (Exception e) {
                arquivosComErro++;
                logger.error("❌ Erro ao importar arquivo {}: ", csv.getName(), e);
            }
        }
        logger.info("========================================");
        logger.info("=== RESUMO DA IMPORTAÇÃO ===");
        logger.info("Arquivos processados: {}", arquivosProcessados);
        logger.info("Arquivos com sucesso: {}", arquivosProcessados - arquivosComErro);
        logger.info("Arquivos com erro: {}", arquivosComErro);
        logger.info("Total de registros importados: {}", totalImportados);
        logger.info("========================================");
    }

    private int extrairNumero(String texto) {
        try {
            String numero = texto.replaceAll("[^0-9]", "");
            return numero.isEmpty() ? 0 : Integer.parseInt(numero);
        } catch (Exception e) {
            logger.warn("Erro ao extrair número do texto: {}", texto);
            return 0;
        }
    }

    public void executarManualmente() {
        logger.info("EXECUÇÃO MANUAL da automação solicitada");
        executarAutomacaoCompleta();
    }

    public void testarEtapa(String etapa) {
        logger.info("=== TESTE DA ETAPA: {} ===", etapa.toUpperCase());

        try {
            criarDiretorios();

            switch (etapa.toLowerCase()) {
                case "download":
                    executarDownload();
                    break;
                case "descompactacao":
                    executarDescompactacao();
                    break;
                case "conversao":
                    executarConversao();
                    break;
                case "importacao":
                    executarImportacao();
                    break;
                default:
                    logger.warn("Etapa '{}' não reconhecida. Etapas disponíveis: download, descompactacao, conversao, importacao", etapa);
            }
            logger.info("=== TESTE DA ETAPA {} CONCLUÍDO ===", etapa.toUpperCase());
        } catch (Exception e) {
            logger.error("Erro no teste da etapa {}: ", etapa, e);
        }
    }
}