package com.kg.mrw.tracking.telegram.daos;

import com.kg.mrw.tracking.telegram.documents.Provider;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProviderDao extends CrudRepository<Provider, ObjectId> {
    Optional<Provider> findByName(String name);
}
