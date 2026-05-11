package com.kyoto.data.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import javax.net.ssl.*;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class R2Config {

    // These pull from our application.properties
    @Value("${r2.access-key}")
    private String ACCESS_KEY;

    @Value("${r2.secret-key}")
    private String SECRET_KEY;

    @Value("${r2.endpoint}")
    private String ENDPOINT;

    @Bean
    public S3Client s3Client() {
        // Kill SSL checks to avoid certificate headaches
        disableSslVerification();

        return S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT))
                // R2 likes "auto", but the SDK sometimes needs a valid string like "us-east-1"
                .region(Region.of("auto"))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .chunkedEncodingEnabled(false)
                                .build())
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    private static void disableSslVerification() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] c, String a) {}
                        public void checkServerTrusted(X509Certificate[] c, String a) {}
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());

            SSLContext.setDefault(sc);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Could not disable SSL verification", e);
        }
    }
}