package com.emilio.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Automacao {

    WebDriver driver;
    String CHAVE_DE_ACESSO = "DB5TZFUO";
    String URL_CFMLIST = "https://sistemas.cfm.org.br/listamedicos/";
    String USUARIO = "adsonnabucoler";
    String SENHA = "30215331";

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
        // Adicionar opções para ambiente headless (se necessário)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");

        System.setProperty("webdriver.chrome.driver", "C:\\AutoCadastroCrm\\automacaocrm\\Driver\\chromedriver.exe");
        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    } //Ok

    public void acessaSiteCfmPreencheEFazDownload() throws InterruptedException {
        driver.navigate().to(URL_CFMLIST);
        Thread.sleep(500);
        try {
            Alert alert = driver.switchTo().alert();
            alert.accept();
        } catch (NoAlertPresentException e) {
            System.out.println("Nenhum alerta encontrado.");
        }

        WebElement labelChaveDeAcesso = driver.findElement(By.id("codigoAcesso"));
        labelChaveDeAcesso.sendKeys(CHAVE_DE_ACESSO);

        driver.findElement(By.className("loginBoxSubmit")).click();
    } //OK

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
                System.out.println("Download finalizado.");
                break;
            }
            Thread.sleep(1000);
            tempo++;
        }
        if (tempo >= timeoutSegundos) {
            System.out.println("Tempo esgotado esperando o download.");
        }
    } //OK

    private void fechaNavegadorAposDownload() {
        if (driver != null) {
            driver.quit();
        }
    } //OK


    public void descompactarZipsNaPasta(String pastaOrigem, String pastaDestino) throws IOException {
        File dirOrigem = new File(pastaOrigem);
        File[] arquivosZip = dirOrigem.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".zip"));

        if (arquivosZip == null || arquivosZip.length == 0) {
            System.out.println("Nenhum arquivo .zip encontrado na pasta.");
            return;
        }
        for (File zipFile : arquivosZip) {
            System.out.println("Descompactando: " + zipFile.getName());
            descompactarZip(zipFile.getAbsolutePath(), pastaDestino);
        }
    } //OK

    public static void descompactarZip(String caminhoZip, String pastaDestino) throws IOException {
        File destDir = new File("C:\\AutoCadastroCrm\\descompactado");
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(caminhoZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File novoArquivo = new File("C:\\AutoCadastroCrm\\descompactado", zipEntry.getName());
            if (zipEntry.isDirectory()) {
                novoArquivo.mkdirs();
            } else {
                new File(novoArquivo.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(novoArquivo);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    } //OK


    public void acessaShift() throws InterruptedException {
        String URL_SHIFTHOMOLOGACAO = "https://homologacao.atomosaude.com.br/main/auth/login?returnUrl=%2Fapp";
        System.setProperty("webdriver.chrome.driver", "C:\\AutoCadastroCrm\\automacaocrm\\Driver\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");

        driver = new ChromeDriver(options);
        driver.navigate().to(URL_SHIFTHOMOLOGACAO);
        driver.manage().window().maximize();

        try {
            Thread.sleep(300);
            WebElement inputUser = driver.findElement(By.xpath("/html/body/app-root/app-base-auth/div/div/div[1]/div/div[2]/app-login/nz-card/div/form/nz-form-item[1]/nz-form-control/div/div/sn-input-with-icon/nz-input-group/input"));
            inputUser.sendKeys(USUARIO);
            WebElement inputPassowrd = driver.findElement(By.xpath("/html/body/app-root/app-base-auth/div/div/div[1]/div/div[2]/app-login/nz-card/div/form/nz-form-item[2]/nz-form-control/div/div/nz-input-group/input"));
            inputPassowrd.sendKeys(SENHA);
            Thread.sleep(500);
            WebElement entrar = driver.findElement(By.cssSelector("button.ant-btn.ant-btn-primary"));
            entrar.click();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("frmNotificationsZen")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#messages a span.fas.fa-envelope")));
            driver.switchTo().defaultContent();

        } catch (InterruptedException e) {
            e.printStackTrace();
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
            Actions actions = new Actions(driver);
            actions.moveToElement(inputAcessoRapido)
                    .click()
                    .sendKeys("Importação de tabelas")
                    .build()
                    .perform();

            WebElement opcaoImportacaoDeTabelas = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@class='ant-select-item-option-content' and contains(text(), 'Importação de tabelas')]")));
            opcaoImportacaoDeTabelas.click();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }   //OK
    public void automacaoImportacaoDeTabelas() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMinutes(40));

        driver.switchTo().frame("frmContentZen");
        WebElement inputTabelas = driver.findElement(By.id("btn_17"));
        inputTabelas.click();

        WebElement opcao = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[text()='Solicitantes']")
        ));
        opcao.click();

        File pasta = new File("C:\\AutoCadastroCrm\\Processados");
        File pastaImportados = new File("C:\\AutoCadastroCrm\\importados");
        if (!pastaImportados.exists()) pastaImportados.mkdirs();

        File[] arquivosCsv = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (arquivosCsv == null || arquivosCsv.length == 0) {
            System.out.println("Nenhum arquivo CSV encontrado na pasta Processados.");
            return;
        }

        int totalImportados = 0;

        for (File csv : arquivosCsv) {
            try {
                // Mudar para o iframe do input file
                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");
                wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("iframe_18")));

                WebElement inputUpload = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("control_6")));

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].style.display='block';" +
                                "arguments[0].style.visibility='visible';" +
                                "arguments[0].style.opacity=1;",
                        inputUpload
                );

                wait.until(ExpectedConditions.elementToBeClickable(inputUpload));
                System.out.println("Enviando arquivo: " + csv.getAbsolutePath());
                inputUpload.sendKeys(csv.getAbsolutePath());

                Thread.sleep(1000);

                // Voltar ao iframe do botão de importação
                driver.switchTo().defaultContent();
                driver.switchTo().frame("frmContentZen");

                WebElement botaoImportar = wait.until(ExpectedConditions.elementToBeClickable(By.id("control_8")));
                botaoImportar.click();

                // Aguarda até o popup de progresso sumir (display: none)
                wait.until(driver1 -> {
                    try {
                        WebElement popup = driver1.findElement(By.id("grpPopupProgresso"));
                        String style = popup.getAttribute("style");
                        System.out.println("Style do progresso: " + style);
                        return style.contains("display: none");
                    } catch (Exception e) {
                        return false;
                    }
                });

                Thread.sleep(500);

                WebElement totalImportadosElemento = driver.findElement(By.id("control_24"));
                String textoImportados = totalImportadosElemento.getText();
                int qtd = extrairNumero(textoImportados);
                totalImportados += qtd;

                System.out.printf("Arquivo %s importado com sucesso. %d médicos.%n", csv.getName(), qtd);

                File destino = new File(pastaImportados, csv.getName());
                if (!csv.renameTo(destino)) {
                    System.err.printf("Erro ao mover o arquivo %s para a pasta 'importados'.%n", csv.getName());
                }

                Thread.sleep(2000);

            } catch (Exception e) {
                System.err.printf("Erro ao importar arquivo %s: %s%n", csv.getName(), e.getMessage());
            }
        }
    System.out.printf("Total geral de médicos importados: %d%n", totalImportados);
    }   //OK
    private int extrairNumero(String texto) {
        try {
            return Integer.parseInt(texto.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }   //OK

    public void fechaShiftAposImportacao() {
        driver.quit();
    }   //OK
}