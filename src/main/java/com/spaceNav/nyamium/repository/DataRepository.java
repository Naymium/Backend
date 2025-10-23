package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.Data;
import com.spaceNav.nyamium.domain.enums.Save;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DataRepository extends JpaRepository<Data, Long> {
    List<Data> findAllBySave(Save save);

    @Modifying
    @Query("update Data d set d.save = :newStatus where d.save = :oldStatus")
    int updateSaveStatus(@Param("oldStatus") Save oldStatus, @Param("newStatus") Save newStatus);

    void deleteAllBySave(Save save);
}
