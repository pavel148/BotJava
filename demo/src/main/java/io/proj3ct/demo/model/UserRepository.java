package io.proj3ct.demo.model;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User,Long>

{
}
