# Socle — Plan A : Backend & API complète — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Livrer un backend Spring Boot complet et testable pour le Socle FuoriMondo : authentification (activation par code, waiting list, login, reset password), profils avec trois tiers, adresses, pages légales, back-office admin minimal. Tous les endpoints REST sont consommables et couverts par des tests d'intégration.

**Architecture:** Monorepo, module `backend/` en Spring Boot 3.x + Java 21 + Maven. Un seul module, packages par domaine (`auth`, `users`, `addresses`, `admin`, `legal`, `email`). H2 file-based en dev, Postgres en prod, H2 in-memory en test. Sécurité par session Spring Session (JDBC) + CSRF, cookie `HttpOnly` `SameSite=Strict`. Flyway pour les migrations. Interface `EmailSender` avec implémentation console en dev.

**Tech Stack:**
- Spring Boot 3.3.x, Java 21, Maven
- Spring Security 6, Spring Session JDBC
- Spring Data JPA, Hibernate
- Flyway, H2 (dev + test), PostgreSQL (prod)
- JUnit 5, Spring Boot Test, Testcontainers (pour Postgres en test si besoin plus tard)
- springdoc-openapi (documentation)
- Bucket4j (rate limiting)
- Validation API (jakarta.validation)

**Référence spec :** `docs/superpowers/specs/2026-04-17-socle-design.md`

**Conventions :**
- DRY, YAGNI, TDD
- Un commit par tâche (format conventional commits : `feat:`, `test:`, `chore:`, `docs:`, `fix:`)
- Tests d'intégration Spring Boot Test avec profil `test` (H2 in-memory)
- Packages : `com.fuorimondo.<domain>` sous `src/main/java/com/fuorimondo/`

---

## Phase 1 — Bootstrap du projet

### Task 1 : Structure initiale du monorepo

**Files:**
- Create: `backend/` (dossier)
- Create: `.gitignore`
- Create: `README.md`

- [ ] **Step 1: Créer l'arborescence de base**

Depuis la racine du repo (`FuoriMondo/`) :
```bash
mkdir -p backend/src/main/java/com/fuorimondo
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/fuorimondo
mkdir -p backend/src/test/resources
```

- [ ] **Step 2: Créer `.gitignore` à la racine**

Fichier `FuoriMondo/.gitignore` :
```
# OS
.DS_Store
Thumbs.db

# IDE
.idea/
.vscode/
*.iml
.settings/
.project
.classpath

# Java / Maven
backend/target/
*.class

# H2 dev data
backend/data/

# Node
frontend/node_modules/
frontend/dist/
frontend/.vite/

# Logs
*.log
```

- [ ] **Step 3: Créer un README minimal**

Fichier `FuoriMondo/README.md` :
```markdown
# FuoriMondo

Application web mobile-first pour la marque Fuori Marmo.

## Structure

- `backend/` — Spring Boot (Maven)
- `frontend/` — Vue 3 (à venir)
- `docs/` — specs, plans
- `ressources/` — design, analyse fonctionnelle

## Backend

Voir `backend/README.md`.
```

- [ ] **Step 4: Commit**

```bash
git add .gitignore README.md backend/
git commit -m "chore: initial monorepo structure with backend skeleton"
```

---

### Task 2 : pom.xml Maven avec dépendances

**Files:**
- Create: `backend/pom.xml`

- [ ] **Step 1: Créer le pom.xml**

Fichier `backend/pom.xml` :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.fuorimondo</groupId>
    <artifactId>fuorimondo-backend</artifactId>
    <version>0.1.0-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>
        <bucket4j.version>8.10.1</bucket4j.version>
        <springdoc.version>2.6.0</springdoc.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Vérifier que Maven résout bien les dépendances**

```bash
cd backend && mvn dependency:resolve -q
```

Expected : commande se termine sans erreur (téléchargement des jars).

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "chore: add Maven pom.xml with Spring Boot 3.3 dependencies"
```

---

### Task 3 : Classe main + profils Spring

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/application-dev.yml`
- Create: `backend/src/main/resources/application-prod.yml`
- Create: `backend/src/main/resources/application-test.yml`

- [ ] **Step 1: Créer la classe main**

Fichier `backend/src/main/java/com/fuorimondo/FuoriMondoApplication.java` :
```java
package com.fuorimondo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FuoriMondoApplication {
    public static void main(String[] args) {
        SpringApplication.run(FuoriMondoApplication.class, args);
    }
}
```

- [ ] **Step 2: Config commune**

Fichier `backend/src/main/resources/application.yml` :
```yaml
spring:
  application:
    name: fuorimondo-backend
  profiles:
    active: dev
  jpa:
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB

server:
  port: 8080
  servlet:
    session:
      timeout: 7d
      cookie:
        name: FMSESSION
        http-only: true
        same-site: strict
        # secure: true  (activé en prod uniquement)

fuorimondo:
  invitation-code:
    length: 6
    expiration-days: 90
  password-reset:
    expiration-hours: 1
  rate-limit:
    login-attempts-per-15min: 5
    activation-attempts-per-15min: 5
    reset-attempts-per-15min: 5

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 3: Profil dev**

Fichier `backend/src/main/resources/application-dev.yml` :
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/fuorimondo-dev;AUTO_SERVER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password: ''
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  h2:
    console:
      enabled: true
      path: /h2-console
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: always

logging:
  level:
    com.fuorimondo: DEBUG
    org.springframework.security: INFO
```

- [ ] **Step 4: Profil prod**

Fichier `backend/src/main/resources/application-prod.yml` :
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/fuorimondo}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  session:
    store-type: jdbc

server:
  servlet:
    session:
      cookie:
        secure: true

logging:
  level:
    com.fuorimondo: INFO
```

- [ ] **Step 5: Profil test**

Fichier `backend/src/main/resources/application-test.yml` :
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:fuorimondo-test;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ''
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  session:
    store-type: none

logging:
  level:
    com.fuorimondo: DEBUG
```

- [ ] **Step 6: Vérifier que le projet démarre (sans migration, donc erreur attendue sur Flyway)**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected : erreur Flyway ou startup réussi mais Flyway ne fait rien (aucune migration). C'est OK, on ajoute les migrations dans la prochaine tâche.

Arrêter avec Ctrl+C.

- [ ] **Step 7: Commit**

```bash
git add backend/src/
git commit -m "chore: add Spring Boot main class and profile configs (dev/prod/test)"
```

---

## Phase 2 — Migrations Flyway & entités JPA

### Task 4 : Migration Flyway V1 (schéma complet)

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `backend/src/main/resources/db/migration/V2__spring_session.sql`

- [ ] **Step 1: Créer la migration principale**

Fichier `backend/src/main/resources/db/migration/V1__init_schema.sql` :
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    civility VARCHAR(20) NOT NULL,
    birth_date DATE,
    phone VARCHAR(30),
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    tier_code VARCHAR(20),
    referrer_info TEXT,
    locale VARCHAR(5) NOT NULL DEFAULT 'FR',
    admin_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

CREATE TABLE addresses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    street VARCHAR(255) NOT NULL,
    street_extra VARCHAR(255),
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_addresses_user_type ON addresses(user_id, type);

CREATE TABLE invitation_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    code VARCHAR(10) NOT NULL UNIQUE,
    generated_at TIMESTAMP NOT NULL,
    generated_by UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_invitation_codes_code ON invitation_codes(code);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens(user_id);
```

- [ ] **Step 2: Migration Spring Session (schéma JDBC)**

Fichier `backend/src/main/resources/db/migration/V2__spring_session.sql` :
```sql
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BLOB NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID) REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
);
```

- [ ] **Step 3: Vérifier que l'app démarre et applique les migrations**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected : logs Flyway affichent `Migrating schema "PUBLIC" to version "1 - init schema"` puis version 2. Startup réussi. Arrêter avec Ctrl+C.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway V1 schema (users/addresses/codes/tokens) and V2 Spring Session tables"
```

---

### Task 5 : Entité commune BaseEntity + enums

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/common/BaseEntity.java`
- Create: `backend/src/main/java/com/fuorimondo/users/UserStatus.java`
- Create: `backend/src/main/java/com/fuorimondo/users/UserRole.java`
- Create: `backend/src/main/java/com/fuorimondo/users/Civility.java`
- Create: `backend/src/main/java/com/fuorimondo/users/Locale.java`
- Create: `backend/src/main/java/com/fuorimondo/users/TierCode.java`
- Create: `backend/src/main/java/com/fuorimondo/addresses/AddressType.java`

- [ ] **Step 1: BaseEntity avec id et timestamps**

Fichier `backend/src/main/java/com/fuorimondo/common/BaseEntity.java` :
```java
package com.fuorimondo.common;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 2: Enums users**

