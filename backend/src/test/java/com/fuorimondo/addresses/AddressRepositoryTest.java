package com.fuorimondo.addresses;

import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AddressRepositoryTest {

    @Autowired UserRepository userRepo;
    @Autowired AddressRepository addressRepo;

    @Test
    void findByUserAndType() {
        User user = createUser();
        userRepo.save(user);

        Address billing = newAddress(user, AddressType.BILLING, true);
        Address shipping = newAddress(user, AddressType.SHIPPING, true);
        addressRepo.saveAll(List.of(billing, shipping));

        List<Address> billings = addressRepo.findByUserIdAndType(user.getId(), AddressType.BILLING);
        assertThat(billings).hasSize(1);
        assertThat(billings.get(0).getType()).isEqualTo(AddressType.BILLING);
    }

    private User createUser() {
        User u = new User();
        u.setEmail("a@ex.com");
        u.setFirstName("A"); u.setLastName("B");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("X");
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        return u;
    }

    private Address newAddress(User u, AddressType type, boolean isDefault) {
        Address a = new Address();
        a.setUser(u);
        a.setType(type);
        a.setFullName("A B");
        a.setStreet("1 rue X");
        a.setPostalCode("75001");
        a.setCity("Paris");
        a.setCountry("FR");
        a.setDefault(isDefault);
        return a;
    }
}
