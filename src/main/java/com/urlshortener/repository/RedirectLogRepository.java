package com.urlshortener.repository;

import com.urlshortener.model.RedirectLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RedirectLogRepository extends MongoRepository<RedirectLog, String> {
    List<RedirectLog> findByShortCode(String shortCode);
    List<RedirectLog> findByUserId(String userId);
    List<RedirectLog> findByShortCodeAndRedirectedAtBetween(String shortCode, LocalDateTime start, LocalDateTime end);
}