Fichier `backend/src/main/java/com/fuorimondo/users/UserStatus.java` :
```java
package com.fuorimondo.users;

public enum UserStatus {
    WAITING_LIST,
    ALLOCATAIRE_PENDING,
    ALLOCATAIRE,
    SUSPENDED
}
```

Fichier `backend/src/main/java/com/fuorimondo/users/UserRole.java` :
```java
package com.fuorimondo.users;

public enum UserRole {
    USER,
    ADMIN
}
```

Fichier `backend/src/main/java/com/fuorimondo/users/Civility.java` :
```java
package com.fuorimondo.users;

public enum Civility {
    MR, MRS, OTHER, NONE
}
```

Fichier `backend/src/main/java/com/fuorimondo/users/Locale.java` :
```java
package com.fuorimondo.users;

public enum Locale {
    FR, IT, EN
}
```

Fichier `backend/src/main/java/com/fuorimondo/users/TierCode.java` :
```java
package com.fuorimondo.users;

public enum TierCode {
    TIER_1, TIER_2, TIER_3
}
```

- [ ] **Step 3: Enum addresses**

Fichier `backend/src/main/java/com/fuorimondo/addresses/AddressType.java` :
```java
package com.fuorimondo.addresses;

public enum AddressType {
    BILLING, SHIPPING
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/
git commit -m "feat: add BaseEntity and domain enums (UserStatus, UserRole, Civility, Locale, TierCode, AddressType)"
```

---

### Task 6 : Entité User + repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/users/User.java`
- Create: `backend/src/main/java/com/fuorimondo/users/UserRepository.java`
- Create: `backend/src/test/java/com/fuorimondo/users/UserRepositoryTest.java`

- [ ] **Step 1: Écrire le test d'abord**

Fichier `backend/src/test/java/com/fuorimondo/users/UserRepositoryTest.java` :
```java
package com.fuorimondo.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase(
    replace = org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
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
```

- [ ] **Step 2: Lancer le test pour voir qu'il échoue**

```bash
cd backend && mvn test -Dtest=UserRepositoryTest
```

Expected : FAIL, classes `User` et `UserRepository` manquantes.

- [ ] **Step 3: Créer l'entité User**

Fichier `backend/src/main/java/com/fuorimondo/users/User.java` :
```java
package com.fuorimondo.users;

import com.fuorimondo.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Civility civility;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_code", length = 20)
    private TierCode tierCode;

    @Column(name = "referrer_info", columnDefinition = "TEXT")
    private String referrerInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Locale locale;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    // Getters / setters (tous)
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.toLowerCase(); }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Civility getCivility() { return civility; }
    public void setCivility(Civility civility) { this.civility = civility; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public TierCode getTierCode() { return tierCode; }
    public void setTierCode(TierCode tierCode) { this.tierCode = tierCode; }
    public String getReferrerInfo() { return referrerInfo; }
    public void setReferrerInfo(String referrerInfo) { this.referrerInfo = referrerInfo; }
    public Locale getLocale() { return locale; }
    public void setLocale(Locale locale) { this.locale = locale; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
```

- [ ] **Step 4: Créer le repository**

Fichier `backend/src/main/java/com/fuorimondo/users/UserRepository.java` :
```java
package com.fuorimondo.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        String emailLike, String nameLike, Pageable pageable);
}
```

- [ ] **Step 5: Relancer le test, il doit passer**

```bash
cd backend && mvn test -Dtest=UserRepositoryTest
```

Expected : PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/users/User.java \
        backend/src/main/java/com/fuorimondo/users/UserRepository.java \
        backend/src/test/java/com/fuorimondo/users/UserRepositoryTest.java
git commit -m "feat: add User entity and repository with uniqueness test"
```

---

### Task 7 : Entité Address + repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/addresses/Address.java`
- Create: `backend/src/main/java/com/fuorimondo/addresses/AddressRepository.java`
- Create: `backend/src/test/java/com/fuorimondo/addresses/AddressRepositoryTest.java`

- [ ] **Step 1: Écrire le test d'abord**

Fichier `backend/src/test/java/com/fuorimondo/addresses/AddressRepositoryTest.java` :
```java
package com.fuorimondo.addresses;

import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase(
    replace = org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
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
```

- [ ] **Step 2: Lancer le test, observer l'échec**

```bash
cd backend && mvn test -Dtest=AddressRepositoryTest
```

Expected : FAIL (classes Address / AddressRepository manquantes).

- [ ] **Step 3: Entité Address**

Fichier `backend/src/main/java/com/fuorimondo/addresses/Address.java` :
```java
package com.fuorimondo.addresses;

import com.fuorimondo.common.BaseEntity;
import com.fuorimondo.users.User;
import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AddressType type;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false)
    private String street;

    @Column(name = "street_extra")
    private String streetExtra;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public AddressType getType() { return type; }
    public void setType(AddressType type) { this.type = type; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getStreetExtra() { return streetExtra; }
    public void setStreetExtra(String streetExtra) { this.streetExtra = streetExtra; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}
```

- [ ] **Step 4: Repository Address**

Fichier `backend/src/main/java/com/fuorimondo/addresses/AddressRepository.java` :
```java
package com.fuorimondo.addresses;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndType(UUID userId, AddressType type);

    List<Address> findByUserId(UUID userId);
}
```

- [ ] **Step 5: Relancer**

```bash
cd backend && mvn test -Dtest=AddressRepositoryTest
```

Expected : PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/addresses/ \
        backend/src/test/java/com/fuorimondo/addresses/
git commit -m "feat: add Address entity and repository"
```

---

### Task 8 : Entité InvitationCode + repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/InvitationCode.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/InvitationCodeRepository.java`
- Create: `backend/src/test/java/com/fuorimondo/auth/InvitationCodeRepositoryTest.java`

- [ ] **Step 1: Test**

Fichier `backend/src/test/java/com/fuorimondo/auth/InvitationCodeRepositoryTest.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase(
    replace = org.springframework.boot.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class InvitationCodeRepositoryTest {

    @Autowired UserRepository userRepo;
    @Autowired InvitationCodeRepository codeRepo;

    @Test
    void findByCode() {
        User admin = user("admin@fm.com", UserRole.ADMIN);
        User pending = user("x@fm.com", UserRole.USER);
        userRepo.saveAll(java.util.List.of(admin, pending));

        InvitationCode ic = new InvitationCode();
        ic.setUser(pending);
        ic.setCode("ABC123");
        ic.setGeneratedAt(Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(Instant.now().plusSeconds(3600));
        codeRepo.save(ic);

        Optional<InvitationCode> found = codeRepo.findByCode("ABC123");
        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("x@fm.com");
    }

    private User user(String email, UserRole role) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("F"); u.setLastName("L");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("X");
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        u.setRole(role);
        u.setLocale(Locale.FR);
        return u;
    }
}
```

- [ ] **Step 2: Lancer, observer l'échec**

```bash
cd backend && mvn test -Dtest=InvitationCodeRepositoryTest
```

Expected : FAIL.

- [ ] **Step 3: Entité**

