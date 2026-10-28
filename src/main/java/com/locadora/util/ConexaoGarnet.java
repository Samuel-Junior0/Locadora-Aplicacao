package com.locadora.util;

import redis.clients.jedis.Jedis;

public final class ConexaoGarnet {

    private static final String HOST = System.getenv().getOrDefault("GARNET_HOST", "localhost");
    private static final int PORTA =
            Integer.parseInt(System.getenv().getOrDefault("GARNET_PORT", "6379"));

    private ConexaoGarnet() {
    }

    public static Jedis abrir() {
        return new Jedis(HOST, PORTA);
    }

    public static String host() {
        return HOST;
    }

    public static int porta() {
        return PORTA;
    }
}
