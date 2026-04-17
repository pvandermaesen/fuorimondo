package com.fuorimondo.admin;

import com.fuorimondo.users.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedEmail;
    private final String seedPassword;

    public AdminSeeder(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        @Value("${fuorimondo.seed.admin-email:admin@fuorimondo.local}") String seedEmail,
                        @Value("${fuorimondo.seed.admin-password:Admin!Password123}") String seedPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (userRepository.existsByEmailIgnoreCase(seedEmail)) {
            log.info("Admin seed: user {} already exists, skipping.", seedEmail);
            return;
        }
        User admin = new User();
        admin.setEmail(seedEmail);
        admin.setFirstName("Admin");
        admin.setLastName("Fuori");
        admin.setCivility(Civility.NONE);
        admin.setCountry("FR");
        admin.setCity("Paris");
        admin.setPasswordHash(passwordEncoder.encode(seedPassword));
        admin.setStatus(UserStatus.ALLOCATAIRE);
        admin.setRole(UserRole.ADMIN);
        admin.setLocale(Locale.FR);
        userRepository.save(admin);
        log.warn("\n=== SEED ADMIN CREATED ===\nEmail: {}\nPassword: {}\n===========================", seedEmail, seedPassword);
    }
}