Fichier `backend/src/main/java/com/fuorimondo/auth/InvitationCode.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.users.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "invitation_codes")
public class InvitationCode {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
    }

    public boolean isValid(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public User getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(User generatedBy) { this.generatedBy = generatedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvitationCode that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 4: Repository**

Fichier `backend/src/main/java/com/fuorimondo/auth/InvitationCodeRepository.java` :
```java
package com.fuorimondo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {

    Optional<InvitationCode> findByCode(String code);

    Optional<InvitationCode> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
```

- [ ] **Step 5: Relancer**

```bash
cd backend && mvn test -Dtest=InvitationCodeRepositoryTest
```

Expected : PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/InvitationCode.java \
        backend/src/main/java/com/fuorimondo/auth/InvitationCodeRepository.java \
        backend/src/test/java/com/fuorimondo/auth/InvitationCodeRepositoryTest.java
git commit -m "feat: add InvitationCode entity and repository"
```

---

### Task 9 : Entité PasswordResetToken + repository

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/PasswordResetToken.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/PasswordResetTokenRepository.java`

- [ ] **Step 1: Entité**

Fichier `backend/src/main/java/com/fuorimondo/auth/PasswordResetToken.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.users.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isValid(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken that)) return false;
        return id != null && id.equals(that.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 2: Repository**

Fichier `backend/src/main/java/com/fuorimondo/auth/PasswordResetTokenRepository.java` :
```java
package com.fuorimondo.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
```

- [ ] **Step 3: Compiler**

```bash
cd backend && mvn compile
```

Expected : BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/PasswordResetToken*.java
git commit -m "feat: add PasswordResetToken entity and repository"
```

---

## Phase 3 — Spring Security foundation

### Task 10 : Configuration de base Spring Security

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/fuorimondo/security/CustomUserDetails.java`
- Create: `backend/src/main/java/com/fuorimondo/security/CustomUserDetailsService.java`

- [ ] **Step 1: CustomUserDetails**

Fichier `backend/src/main/java/com/fuorimondo/security/CustomUserDetails.java` :
```java
package com.fuorimondo.security;

import com.fuorimondo.users.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public UUID getUserId() { return user.getId(); }
    public User getUser() { return user; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPasswordHash(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != com.fuorimondo.users.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return user.getPasswordHash() != null
            && user.getStatus() != com.fuorimondo.users.UserStatus.ALLOCATAIRE_PENDING;
    }
}
```

- [ ] **Step 2: CustomUserDetailsService**

Fichier `backend/src/main/java/com/fuorimondo/security/CustomUserDetailsService.java` :
```java
package com.fuorimondo.security;

import com.fuorimondo.users.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email)
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
```

- [ ] **Step 3: SecurityConfig**

Fichier `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java` :
```java
package com.fuorimondo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(csrfHandler))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/legal/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // for H2 console
            .formLogin(fl -> fl.disable())
            .httpBasic(hb -> hb.disable())
            .logout(lo -> lo
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));

        return http.build();
    }
}
```

- [ ] **Step 4: Compiler + démarrer**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected : démarrage OK, logs Spring Security actifs. Arrêter.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/security/
git commit -m "feat: add Spring Security config with session + CSRF + role-based access"
```

---

### Task 11 : Rate limiting filter

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/security/RateLimitFilter.java`
- Create: `backend/src/main/java/com/fuorimondo/security/RateLimitConfig.java`

- [ ] **Step 1: Config des buckets Bucket4j**

Fichier `backend/src/main/java/com/fuorimondo/security/RateLimitConfig.java` :
```java
package com.fuorimondo.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RateLimitConfig {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public RateLimitConfig(int maxAttempts, Duration window) {
        this.maxAttempts = maxAttempts;
        this.window = window;
    }

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
            .addLimit(Bandwidth.classic(maxAttempts, Refill.greedy(maxAttempts, window)))
            .build());
    }
}
```

- [ ] **Step 2: Filter**

Fichier `backend/src/main/java/com/fuorimondo/security/RateLimitFilter.java` :
```java
package com.fuorimondo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitConfig> configs;

    public RateLimitFilter(
        @Value("${fuorimondo.rate-limit.login-attempts-per-15min:5}") int loginLimit,
        @Value("${fuorimondo.rate-limit.activation-attempts-per-15min:5}") int activationLimit,
        @Value("${fuorimondo.rate-limit.reset-attempts-per-15min:5}") int resetLimit
    ) {
        Duration window = Duration.ofMinutes(15);
        this.configs = Map.of(
            "/api/auth/login", new RateLimitConfig(loginLimit, window),
            "/api/auth/activate", new RateLimitConfig(activationLimit, window),
            "/api/auth/password-reset/request", new RateLimitConfig(resetLimit, window)
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitConfig config = configs.get(path);
        if (config != null && "POST".equals(request.getMethod())) {
            String key = path + ":" + request.getRemoteAddr();
            if (!config.resolveBucket(key).tryConsume(1)) {
                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"too_many_requests\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 3: Ajouter le filter à la chaîne de sécurité**

Modifier `backend/src/main/java/com/fuorimondo/security/SecurityConfig.java` — injecter `RateLimitFilter` et l'ajouter avant `UsernamePasswordAuthenticationFilter` :
```java
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Dans SecurityConfig, ajouter un champ et un constructeur :
private final RateLimitFilter rateLimitFilter;

public SecurityConfig(RateLimitFilter rateLimitFilter) {
    this.rateLimitFilter = rateLimitFilter;
}

// Dans filterChain(), avant return http.build(); :
http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
```

- [ ] **Step 4: Compiler**

```bash
cd backend && mvn compile
```

Expected : BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/security/RateLimit*.java \
        backend/src/main/java/com/fuorimondo/security/SecurityConfig.java
git commit -m "feat: add per-IP rate limiting on login/activate/password-reset endpoints"
```

---

## Phase 4 — Service EmailSender (console en dev)

### Task 12 : Interface EmailSender + implémentation console

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/email/EmailSender.java`
- Create: `backend/src/main/java/com/fuorimondo/email/ConsoleEmailSender.java`

- [ ] **Step 1: Interface**

Fichier `backend/src/main/java/com/fuorimondo/email/EmailSender.java` :
```java
package com.fuorimondo.email;

import com.fuorimondo.users.Locale;

public interface EmailSender {
    void sendActivationCode(String to, String code, Locale locale);
    void sendWaitingListConfirmation(String to, String firstName, Locale locale);
    void sendPasswordResetLink(String to, String resetUrl, Locale locale);
}
```

- [ ] **Step 2: Implémentation console**

Fichier `backend/src/main/java/com/fuorimondo/email/ConsoleEmailSender.java` :
```java
package com.fuorimondo.email;

import com.fuorimondo.users.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSender.class);

    @Override
    public void sendActivationCode(String to, String code, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Votre code d'activation\nLOCALE: {}\nBODY:\nVotre code d'activation : {}\n====", to, locale, code);
    }

    @Override
    public void sendWaitingListConfirmation(String to, String firstName, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Inscription enregistrée\nLOCALE: {}\nBODY:\nBonjour {},\nVotre inscription à la liste d'attente est enregistrée.\n====", to, locale, firstName);
    }

    @Override
    public void sendPasswordResetLink(String to, String resetUrl, Locale locale) {
        log.info("\n==== EMAIL ====\nTO: {}\nSUBJECT: Fuori Marmo — Réinitialisation du mot de passe\nLOCALE: {}\nBODY:\nLien : {}\n====", to, locale, resetUrl);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/email/
git commit -m "feat: add EmailSender interface and console implementation for dev"
```

---

## Phase 5 — Service Auth & endpoints

### Task 13 : Service de génération de code d'invitation

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/InvitationCodeService.java`
- Create: `backend/src/test/java/com/fuorimondo/auth/InvitationCodeServiceTest.java`

- [ ] **Step 1: Test**

Fichier `backend/src/test/java/com/fuorimondo/auth/InvitationCodeServiceTest.java` :
```java
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
        assertThat(seen.size()).isGreaterThan(90); // near-unique over 100 trials
    }
}
```

- [ ] **Step 2: Lancer, échec attendu**

```bash
cd backend && mvn test -Dtest=InvitationCodeServiceTest
```

Expected : FAIL (classe manquante).

- [ ] **Step 3: Implémenter**

Fichier `backend/src/main/java/com/fuorimondo/auth/InvitationCodeService.java` :
```java
package com.fuorimondo.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class InvitationCodeService {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final int length;
    private final int expirationDays;

    public InvitationCodeService(
        @Value("${fuorimondo.invitation-code.length:6}") int length,
        @Value("${fuorimondo.invitation-code.expiration-days:90}") int expirationDays
    ) {
        this.length = length;
        this.expirationDays = expirationDays;
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public Instant computeExpiration() {
        return Instant.now().plus(expirationDays, ChronoUnit.DAYS);
    }
}
```

- [ ] **Step 4: Relancer**

```bash
cd backend && mvn test -Dtest=InvitationCodeServiceTest
```

Expected : PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/InvitationCodeService.java \
        backend/src/test/java/com/fuorimondo/auth/InvitationCodeServiceTest.java
git commit -m "feat: add InvitationCodeService with configurable length and expiration"
```

---

### Task 14 : DTOs et validation pour l'inscription waiting list

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/dto/RegisterWaitingListRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/dto/ActivateRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/dto/SetPasswordRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/dto/PasswordResetRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/dto/PasswordResetConfirmRequest.java`

- [ ] **Step 1: RegisterWaitingListRequest**

Fichier `backend/src/main/java/com/fuorimondo/auth/dto/RegisterWaitingListRequest.java` :
```java
package com.fuorimondo.auth.dto;

import com.fuorimondo.users.Locale;
import jakarta.validation.constraints.*;

public record RegisterWaitingListRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 2000) String referrerInfo,
    @NotNull Locale locale,
    @NotBlank @Size(min = 12, max = 200) String password,
    @AssertTrue(message = "must accept terms") boolean acceptTerms,
    @AssertTrue(message = "must accept privacy") boolean acceptPrivacy
) {}
```

- [ ] **Step 2: ActivateRequest (étape 1 de l'activation : valider email + code)**

Fichier `backend/src/main/java/com/fuorimondo/auth/dto/ActivateRequest.java` :
```java
package com.fuorimondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivateRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 10) String code
) {}
```

- [ ] **Step 3: SetPasswordRequest (étape 2 : définir mdp après code validé)**

Fichier `backend/src/main/java/com/fuorimondo/auth/dto/SetPasswordRequest.java` :
```java
package com.fuorimondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 10) String code,
    @NotBlank @Size(min = 12, max = 200) String password
) {}
```

- [ ] **Step 4: PasswordResetRequest & Confirm**

Fichier `backend/src/main/java/com/fuorimondo/auth/dto/PasswordResetRequest.java` :
```java
package com.fuorimondo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank @Email String email) {}
```

Fichier `backend/src/main/java/com/fuorimondo/auth/dto/PasswordResetConfirmRequest.java` :
```java
package com.fuorimondo.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 12, max = 200) String password
) {}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/dto/
git commit -m "feat: add auth DTOs with validation annotations"
```

---

### Task 15 : AuthService (logique métier)

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/AuthService.java`
- Create: `backend/src/main/java/com/fuorimondo/auth/AuthException.java`
- Create: `backend/src/test/java/com/fuorimondo/auth/AuthServiceTest.java`

- [ ] **Step 1: AuthException**

Fichier `backend/src/main/java/com/fuorimondo/auth/AuthException.java` :
```java
package com.fuorimondo.auth;

public class AuthException extends RuntimeException {
    public enum Reason {
        EMAIL_ALREADY_USED,
        INVALID_CODE,
        CODE_EXPIRED,
        CODE_ALREADY_USED,
        INVALID_TOKEN,
        TOKEN_EXPIRED
    }

    private final Reason reason;

    public AuthException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
```

- [ ] **Step 2: AuthService (squelette)**

Fichier `backend/src/main/java/com/fuorimondo/auth/AuthService.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.auth.dto.*;
import com.fuorimondo.email.EmailSender;
import com.fuorimondo.users.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InvitationCodeRepository codeRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final InvitationCodeService codeService;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final int resetExpirationHours;
    private final String frontendBaseUrl;
    private static final SecureRandom RNG = new SecureRandom();

    public AuthService(
        UserRepository userRepository,
        InvitationCodeRepository codeRepository,
        PasswordResetTokenRepository tokenRepository,
        InvitationCodeService codeService,
        PasswordEncoder passwordEncoder,
        EmailSender emailSender,
        @Value("${fuorimondo.password-reset.expiration-hours:1}") int resetExpirationHours,
        @Value("${fuorimondo.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.tokenRepository = tokenRepository;
        this.codeService = codeService;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.resetExpirationHours = resetExpirationHours;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public User registerWaitingList(RegisterWaitingListRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.EMAIL_ALREADY_USED, "email already used");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setPhone(req.phone());
        user.setCivility(Civility.NONE);
        user.setCountry(req.country());
        user.setCity(req.city());
        user.setReferrerInfo(req.referrerInfo());
        user.setLocale(req.locale());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.WAITING_LIST);
        user.setRole(UserRole.USER);
        userRepository.save(user);
        emailSender.sendWaitingListConfirmation(user.getEmail(), user.getFirstName(), user.getLocale());
        return user;
    }

    @Transactional(readOnly = true)
    public void verifyInvitationCode(ActivateRequest req) {
        InvitationCode ic = codeRepository.findByCode(req.code().toUpperCase())
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_CODE, "invalid code"));
        if (!ic.getUser().getEmail().equalsIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.INVALID_CODE, "email/code mismatch");
        }
        if (ic.getUsedAt() != null) {
            throw new AuthException(AuthException.Reason.CODE_ALREADY_USED, "already used");
        }
        if (Instant.now().isAfter(ic.getExpiresAt())) {
            throw new AuthException(AuthException.Reason.CODE_EXPIRED, "expired");
        }
    }

    @Transactional
    public User activateAccount(SetPasswordRequest req) {
        InvitationCode ic = codeRepository.findByCode(req.code().toUpperCase())
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_CODE, "invalid code"));
        if (!ic.getUser().getEmail().equalsIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.INVALID_CODE, "mismatch");
        }
        if (!ic.isValid(Instant.now())) {
            throw new AuthException(AuthException.Reason.CODE_EXPIRED, "invalid or expired");
        }
        User user = ic.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setStatus(UserStatus.ALLOCATAIRE);
        ic.setUsedAt(Instant.now());
        return user;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            byte[] raw = new byte[32];
            RNG.nextBytes(raw);
            String plainToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            String hash = sha256Hex(plainToken);

            tokenRepository.deleteByUserId(user.getId());

            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash);
            token.setExpiresAt(Instant.now().plus(resetExpirationHours, ChronoUnit.HOURS));
            tokenRepository.save(token);

            String url = frontendBaseUrl + "/reset-password?token=" + plainToken;
            emailSender.sendPasswordResetLink(user.getEmail(), url, user.getLocale());
        });
    }

    @Transactional
    public void confirmPasswordReset(String plainToken, String newPassword) {
        String hash = sha256Hex(plainToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException(AuthException.Reason.INVALID_TOKEN, "invalid"));
        if (!token.isValid(Instant.now())) {
            throw new AuthException(AuthException.Reason.TOKEN_EXPIRED, "invalid or expired");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3: Test minimal avec @SpringBootTest**

Fichier `backend/src/test/java/com/fuorimondo/auth/AuthServiceTest.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.auth.dto.*;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired InvitationCodeRepository codeRepository;
    @Autowired InvitationCodeService codeService;

    @Test
    void registerWaitingListCreatesUser() {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "new@ex.com", "New", "User", "+33100000000", "France", "Paris",
            "Parrain: X", Locale.FR, "aVerySecurePass123!", true, true);
        User user = authService.registerWaitingList(req);
        assertThat(user.getStatus()).isEqualTo(UserStatus.WAITING_LIST);
        assertThat(user.getPasswordHash()).isNotNull();
    }

    @Test
    void duplicateEmailRejected() {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "dup@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", true, true);
        authService.registerWaitingList(req);
        assertThrows(AuthException.class, () -> authService.registerWaitingList(req));
    }

    @Test
    void activationWithValidCodeSucceeds() {
        User admin = createAdmin();
        User pending = createPending();
        InvitationCode ic = new InvitationCode();
        ic.setUser(pending);
        ic.setCode(codeService.generateCode());
        ic.setGeneratedAt(java.time.Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(codeService.computeExpiration());
        codeRepository.save(ic);

        SetPasswordRequest req = new SetPasswordRequest(
            pending.getEmail(), ic.getCode(), "aVerySecurePass123!");
        User activated = authService.activateAccount(req);
        assertThat(activated.getStatus()).isEqualTo(UserStatus.ALLOCATAIRE);
        assertThat(activated.getPasswordHash()).isNotNull();
    }

    private User createAdmin() {
        User u = baseUser("admin@fm.com");
        u.setRole(UserRole.ADMIN);
        u.setStatus(UserStatus.ALLOCATAIRE);
        return userRepository.save(u);
    }

    private User createPending() {
        User u = baseUser("pending@fm.com");
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        return userRepository.save(u);
    }

    private User baseUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFirstName("F"); u.setLastName("L");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("X");
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        return u;
    }
}
```

- [ ] **Step 4: Lancer**

```bash
cd backend && mvn test -Dtest=AuthServiceTest
```

Expected : PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/AuthService.java \
        backend/src/main/java/com/fuorimondo/auth/AuthException.java \
        backend/src/test/java/com/fuorimondo/auth/AuthServiceTest.java
git commit -m "feat: add AuthService with register, activate, password reset flows + tests"
```

---

### Task 16 : AuthController + gestion d'erreurs globale

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/auth/AuthController.java`
- Create: `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/fuorimondo/common/ApiError.java`
- Create: `backend/src/test/java/com/fuorimondo/auth/AuthControllerTest.java`

- [ ] **Step 1: ApiError**

Fichier `backend/src/main/java/com/fuorimondo/common/ApiError.java` :
```java
package com.fuorimondo.common;

import java.util.List;

public record ApiError(String code, String message, List<String> details) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
```

- [ ] **Step 2: GlobalExceptionHandler**

Fichier `backend/src/main/java/com/fuorimondo/common/GlobalExceptionHandler.java` :
```java
package com.fuorimondo.common;

import com.fuorimondo.auth.AuthException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiError> handleAuth(AuthException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case EMAIL_ALREADY_USED -> HttpStatus.CONFLICT;
            case INVALID_CODE, INVALID_TOKEN -> HttpStatus.BAD_REQUEST;
            case CODE_EXPIRED, TOKEN_EXPIRED, CODE_ALREADY_USED -> HttpStatus.GONE;
        };
        return ResponseEntity.status(status).body(ApiError.of(ex.getReason().name().toLowerCase(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest().body(new ApiError("validation_error", "invalid request", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("constraint_violation", ex.getMessage()));
    }
}
```

- [ ] **Step 3: AuthController**

Fichier `backend/src/main/java/com/fuorimondo/auth/AuthController.java` :
```java
package com.fuorimondo.auth;

import com.fuorimondo.auth.dto.*;
import com.fuorimondo.users.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, AuthenticationManager authenticationManager) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
    }

    public record LoginRequest(@jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
                               @jakarta.validation.constraints.NotBlank String password) {}

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest req,
                                      HttpServletRequest request,
                                      jakarta.servlet.http.HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterWaitingListRequest req) {
        User user = authService.registerWaitingList(req);
        return ResponseEntity.status(201).body(user);
    }

    @PostMapping("/activate/verify")
    public ResponseEntity<Void> verifyCode(@Valid @RequestBody ActivateRequest req) {
        authService.verifyInvitationCode(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody SetPasswordRequest req) {
        authService.activateAccount(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestReset(@Valid @RequestBody PasswordResetRequest req) {
        authService.requestPasswordReset(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmReset(@Valid @RequestBody PasswordResetConfirmRequest req) {
        authService.confirmPasswordReset(req.token(), req.password());
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Test d'intégration MockMvc**

Fichier `backend/src/test/java/com/fuorimondo/auth/AuthControllerTest.java` :
```java
package com.fuorimondo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.auth.dto.RegisterWaitingListRequest;
import com.fuorimondo.users.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void registerWaitingList201() throws Exception {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "controller-test@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", true, true);
        mvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());
    }

    @Test
    void registerWithoutAcceptingTermsIs400() throws Exception {
        RegisterWaitingListRequest req = new RegisterWaitingListRequest(
            "no-terms@ex.com", "A", "B", null, "FR", "X", null,
            Locale.FR, "aVerySecurePass123!", false, true);
        mvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 5: Lancer**

```bash
cd backend && mvn test -Dtest=AuthControllerTest
```

Expected : PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/auth/AuthController.java \
        backend/src/main/java/com/fuorimondo/common/ \
        backend/src/test/java/com/fuorimondo/auth/AuthControllerTest.java
git commit -m "feat: add AuthController endpoints and global exception handler"
```

---

## Phase 6 — Endpoints profil & adresses

### Task 17 : Endpoints /api/me (lecture, mise à jour)

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/users/UserController.java`
- Create: `backend/src/main/java/com/fuorimondo/users/UserService.java`
- Create: `backend/src/main/java/com/fuorimondo/users/dto/UserResponse.java`
- Create: `backend/src/main/java/com/fuorimondo/users/dto/UpdateProfileRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/users/dto/ChangePasswordRequest.java`
- Create: `backend/src/test/java/com/fuorimondo/users/UserControllerTest.java`

- [ ] **Step 1: DTOs**

Fichier `backend/src/main/java/com/fuorimondo/users/dto/UserResponse.java` :
```java
package com.fuorimondo.users.dto;

import com.fuorimondo.users.*;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Civility civility,
    LocalDate birthDate,
    String phone,
    String country,
    String city,
    UserStatus status,
    UserRole role,
    TierCode tierCode,
    Locale locale
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale());
    }
}
```

Fichier `backend/src/main/java/com/fuorimondo/users/dto/UpdateProfileRequest.java` :
```java
package com.fuorimondo.users.dto;

import com.fuorimondo.users.Civility;
import com.fuorimondo.users.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotNull Civility civility,
    LocalDate birthDate,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @NotNull Locale locale
) {}
```

Fichier `backend/src/main/java/com/fuorimondo/users/dto/ChangePasswordRequest.java` :
```java
package com.fuorimondo.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 12, max = 200) String newPassword
) {}
```

- [ ] **Step 2: UserService**

Fichier `backend/src/main/java/com/fuorimondo/users/UserService.java` :
```java
package com.fuorimondo.users;

import com.fuorimondo.users.dto.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow();
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileRequest req) {
        User user = getById(id);
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setCivility(req.civility());
        user.setBirthDate(req.birthDate());
        user.setPhone(req.phone());
        user.setCountry(req.country());
        user.setCity(req.city());
        user.setLocale(req.locale());
        return user;
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = getById(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("invalid current password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }
}
```

- [ ] **Step 3: UserController**

Fichier `backend/src/main/java/com/fuorimondo/users/UserController.java` :
```java
package com.fuorimondo.users;

import com.fuorimondo.security.CustomUserDetails;
import com.fuorimondo.users.dto.ChangePasswordRequest;
import com.fuorimondo.users.dto.UpdateProfileRequest;
import com.fuorimondo.users.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal CustomUserDetails principal) {
        return UserResponse.from(userService.getById(principal.getUserId()));
    }

    @PatchMapping
    public UserResponse updateMe(@AuthenticationPrincipal CustomUserDetails principal,
                                  @Valid @RequestBody UpdateProfileRequest req) {
        return UserResponse.from(userService.updateProfile(principal.getUserId(), req));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(principal.getUserId(), req.currentPassword(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Test intégration**

Fichier `backend/src/test/java/com/fuorimondo/users/UserControllerTest.java` :
```java
package com.fuorimondo.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.users.dto.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User u;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        u = new User();
        u.setEmail("me@fm.com");
        u.setFirstName("Me"); u.setLastName("User");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("Paris");
        u.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setTierCode(TierCode.TIER_1);
        u.setLocale(Locale.FR);
        userRepository.save(u);
    }

    @Test
    void getMeUnauthorized() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void getMeAuthenticated() throws Exception {
        mvc.perform(get("/api/me").with(user("me@fm.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("me@fm.com"))
            .andExpect(jsonPath("$.tierCode").value("TIER_1"));
    }

    @Test
    void updateMe() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest(
            "New", "Name", Civility.MR, null, "+33100", "FR", "Lyon", Locale.IT);
        mvc.perform(patch("/api/me").with(user("me@fm.com").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("New"))
            .andExpect(jsonPath("$.locale").value("IT"));
    }
}
```

- [ ] **Step 5: Lancer**

```bash
cd backend && mvn test -Dtest=UserControllerTest
```

Expected : PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/users/UserController.java \
        backend/src/main/java/com/fuorimondo/users/UserService.java \
        backend/src/main/java/com/fuorimondo/users/dto/ \
        backend/src/test/java/com/fuorimondo/users/UserControllerTest.java
git commit -m "feat: add /api/me endpoints (read profile, update, change password)"
```

---

### Task 18 : CRUD adresses

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/addresses/AddressService.java`
- Create: `backend/src/main/java/com/fuorimondo/addresses/AddressController.java`
- Create: `backend/src/main/java/com/fuorimondo/addresses/dto/AddressRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/addresses/dto/AddressResponse.java`
- Create: `backend/src/test/java/com/fuorimondo/addresses/AddressControllerTest.java`

- [ ] **Step 1: DTOs**

Fichier `backend/src/main/java/com/fuorimondo/addresses/dto/AddressRequest.java` :
```java
package com.fuorimondo.addresses.dto;

import com.fuorimondo.addresses.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
    @NotNull AddressType type,
    @NotBlank @Size(max = 200) String fullName,
    @NotBlank String street,
    @Size(max = 255) String streetExtra,
    @NotBlank @Size(max = 20) String postalCode,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String country,
    boolean isDefault
) {}
```

Fichier `backend/src/main/java/com/fuorimondo/addresses/dto/AddressResponse.java` :
```java
package com.fuorimondo.addresses.dto;

import com.fuorimondo.addresses.Address;
import com.fuorimondo.addresses.AddressType;

import java.util.UUID;

public record AddressResponse(
    UUID id,
    AddressType type,
    String fullName,
    String street,
    String streetExtra,
    String postalCode,
    String city,
    String country,
    boolean isDefault
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getType(), a.getFullName(), a.getStreet(),
            a.getStreetExtra(), a.getPostalCode(), a.getCity(), a.getCountry(), a.isDefault());
    }
}
```

- [ ] **Step 2: Service**

Fichier `backend/src/main/java/com/fuorimondo/addresses/AddressService.java` :
```java
package com.fuorimondo.addresses;

