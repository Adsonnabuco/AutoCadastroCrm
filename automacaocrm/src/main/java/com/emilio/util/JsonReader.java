package com.emilio.util;

import com.emilio.model.Credenciais;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class JsonReader {
    public static Credenciais carregarCredenciais() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File("config.json"), Credenciais.class);
    }
}

