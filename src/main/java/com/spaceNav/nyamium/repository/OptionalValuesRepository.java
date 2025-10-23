package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.OptionalValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OptionalValuesRepository extends JpaRepository<OptionalValues, Long> {
    List<OptionalValues> findByData_IdIn(Collection<Long> ids);
}
