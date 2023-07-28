package com.kg.mrw.tracking.telegram.daos;

import com.kg.mrw.tracking.telegram.documents.User;
import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserDao extends CrudRepository<User, ObjectId> {
    Optional<User> findByUserName(String userName);
}
