package com.locadora.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private Json() {
    }

    public static String paraJson(Object valor) {
        try {
            return MAPPER.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Não foi possível serializar " + valor.getClass().getSimpleName(), e);
        }
    }

    public static <T> T paraObjeto(String json, Class<T> tipo) {
        try {
            return MAPPER.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "JSON inválido para " + tipo.getSimpleName() + ": " + json, e);
        }
    }
}
