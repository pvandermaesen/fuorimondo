package com.fuorimondo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class StartupBanner {

    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    private final Environment env;

    public StartupBanner(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printBanner() {
        String appName = env.getProperty("spring.application.name", "application");
        String[] profiles = env.getActiveProfiles();
        String profile = profiles.length == 0 ? "default" : String.join(",", profiles);
        String protocol = "https".equalsIgnoreCase(env.getProperty("server.ssl.enabled", "false")) ? "https" : "http";
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        String dbUrl = env.getProperty("spring.datasource.url", "(n/a)");
        String dbUser = env.getProperty("spring.datasource.username", "(n/a)");
        String h2Console = Boolean.parseBoolean(env.getProperty("spring.h2.console.enabled", "false"))
                ? env.getProperty("spring.h2.console.path", "/h2-console")
                : null;
        String swaggerPath = env.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");

        String localBase = protocol + "://localhost:" + port + contextPath;
        String networkBase = protocol + "://" + host + ":" + port + contextPath;

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append("----------------------------------------------------------\n");
        sb.append("  ").append(appName).append(" - ready\n");
        sb.append("----------------------------------------------------------\n");
        sb.append("  Profile     : ").append(profile).append('\n');
        sb.append("  Local       : ").append(localBase).append('\n');
        sb.append("  Network     : ").append(networkBase).append('\n');
        sb.append("  API         : ").append(localBase).append("/api/\n");
        sb.append("  Swagger UI  : ").append(localBase).append(swaggerPath).append('\n');
        if (h2Console != null) {
            sb.append("  H2 console  : ").append(localBase).append(h2Console).append('\n');
        }
        sb.append("  DB URL      : ").append(dbUrl).append('\n');
        sb.append("  DB user     : ").append(dbUser).append('\n');
        sb.append("----------------------------------------------------------");

        log.info(sb.toString());
    }
}
