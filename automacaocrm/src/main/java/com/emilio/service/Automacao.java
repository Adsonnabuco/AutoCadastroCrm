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
import java.time.Duration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class Automacao {

    private static final Logger logger = LoggerFactory.getLogger(Automacao.class);
    WebDriver driver;
    String CHAVE_DE_ACESSO = "DB5TZFUO";
    String URL_CFMLIST = "https://sistemas.cfm.org.br/listamedicos/";
    String USUARIO = "adsonnabucoler";
    String SENHA = "30215331";

    @Scheduled(cron = "0 0 21 * * *")
    public void executarAutomacaoCompleta() {
        try {
            logger.info("=== INICIANDO ETAPA 1: DOWNLOAD DO CFM ===");
            iniciandowebDriver();

            logger.info("=== INICIANDO ETAPA 2: DESCOMPACTAÇÃO ===");
            descompactarZipsNaPasta("C:\\AutoCadastroCrm\\downloads", "C:\\AutoCadastroCrm\\descompactado");

            logger.info("=== INICIANDO ETAPA 3: CONVERSÃO TXT PARA CSV ===");
            ConversorTxtParaCsvService conversor = new ConversorTxtParaCsvService();
            conversor.processarArquivosTxt();

            logger.info("=== INICIANDO ETAPA 4: IMPORTAÇÃO NO SHIFT ===");
            acessaShift();

            logger.info("=== PROCESSO CONCLUÍDO COM SUCESSO! ===");
        } catch (Exception e) {
            logger.error("Erro na execução da automação: ", e);
        }
    }

    public void iniciandowebDriver() throws InterruptedException {
        configurarChromeDriver();
        acessaSiteCfmPreencheEFazDownload();
        aguardarDownloadFinalizar("C:\\AutoCadastroCrm\\downloads", 120);
        fechaNavegadorAposDownload();
    }

    public void configurarChromeDriver() {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", "C:\\AutoCadastroCrm\\downloads");
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.prompt_for_download", false);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--disable-extensions");

        System.setProperty("webdriver.chrome.driver", "C:\\AutoCadastroCrm\\automacaocrm\\Driver\\chromedriver.exe");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    public void acessaSiteCfmPreencheEFazDownload() throws InterruptedException {
        driver.navigate().to(URL_CFMLIST);
        Thread.sleep(500);
        try {
            Alert alert = driver.switchTo().alert();
            alert.accept();
        } catch (NoAlertPresentException e) {
            logger.info("Nenhum alerta encontrado.");
        }
        driver.findElement(By.id("codigoAcesso")).sendKeys(CHAVE_DE_ACESSO);
        driver.findElement(By.className("loginBoxSubmit")).click();
    }

    public static void aguardarDownloadFinalizar(String pastaDownload, int timeoutSegundos) throws InterruptedException {
        File dir = new File(pastaDownload);
        int tempo = 0;
        while (tempo < timeoutSegundos) {
            boolean downloadEmAndamento = false;
            for (File arquivo : dir.listFiles()) {
                if (arquivo.getName().endsWith(".tmp") || arquivo.getName().endsWith(".crdownload")) {
                    downloadEmAndamento = true;
                    break;
                }
            }
            if (!downloadEmAndamento) {
                logger.info("Download finalizado.");
                break;
            }
            Thread.sleep(1000);
            tempo++;
        }
        if (tempo >= timeoutSegundos) {
            logger.warn("Tempo esgotado esperando o download.");
        }
    }

    private void fechaNavegadorAposDownload() {
        if (driver != null) {
            driver.quit();
        }
    }

    public void descompactarZipsNaPasta(String pastaOrigem, String pastaDestino) throws IOException {
        File dirOrigem = new File(pastaOrigem);
        File[] arquivosZip = dirOrigem.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".zip"));
        if (arquivosZip == null || arquivosZip.length == 0) {
            logger.info("Nenhum arquivo .zip encontrado na pasta.");
            return;
        }
        for (File zipFile : arquivosZip) {
            logger.info("Descompactando: {}", zipFile.getName());
            descompactarZip(zipFile.getAbsolutePath(), pastaDestino);
        }
    }

    public static void descompactarZip(String caminhoZip, String pastaDestino) throws IOException {
        File destDir = new File(pastaDestino);
        if (!destDir.exists()) destDir.mkdirs();
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(caminhoZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File novoArquivo = new File(destDir, zipEntry.getName());
            if (zipEntry.isDirectory()) novoArquivo.mkdirs();
            else {
                new File(novoArquivo.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(novoArquivo);
                int len;
                while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public void acessaShift() throws InterruptedException {
        String URL_SHIFTHOMOLOGACAO = "https://homologacao.atomosaude.com.br/main/auth/login?returnUrl=%2Fapp";
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--disable-extensions");
        driver = new ChromeDriver(options);
        driver.navigate().to(URL_SHIFTHOMOLOGACAO);
        driver.manage().window().maximize();

        try {
            Thread.sleep(300);
            driver.findElement(By.xpath("//input[@type='text']")).sendKeys(USUARIO);
            driver.findElement(By.xpath("//input[@type='password']")).sendKeys(SENHA);
            Thread.sleep(500);
            driver.findElement(By.cssSelector("button.ant-btn.ant-btn-primary")).click();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("frmNotificationsZen")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#messages a span.fas.fa-envelope")));
            driver.switchTo().defaultContent();
        } catch (InterruptedException e) {
            logger.error("Erro ao acessar o Shift: ", e);
        }

        acessandoPaginaImportacaoDeTabela();
        automacaoImportacaoDeTabelas();
        fechaShiftAposImportacao();
    }

    public void acessandoPaginaImportacaoDeTabela() {
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
        } catch (InterruptedException e) {
            logger.error("Erro ao acessar página de importação: ", e);
        }
    }

    public void automacaoImportacaoDeTabelas() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(30));
        driver.switchTo().frame("frmContentZen");
        driver.findElement(By.id("btn_17")).click();
        WebElement opcao = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[text()='Solicitantes']")));
        opcao.click();

        File pasta = new File("C:\\AutoCadastroCrm\\Processados");
        File pastaImportados = new File("C:\\AutoCadastroCrm\\importados");
        if (!pastaImportados.exists()) pastaImportados.mkdirs();

        File[] arquivosCsv = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (arquivosCsv == null || arquivosCsv.length == 0) {
            logger.info("Nenhum arquivo CSV encontrado na pasta Processados.");
            return;
        }

        int totalImportados = 0;
        for (File csv : arquivosCsv) {
            try {
                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("iframe_18")));

                WebElement inputUpload = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("control_6")));
                ((JavascriptExecutor) driver).executeScript("arguments[0].style.display='block'; arguments[0].style.visibility='visible'; arguments[0].style.opacity=1;", inputUpload);
                wait.until(ExpectedConditions.elementToBeClickable(inputUpload));
                logger.info("Enviando arquivo: {}", csv.getAbsolutePath());
                inputUpload.sendKeys(csv.getAbsolutePath());
                Thread.sleep(1000);

                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");
                driver.findElement(By.id("control_8")).click();

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

                logger.info("Arquivo {} importado com sucesso. {} médicos.", csv.getName(), qtd);

                File destino = new File(pastaImportados, csv.getName());
                if (!csv.renameTo(destino)) logger.error("Erro ao mover o arquivo {} para a pasta 'importados'.", csv.getName());
                Thread.sleep(2000);
            } catch (Exception e) {
                logger.error("Erro ao importar arquivo {}: {}", csv.getName(), e.getMessage());
            }
        }
        logger.info("Total geral de médicos importados: {}", totalImportados);
    }

    private int extrairNumero(String texto) {
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    public void fechaShiftAposImportacao() {
        if (driver != null) driver.quit();
    }
}
