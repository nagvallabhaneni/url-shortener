package com.urlshortener.orchestration.repository;

import com.urlshortener.orchestration.model.WorkflowRun;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRunRepository extends MongoRepository<WorkflowRun, String> {
}
