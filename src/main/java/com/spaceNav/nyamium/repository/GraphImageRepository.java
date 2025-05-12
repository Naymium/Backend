package com.spaceNav.nyamium.repository;

import com.spaceNav.nyamium.domain.GraphImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphImageRepository extends JpaRepository<GraphImage, Long> {

    GraphImage findTopByOrderByCreatedAtDesc();
}
