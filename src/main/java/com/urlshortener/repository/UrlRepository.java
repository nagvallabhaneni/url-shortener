package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.redis.repository.RedisRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends RedisRepository<UrlMapping, String> {
}
