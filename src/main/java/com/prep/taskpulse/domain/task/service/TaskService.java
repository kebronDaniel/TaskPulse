package com.prep.taskpulse.domain.task.service;

import com.prep.taskpulse.domain.task.dto.TaskResponse;
import com.prep.taskpulse.domain.task.entity.Task;
import com.prep.taskpulse.domain.task.mapper.TaskMapper;
import com.prep.taskpulse.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    // TODO: custom exceptions.
    public TaskResponse findById(UUID id){
        Task task = taskRepository.findById(id).orElseThrow();
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void updateTitle(UUID id, String title) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.changeTitle(title);
    }
}
