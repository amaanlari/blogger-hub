package com.lari.bloggerhub.repository;

import com.lari.bloggerhub.model.BlogUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BlogUserRepository extends MongoRepository<BlogUser, String> {

    Optional<BlogUser> findByUsername(String username);

    Optional<BlogUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}
