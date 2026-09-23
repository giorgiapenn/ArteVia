package com.artevia.repository;

import com.artevia.model.ClubMembership;
import com.artevia.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {
    Optional<ClubMembership> findByUserAndActiveTrue(User user);
}