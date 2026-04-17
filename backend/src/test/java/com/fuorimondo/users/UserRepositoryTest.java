package com.fuorimondo.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    UserRepository repository;

    @Test
    void saveAndFindByEmail() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Martin");
        user.setCivility(Civility.MRS);
        user.setCountry("France");
        user.setCity("Paris");
        user.setStatus(UserStatus.WAITING_LIST);
        user.setRole(UserRole.USER);
        user.setLocale(Locale.FR);

        repository.save(user);

        Optional<User> found = repository.findByEmailIgnoreCase("ALICE@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Alice");
    }

    @Test
    void emailIsUnique() {
        User a = baseUser("bob@example.com");
        User b = baseUser("bob@example.com");
        repository.save(a);

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> repository.saveAndFlush(b)
        );
    }

    private User baseUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setCivility(Civility.NONE);
        u.setCountry("FR");
        u.setCity("X");
        u.setStatus(UserStatus.WAITING_LIST);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        return u;
    }
}
