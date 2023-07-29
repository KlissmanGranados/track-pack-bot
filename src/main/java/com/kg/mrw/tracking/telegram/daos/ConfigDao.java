package com.kg.mrw.tracking.telegram.daos;

import com.kg.mrw.tracking.telegram.documents.Config;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ConfigDao extends CrudRepository<Config, ObjectId> {
    Optional<Config> findByAppName(String appName);
}
