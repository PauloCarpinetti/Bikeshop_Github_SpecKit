package com.bikeshop.common.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeilisearchConfig {

  @Bean
  public Client meilisearchClient(
      @Value("${meilisearch.host}") String host, @Value("${meilisearch.api-key}") String apiKey) {
    return new Client(new Config(host, apiKey));
  }
}
