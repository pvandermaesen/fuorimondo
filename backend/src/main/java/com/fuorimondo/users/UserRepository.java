package com.fuorimondo.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        String emailLike, String nameLike, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.isParrain = true
          AND (:q IS NULL OR :q = ''
               OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY u.lastName ASC, u.firstName ASC
        """)
    List<User> searchParrains(@Param("q") String q, Pageable pageable);
}
