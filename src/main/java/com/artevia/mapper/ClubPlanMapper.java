package com.artevia.mapper;

import com.artevia.dto.ClubPlanDto;
import com.artevia.model.ClubPlan;
import org.mapstruct.Mapper;

@Mapper (componentModel = "spring")
public interface ClubPlanMapper {
    ClubPlanDto toDto(ClubPlan ClubPlan);
}
