package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.Data;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataRepository extends JpaRepository<Data, Long> {
}
