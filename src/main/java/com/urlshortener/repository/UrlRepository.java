package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlRepository extends MongoRepository<UrlMapping, String> {
    List<UrlMapping> findByUserId(String userId);
}
