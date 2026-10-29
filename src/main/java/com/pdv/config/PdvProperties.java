package com.pdv.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pdv")
public class PdvProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Upload upload = new Upload();
    private Sync sync = new Sync();
    private Mercadopago mercadopago = new Mercadopago();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
        private long refreshExpirationMs;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }

    @Getter
    @Setter
    public static class Upload {
        private String maxFileSize;
        private String allowedTypes;
    }

    @Getter
    @Setter
    public static class Sync {
        private int batchSize = 100;
        private int maxRetries = 3;
    }

    @Getter
    @Setter
    public static class Mercadopago {
        // URL base da API — usada para montar a URL do webhook enviada ao MP
        private String webhookBaseUrl;
        // Tempo em minutos que o QR Code PIX fica válido
        private int expiracaoMinutos = 30;
    }
}
