package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.OptionalValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OptionalValuesRepository extends JpaRepository<OptionalValues, Long> {
}
