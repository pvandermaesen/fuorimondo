package com.fuorimondo.legal;

import com.fuorimondo.users.Locale;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private final LegalService service;

    public LegalController(LegalService service) { this.service = service; }

    public record LegalResponse(String slug, Locale locale, String markdown) {}

    @GetMapping("/{slug}")
    public LegalResponse get(@PathVariable String slug,
                              @RequestParam(defaultValue = "FR") Locale locale) {
        return new LegalResponse(slug, locale, service.getContent(slug, locale));
    }
}
