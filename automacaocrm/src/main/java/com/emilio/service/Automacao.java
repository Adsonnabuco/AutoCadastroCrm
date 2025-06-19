package com.emilio.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.system.SystemProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Automacao {

    WebDriver driver;
    String CHAVE_DE_ACESSO = "DB5TZFUO";
    String URL_CFMLIST = "https://sistemas.cfm.org.br/listamedicos/";

    public void iniciandowebDriver() throws InterruptedException {

        configurarChromeDriver();

        acessaSiteCfmPreencheEFazDownload();

        aguardarDownloadFinalizar("C:\\AutoCadastroCrm\\downloads", 60);

        fechaNavegadorAposDownload();

    }

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
    }



    private void fechaNavegadorAposDownload() {
        driver.close();
    }

    public void configurarChromeDriver() {
        HashMap<String, Object> chromePrefs = new HashMap<>();
        chromePrefs.put("download.default_directory", "C:\\AutoCadastroCrm\\downloads");
        chromePrefs.put("profile.default_content_settings.popups", 0);
        chromePrefs.put("download.prompt_for_download", false);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
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
            System.out.println("Nenhum alerta encontrado.");
        }

        WebElement labelChaveDeAcesso = driver.findElement(By.id("codigoAcesso"));
        labelChaveDeAcesso.sendKeys(CHAVE_DE_ACESSO);

        driver.findElement(By.className("loginBoxSubmit")).click();
    }

    public static void aguardarDownloadFinalizar(String pastaDownload, int timeoutSegundos) throws InterruptedException{
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
    }

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

    }
}
