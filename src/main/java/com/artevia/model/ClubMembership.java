package com.artevia.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_membership")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private ClubPlan plan;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
}