package com.kyoto.data.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class D1Config {

    @Value("${cloudflare.account.id}")
    private String accountId;

    @Value("${cloudflare.database.id}")
    private String databaseId;

    @Value("${cloudflare.api.token}")
    private String apiToken;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public String getApiToken() {
        return apiToken;
    }
}
