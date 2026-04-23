package com.fuorimondo.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mollie")
public class MollieConfig {
    private String apiKey;
    private boolean enabled = true;
    private String apiBaseUrl = "https://api.mollie.com/v2";
    private String redirectBaseUrl;
    private String webhookBaseUrl;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getRedirectBaseUrl() { return redirectBaseUrl; }
    public void setRedirectBaseUrl(String redirectBaseUrl) { this.redirectBaseUrl = redirectBaseUrl; }
    public String getWebhookBaseUrl() { return webhookBaseUrl; }
    public void setWebhookBaseUrl(String webhookBaseUrl) { this.webhookBaseUrl = webhookBaseUrl; }
}
