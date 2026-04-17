package com.fuorimondo.legal;

import com.fuorimondo.users.Locale;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Service
public class LegalService {

    private static final Set<String> ALLOWED_SLUGS = Set.of("cgu", "cgv", "privacy", "cookies", "mentions");

    public String getContent(String slug, Locale locale) {
        if (!ALLOWED_SLUGS.contains(slug)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "unknown slug");
        }
        String path = "legal/" + slug + "." + locale.name().toLowerCase() + ".md";
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "missing content");
        }
        try (var in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
