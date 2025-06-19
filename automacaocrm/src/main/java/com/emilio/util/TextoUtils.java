package com.emilio.util;

import java.text.Normalizer;

public class TextoUtils {

    public static String removerAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .replaceAll("ç", "c")
                .replaceAll("Ç", "C");
    }
}