import com.fuorimondo.addresses.dto.AddressRequest;
import com.fuorimondo.users.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> listForUser(UUID userId) {
        return addressRepository.findByUserId(userId);
    }

    @Transactional
    public Address create(UUID userId, AddressRequest req) {
        Address a = new Address();
        a.setUser(userRepository.findById(userId).orElseThrow());
        apply(a, req);
        if (req.isDefault()) unsetOtherDefaults(userId, req.type(), null);
        a.setDefault(req.isDefault());
        return addressRepository.save(a);
    }

    @Transactional
    public Address update(UUID userId, UUID addressId, AddressRequest req) {
        Address a = fetchOwned(userId, addressId);
        apply(a, req);
        if (req.isDefault()) unsetOtherDefaults(userId, req.type(), addressId);
        a.setDefault(req.isDefault());
        return a;
    }

    @Transactional
    public void delete(UUID userId, UUID addressId) {
        Address a = fetchOwned(userId, addressId);
        addressRepository.delete(a);
    }

    private Address fetchOwned(UUID userId, UUID addressId) {
        Address a = addressRepository.findById(addressId).orElseThrow();
        if (!a.getUser().getId().equals(userId)) throw new AccessDeniedException("not owner");
        return a;
    }

    private void unsetOtherDefaults(UUID userId, AddressType type, UUID excludeId) {
        for (Address other : addressRepository.findByUserIdAndType(userId, type)) {
            if (excludeId != null && other.getId().equals(excludeId)) continue;
            other.setDefault(false);
        }
    }

    private void apply(Address a, AddressRequest req) {
        a.setType(req.type());
        a.setFullName(req.fullName());
        a.setStreet(req.street());
        a.setStreetExtra(req.streetExtra());
        a.setPostalCode(req.postalCode());
        a.setCity(req.city());
        a.setCountry(req.country());
    }
}
```

- [ ] **Step 3: Controller**

Fichier `backend/src/main/java/com/fuorimondo/addresses/AddressController.java` :
```java
package com.fuorimondo.addresses;

