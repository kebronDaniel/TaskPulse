package com.prep.taskpulse.domain.task.mapper;

import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.entity.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponse toResponse(Task task);
}
