package com.prep.taskpulse.domain.task.entity;

import com.prep.taskpulse.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "workspaces")
public class Workspace extends BaseEntity {
    @NotBlank
    @Column(nullable = false)
    private String name;
}
