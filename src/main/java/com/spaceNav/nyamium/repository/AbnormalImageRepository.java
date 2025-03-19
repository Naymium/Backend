package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.AbnormalImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AbnormalImageRepository extends JpaRepository<AbnormalImage, Long> {
}
