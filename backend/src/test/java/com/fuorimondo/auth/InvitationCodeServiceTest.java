package com.fuorimondo.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationCodeServiceTest {

    @Test
    void generatedCodeHasRightLengthAndAllowedChars() {
        InvitationCodeService service = new InvitationCodeService(6, 90);
        for (int i = 0; i < 200; i++) {
            String code = service.generateCode();
            assertThat(code).hasSize(6);
            assertThat(code).matches("[A-HJ-KM-NP-Z2-9]+");
        }
    }

    @Test
    void codesHaveVariability() {
        InvitationCodeService service = new InvitationCodeService(6, 90);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) seen.add(service.generateCode());
        assertThat(seen.size()).isGreaterThan(90);
    }
}
