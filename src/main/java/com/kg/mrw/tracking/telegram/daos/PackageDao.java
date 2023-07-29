package com.kg.mrw.tracking.telegram.daos;

import com.kg.mrw.tracking.telegram.documents.Package;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PackageDao extends CrudRepository<Package, ObjectId> {
    Optional<Package> findByTrackingCodeAndChatId(String trackingCode, Long chatId);
    @Query(value = "{ 'updatedAt' : { $gte: ?0 }, 'chatId': ?1 }", sort = "{ 'updatedAt' : -1 }")
    List<Package> findPackagesUpdatedAfter(Instant date, Long chatId);
}
