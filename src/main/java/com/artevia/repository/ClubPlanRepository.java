package com.artevia.repository;

import com.artevia.model.ClubPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClubPlanRepository extends JpaRepository<ClubPlan, Long> {}