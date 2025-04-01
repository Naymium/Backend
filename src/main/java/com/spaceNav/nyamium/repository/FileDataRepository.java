package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.FileData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileDataRepository extends JpaRepository<FileData, Long> {
}