import com.fuorimondo.addresses.dto.AddressRequest;
import com.fuorimondo.addresses.dto.AddressResponse;
import com.fuorimondo.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/me/addresses")
public class AddressController {

    private final AddressService service;

    public AddressController(AddressService service) { this.service = service; }

    @GetMapping
    public List<AddressResponse> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return service.listForUser(principal.getUserId()).stream()
            .map(AddressResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @Valid @RequestBody AddressRequest req) {
        return ResponseEntity.status(201).body(AddressResponse.from(service.create(principal.getUserId(), req)));
    }

    @PutMapping("/{id}")
    public AddressResponse update(@AuthenticationPrincipal CustomUserDetails principal,
                                   @PathVariable UUID id,
                                   @Valid @RequestBody AddressRequest req) {
        return AddressResponse.from(service.update(principal.getUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable UUID id) {
        service.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Test intégration**

Fichier `backend/src/test/java/com/fuorimondo/addresses/AddressControllerTest.java` :
```java
package com.fuorimondo.addresses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.addresses.dto.AddressRequest;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AddressControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired AddressRepository addressRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User u;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();
        u = new User();
        u.setEmail("addr@fm.com");
        u.setFirstName("A"); u.setLastName("B");
        u.setCivility(Civility.NONE);
        u.setCountry("FR"); u.setCity("Paris");
        u.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        u.setStatus(UserStatus.ALLOCATAIRE);
        u.setRole(UserRole.USER);
        u.setLocale(Locale.FR);
        userRepository.save(u);
    }

    @Test
    void createAndListAddress() throws Exception {
        AddressRequest req = new AddressRequest(AddressType.BILLING, "A B",
            "1 rue X", null, "75001", "Paris", "FR", true);
        mvc.perform(post("/api/me/addresses").with(user("addr@fm.com").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/me/addresses").with(user("addr@fm.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("BILLING"))
            .andExpect(jsonPath("$[0].isDefault").value(true));
    }
}
```

- [ ] **Step 5: Lancer**

```bash
cd backend && mvn test -Dtest=AddressControllerTest
```

Expected : PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/addresses/ \
        backend/src/test/java/com/fuorimondo/addresses/AddressControllerTest.java
git commit -m "feat: add address CRUD endpoints with owner enforcement and single-default per type"
```

---

## Phase 7 — Pages légales

### Task 19 : Textes markdown par défaut FR/IT/EN

**Files:**
- Create: `backend/src/main/resources/legal/cgu.fr.md`
- Create: `backend/src/main/resources/legal/cgu.it.md`
- Create: `backend/src/main/resources/legal/cgu.en.md`
- Create: `backend/src/main/resources/legal/cgv.fr.md`, `.it.md`, `.en.md`
- Create: `backend/src/main/resources/legal/privacy.fr.md`, `.it.md`, `.en.md`
- Create: `backend/src/main/resources/legal/cookies.fr.md`, `.it.md`, `.en.md`
- Create: `backend/src/main/resources/legal/mentions.fr.md`, `.it.md`, `.en.md`

- [ ] **Step 1: Créer les 15 fichiers avec le contenu par défaut**

Tous les fichiers commencent par le bandeau suivant (adapté en IT et EN) :

FR : `> **Projet de texte — en attente de validation juridique**`
IT : `> **Testo provvisorio — in attesa di convalida legale**`
EN : `> **Draft text — awaiting legal review**`

Contenu `cgu.fr.md` (modèle ; répéter la structure pour les 14 autres avec contenu approprié à chaque page / langue) :

```markdown
> **Projet de texte — en attente de validation juridique**

# Conditions Générales d'Utilisation

## 1. Objet
Les présentes conditions régissent l'utilisation de l'application FuoriMondo éditée par [Éditeur à compléter].

## 2. Accès à l'application
L'accès allocataire est soumis à invitation. L'inscription en liste d'attente est ouverte à toute personne majeure.

## 3. Compte utilisateur
L'utilisateur est responsable de la confidentialité de son mot de passe.

## 4. Vente d'alcool
Conformément à la réglementation, l'accès et l'usage sont réservés aux personnes majeures.

## 5. Responsabilité
L'éditeur décline toute responsabilité en cas de force majeure ou d'utilisation non conforme.

## 6. Modification
L'éditeur peut modifier les présentes conditions à tout moment ; l'utilisateur sera notifié.

## 7. Droit applicable
Droit applicable : [à préciser].
```

Contenu `cgv.fr.md` (modèle) :
```markdown
> **Projet de texte — en attente de validation juridique**

# Conditions Générales de Vente

## 1. Champ d'application
Les présentes conditions s'appliquent à toutes les ventes réalisées via l'application FuoriMondo.

## 2. Commandes
Toute commande vaut acceptation des prix et conditions.

## 3. Livraison
Les délais sont communiqués à titre indicatif.

## 4. Rétractation
[Délai et modalités — à préciser selon la juridiction.]

## 5. Paiement
[Moyens acceptés — à préciser.]

## 6. Responsabilité
Les produits sont conformes aux normes en vigueur au moment de l'envoi.
```

Contenu `privacy.fr.md` (modèle) :
```markdown
> **Projet de texte — en attente de validation juridique**

# Politique de confidentialité

## 1. Données collectées
Nom, prénom, email, téléphone, adresses, historique de commandes.

## 2. Finalités
Gestion du compte, exécution des commandes, communication sur les allocations et événements.

## 3. Base légale
Exécution du contrat et consentement pour les communications.

## 4. Durée de conservation
Données conservées pendant la durée de la relation client + 3 ans.

## 5. Destinataires
Personnel de l'éditeur, prestataires logistiques.

## 6. Droits
Accès, rectification, effacement, portabilité, opposition. Contact : [email à compléter].

## 7. Cookies
Voir la politique cookies dédiée.
```

Contenu `cookies.fr.md` (modèle) :
```markdown
> **Projet de texte — en attente de validation juridique**

# Politique cookies

## 1. Définition
Un cookie est un petit fichier déposé sur votre appareil lors de la visite de l'application.

## 2. Cookies utilisés
- Session : strictement nécessaires au fonctionnement (authentification).
- Mesure d'audience : [anonymisée le cas échéant].

## 3. Gestion
Vous pouvez gérer les cookies via les paramètres de votre navigateur.
```

Contenu `mentions.fr.md` (modèle) :
```markdown
> **Projet de texte — en attente de validation juridique**

# Mentions légales

## Éditeur
[Raison sociale]
[Adresse]
[RCS / numéro d'entreprise]
[Responsable de publication]

## Hébergeur
[Nom]
[Adresse]

## Contact
[Email]
[Téléphone]
```

Versions IT (créer `*.it.md`) et EN (`*.en.md`) : reproduire la même structure, traduire les titres et paragraphes.

Pour tenir la promesse d'un plan sans placeholder : les titres IT à utiliser sont :
- CGU : `Condizioni Generali d'Uso`
- CGV : `Condizioni Generali di Vendita`
- Privacy : `Informativa sulla privacy`
- Cookies : `Politica cookie`
- Mentions : `Informazioni legali`

Titres EN :
- CGU : `Terms of Use`
- CGV : `Terms of Sale`
- Privacy : `Privacy Policy`
- Cookies : `Cookie Policy`
- Mentions : `Legal Notice`

Traduire les sections 1 à 7 en gardant la même structure, en remplaçant les paragraphes par leur équivalent en IT / EN.

- [ ] **Step 2: Vérifier les 15 fichiers présents**

```bash
ls backend/src/main/resources/legal/ | wc -l
```

Expected : `15`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/legal/
git commit -m "feat: add default legal page drafts (FR/IT/EN) with pending-legal-review banner"
```

---

### Task 20 : Endpoint GET /api/legal/{slug}

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/legal/LegalController.java`
- Create: `backend/src/main/java/com/fuorimondo/legal/LegalService.java`
- Create: `backend/src/test/java/com/fuorimondo/legal/LegalControllerTest.java`

- [ ] **Step 1: Service**

Fichier `backend/src/main/java/com/fuorimondo/legal/LegalService.java` :
```java
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
```

- [ ] **Step 2: Controller**

Fichier `backend/src/main/java/com/fuorimondo/legal/LegalController.java` :
```java
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
```

- [ ] **Step 3: Test**

Fichier `backend/src/test/java/com/fuorimondo/legal/LegalControllerTest.java` :
```java
package com.fuorimondo.legal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LegalControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void getExistingSlugReturns200() throws Exception {
        mvc.perform(get("/api/legal/cgu").param("locale", "FR"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("cgu"))
            .andExpect(jsonPath("$.markdown").exists());
    }

    @Test
    void unknownSlugReturns404() throws Exception {
        mvc.perform(get("/api/legal/unknown"))
            .andExpect(status().isNotFound());
    }

    @Test
    void italianLocaleReturnsContent() throws Exception {
        mvc.perform(get("/api/legal/cgv").param("locale", "IT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locale").value("IT"));
    }
}
```

- [ ] **Step 4: Lancer**

```bash
cd backend && mvn test -Dtest=LegalControllerTest
```

Expected : PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/legal/ \
        backend/src/test/java/com/fuorimondo/legal/
git commit -m "feat: add public /api/legal/{slug} endpoint with locale selection"
```

---

## Phase 8 — Back-office admin

### Task 21 : Endpoints admin — lister utilisateurs

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/admin/AdminUserController.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/AdminUserService.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/AdminUserResponse.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/CreateAllocataireRequest.java`
- Create: `backend/src/main/java/com/fuorimondo/admin/dto/UpdateUserByAdminRequest.java`
- Create: `backend/src/test/java/com/fuorimondo/admin/AdminUserControllerTest.java`

- [ ] **Step 1: DTOs**

Fichier `backend/src/main/java/com/fuorimondo/admin/dto/AdminUserResponse.java` :
```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.users.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUserResponse(
    UUID id, String email, String firstName, String lastName, Civility civility,
    LocalDate birthDate, String phone, String country, String city,
    UserStatus status, UserRole role, TierCode tierCode, Locale locale,
    String referrerInfo, String adminNotes, Instant createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
            u.getCivility(), u.getBirthDate(), u.getPhone(), u.getCountry(), u.getCity(),
            u.getStatus(), u.getRole(), u.getTierCode(), u.getLocale(),
            u.getReferrerInfo(), u.getAdminNotes(), u.getCreatedAt());
    }
}
```

Fichier `backend/src/main/java/com/fuorimondo/admin/dto/CreateAllocataireRequest.java` :
```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.users.Civility;
import com.fuorimondo.users.Locale;
import com.fuorimondo.users.TierCode;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateAllocataireRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotNull Civility civility,
    LocalDate birthDate,
    @Size(max = 30) String phone,
    @NotBlank @Size(max = 100) String country,
    @NotBlank @Size(max = 100) String city,
    @NotNull TierCode tierCode,
    @NotNull Locale locale,
    @Size(max = 2000) String adminNotes
) {}
```

Fichier `backend/src/main/java/com/fuorimondo/admin/dto/UpdateUserByAdminRequest.java` :
```java
package com.fuorimondo.admin.dto;

import com.fuorimondo.users.TierCode;
import com.fuorimondo.users.UserStatus;
import jakarta.validation.constraints.Size;

public record UpdateUserByAdminRequest(
    UserStatus status,
    TierCode tierCode,
    @Size(max = 2000) String adminNotes
) {}
```

- [ ] **Step 2: Service admin**

Fichier `backend/src/main/java/com/fuorimondo/admin/AdminUserService.java` :
```java
package com.fuorimondo.admin;

import com.fuorimondo.admin.dto.CreateAllocataireRequest;
import com.fuorimondo.admin.dto.UpdateUserByAdminRequest;
import com.fuorimondo.auth.*;
import com.fuorimondo.users.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final InvitationCodeRepository codeRepository;
    private final InvitationCodeService codeService;

    public AdminUserService(UserRepository userRepository,
                             InvitationCodeRepository codeRepository,
                             InvitationCodeService codeService) {
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.codeService = codeService;
    }

    @Transactional(readOnly = true)
    public Page<User> search(String status, String query, Pageable pageable) {
        if (query != null && !query.isBlank()) {
            return userRepository.findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                query, query, pageable);
        }
        if (status != null && !status.isBlank()) {
            return userRepository.findByStatus(UserStatus.valueOf(status), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow();
    }

    @Transactional
    public CreateAllocataireResult createAllocataire(CreateAllocataireRequest req, UUID adminId) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new AuthException(AuthException.Reason.EMAIL_ALREADY_USED, "email already used");
        }
        User admin = userRepository.findById(adminId).orElseThrow();
        User u = new User();
        u.setEmail(req.email());
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setCivility(req.civility());
        u.setBirthDate(req.birthDate());
        u.setPhone(req.phone());
        u.setCountry(req.country());
        u.setCity(req.city());
        u.setTierCode(req.tierCode());
        u.setLocale(req.locale());
        u.setAdminNotes(req.adminNotes());
        u.setStatus(UserStatus.ALLOCATAIRE_PENDING);
        u.setRole(UserRole.USER);
        userRepository.save(u);

        InvitationCode ic = newCodeFor(u, admin);
        codeRepository.save(ic);
        return new CreateAllocataireResult(u, ic.getCode());
    }

    @Transactional
    public String regenerateCode(UUID userId, UUID adminId) {
        User u = userRepository.findById(userId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        codeRepository.deleteByUserId(userId);
        codeRepository.flush();
        InvitationCode ic = newCodeFor(u, admin);
        codeRepository.save(ic);
        return ic.getCode();
    }

    @Transactional
    public User update(UUID userId, UpdateUserByAdminRequest req) {
        User u = userRepository.findById(userId).orElseThrow();
        if (req.status() != null) u.setStatus(req.status());
        if (req.tierCode() != null) u.setTierCode(req.tierCode());
        if (req.adminNotes() != null) u.setAdminNotes(req.adminNotes());
        return u;
    }

    private InvitationCode newCodeFor(User user, User admin) {
        InvitationCode ic = new InvitationCode();
        ic.setUser(user);
        ic.setCode(codeService.generateCode());
        ic.setGeneratedAt(Instant.now());
        ic.setGeneratedBy(admin);
        ic.setExpiresAt(codeService.computeExpiration());
        return ic;
    }

    public record CreateAllocataireResult(User user, String code) {}
}
```

- [ ] **Step 3: Controller admin**

Fichier `backend/src/main/java/com/fuorimondo/admin/AdminUserController.java` :
```java
package com.fuorimondo.admin;

import com.fuorimondo.admin.dto.AdminUserResponse;
import com.fuorimondo.admin.dto.CreateAllocataireRequest;
import com.fuorimondo.admin.dto.UpdateUserByAdminRequest;
import com.fuorimondo.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) { this.service = service; }

    public record CreateAllocataireResponse(AdminUserResponse user, String code) {}

    @GetMapping
    public Page<AdminUserResponse> list(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String q,
                                         Pageable pageable) {
        return service.search(status, q, pageable).map(AdminUserResponse::from);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return AdminUserResponse.from(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<CreateAllocataireResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateAllocataireRequest req) {
        var result = service.createAllocataire(req, principal.getUserId());
        return ResponseEntity.status(201).body(
            new CreateAllocataireResponse(AdminUserResponse.from(result.user()), result.code()));
    }

    @PostMapping("/{id}/regenerate-code")
    public ResponseEntity<String> regenerate(@AuthenticationPrincipal CustomUserDetails principal,
                                              @PathVariable UUID id) {
        return ResponseEntity.ok(service.regenerateCode(id, principal.getUserId()));
    }

    @PatchMapping("/{id}")
    public AdminUserResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateUserByAdminRequest req) {
        return AdminUserResponse.from(service.update(id, req));
    }
}
```

- [ ] **Step 4: Test**

Fichier `backend/src/test/java/com/fuorimondo/admin/AdminUserControllerTest.java` :
```java
package com.fuorimondo.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fuorimondo.admin.dto.CreateAllocataireRequest;
import com.fuorimondo.users.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        User admin = new User();
        admin.setEmail("admin@fm.com");
        admin.setFirstName("A"); admin.setLastName("Dmin");
        admin.setCivility(Civility.NONE);
        admin.setCountry("FR"); admin.setCity("X");
        admin.setPasswordHash(passwordEncoder.encode("aVerySecurePass123!"));
        admin.setStatus(UserStatus.ALLOCATAIRE);
        admin.setRole(UserRole.ADMIN);
        admin.setLocale(Locale.FR);
        userRepository.save(admin);
    }

    @Test
    void nonAdminGets403() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("x").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAllocataireAndGetCode() throws Exception {
        CreateAllocataireRequest req = new CreateAllocataireRequest(
            "new.allo@fm.com", "New", "Allo", Civility.MR, null, "+33",
            "FR", "Paris", TierCode.TIER_2, Locale.FR, null);
        mvc.perform(post("/api/admin/users")
                .with(user("admin@fm.com").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.status").value("ALLOCATAIRE_PENDING"))
            .andExpect(jsonPath("$.code").isNotEmpty());
    }
}
```

- [ ] **Step 5: Lancer**

```bash
cd backend && mvn test -Dtest=AdminUserControllerTest
```

Expected : PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/admin/ \
        backend/src/test/java/com/fuorimondo/admin/
git commit -m "feat: add admin endpoints (list/create/regenerate-code/update users)"
```

---

### Task 22 : Seeding initial admin au démarrage (profil dev)

**Files:**
- Create: `backend/src/main/java/com/fuorimondo/admin/AdminSeeder.java`

- [ ] **Step 1: Seeder**

Fichier `backend/src/main/java/com/fuorimondo/admin/AdminSeeder.java` :
```java
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
```

- [ ] **Step 2: Tester le démarrage dev**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected : dans les logs, voir `=== SEED ADMIN CREATED ===`. Redémarrer : voir `user ... already exists, skipping.`. Arrêter.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/fuorimondo/admin/AdminSeeder.java
git commit -m "feat: seed default admin account on dev profile startup"
```

---

## Phase 9 — Documentation & finalisation

### Task 23 : OpenAPI (springdoc) + README backend

**Files:**
- Modify: `backend/src/main/resources/application.yml` (ajouter springdoc config)
- Create: `backend/README.md`

- [ ] **Step 1: Config springdoc**

Ajouter à `backend/src/main/resources/application.yml`, à la racine :
```yaml
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
  api-docs:
    path: /v3/api-docs
```

- [ ] **Step 2: README backend**

Fichier `backend/README.md` :
```markdown
# FuoriMondo — Backend

Spring Boot 3.3 / Java 21 / Maven.

## Prérequis

- JDK 21
- Maven 3.9+

## Développement

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Démarre sur http://localhost:8080 avec H2 en fichier (`./data/fuorimondo-dev`).

**Console H2** : http://localhost:8080/h2-console — URL JDBC : `jdbc:h2:file:./data/fuorimondo-dev`, user `sa`, pas de password.

**Swagger UI** : http://localhost:8080/swagger-ui.html

**Admin par défaut (dev)** : `admin@fuorimondo.local` / `Admin!Password123` (logué en console au premier démarrage).

## Tests

```bash
mvn test
```

Tests d'intégration Spring Boot sur H2 in-memory (profil `test`).

## Profils

- `dev` (défaut) — H2 file-based, logs DEBUG, console H2 exposée
- `prod` — PostgreSQL via variables `DB_URL`, `DB_USER`, `DB_PASSWORD`
- `test` — H2 in-memory, pas de store session

## Structure

```
src/main/java/com/fuorimondo/
├── admin/        back-office admin
├── addresses/    gestion des adresses
├── auth/         authentification, codes, tokens, reset
├── common/       BaseEntity, ApiError, GlobalExceptionHandler
├── email/        interface + implémentation console
├── legal/        pages légales
├── security/     Spring Security config, rate limiting
└── users/        entité User + profil
```

## Endpoints principaux

| Méthode | Chemin | Description |
|---|---|---|
| POST | `/api/auth/login` | Connexion (email/password) |
| POST | `/api/auth/logout` | Déconnexion |
| POST | `/api/auth/register` | Inscription liste d'attente |
| POST | `/api/auth/activate/verify` | Vérifie email+code |
| POST | `/api/auth/activate` | Définit le mot de passe après validation code |
| POST | `/api/auth/password-reset/request` | Demande de reset |
| POST | `/api/auth/password-reset/confirm` | Confirmation avec token |
| GET | `/api/me` | Profil courant |
| PATCH | `/api/me` | Mise à jour profil |
| POST | `/api/me/password` | Changement mdp |
| GET/POST/PUT/DELETE | `/api/me/addresses` | CRUD adresses |
| GET | `/api/legal/{slug}` | Contenu markdown d'une page légale (CGU, CGV, privacy, cookies, mentions) |
| GET | `/api/admin/users` | Lister utilisateurs (ADMIN) |
| GET | `/api/admin/users/{id}` | Détail user (ADMIN) |
| POST | `/api/admin/users` | Créer un allocataire + code (ADMIN) |
| POST | `/api/admin/users/{id}/regenerate-code` | Régénérer un code (ADMIN) |
| PATCH | `/api/admin/users/{id}` | MAJ status/tier/notes (ADMIN) |
```

- [ ] **Step 3: Vérifier Swagger UI en dev**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Ouvrir http://localhost:8080/swagger-ui.html. Vérifier la liste complète des endpoints. Arrêter.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml backend/README.md
git commit -m "docs: add backend README and configure springdoc OpenAPI"
```

---

### Task 24 : Suite de tests complète + vérification finale

**Files:** (aucune création, juste exécution)

- [ ] **Step 1: Lancer toute la suite**

```bash
cd backend && mvn clean test
```

Expected : PASS pour tous les tests (`UserRepositoryTest`, `AddressRepositoryTest`, `InvitationCodeRepositoryTest`, `InvitationCodeServiceTest`, `AuthServiceTest`, `AuthControllerTest`, `UserControllerTest`, `AddressControllerTest`, `LegalControllerTest`, `AdminUserControllerTest`). 0 failures, 0 errors.

- [ ] **Step 2: Package**

```bash
cd backend && mvn clean package -DskipTests
```

Expected : BUILD SUCCESS, produit `backend/target/fuorimondo-backend-0.1.0-SNAPSHOT.jar`.

- [ ] **Step 3: Vérifier startup du JAR en prod-like**

Créer temporairement un fichier `.env` de test (ne pas committer) pour simuler Postgres — ou utiliser H2 via flag explicite.

Simple test de non-régression : démarrage en `dev` avec fresh DB.

```bash
rm -rf backend/data
cd backend && java -jar target/fuorimondo-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Expected : startup complet sans erreur, migrations Flyway appliquées, admin seedé. Arrêter avec Ctrl+C.

- [ ] **Step 4: Nettoyage**

```bash
rm -rf backend/data
```

- [ ] **Step 5: Commit final du plan A**

Rien de nouveau à committer ici — c'est une tâche de vérification. Le plan A est complet.

---

## Récapitulatif Plan A

**Critères d'acceptation (livrable backend) :**
- [ ] Les 24 tâches sont complétées et commitées
- [ ] `mvn clean test` passe (toutes les classes de tests listées au-dessus)
- [ ] `mvn clean package` produit un JAR fonctionnel
- [ ] Le JAR démarre en profil `dev` (H2) et applique les migrations Flyway
- [ ] Swagger UI liste tous les endpoints documentés dans le README
- [ ] L'admin seedé peut être utilisé pour tester manuellement via Swagger ou curl : création d'un allocataire, génération d'un code, activation par un second client

**Prochaine étape :** écrire et exécuter le **Plan B — Frontend & E2E** (Vue scaffolding, design tokens Tailwind + Adobe Fonts, i18n, shell de navigation, écrans, tests Playwright).
