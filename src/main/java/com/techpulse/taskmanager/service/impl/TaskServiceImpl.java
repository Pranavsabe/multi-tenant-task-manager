package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.AssignTaskRequest;
import com.techpulse.taskmanager.dto.request.CreateTaskRequest;
import com.techpulse.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.dto.response.TaskResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Task;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.ResourceNotFoundException;
import com.techpulse.taskmanager.exception.TenantAccessDeniedException;
import com.techpulse.taskmanager.repository.TaskRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import com.techpulse.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        // tenant_id always comes from the JWT-backed TenantContext.
        Long tenantId = TenantContext.getTenantId();

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .tenantId(tenantId)
                .build();

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse assignTask(Long taskId, AssignTaskRequest request) {
        Long tenantId = TenantContext.getTenantId();

        
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // The assignee must also belong to the SAME tenant - prevents cross-tenant assignment.
        User assignee = userRepository.findByIdAndTenantId(request.getUserId(), tenantId)
                .orElseThrow(() -> new TenantAccessDeniedException(
                        "Cannot assign task to a user outside your tenant"));

        task.setAssignedTo(assignee.getId());
        task = taskRepository.save(task);

        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(Long taskId, UpdateTaskStatusRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Long currentUserId = TenantContext.getUserId();

        // Ownership validated before update: task must belong to caller's tenant...
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        // ...and a USER may only update a task assigned to themselves.
        if (task.getAssignedTo() == null || !task.getAssignedTo().equals(currentUserId)) {
            throw new TenantAccessDeniedException("You can only update tasks assigned to you");
        }

        task.setStatus(request.getStatus());
        task = taskRepository.save(task);

        return toResponse(task);
    }

    @Override
    public List<TaskResponse> getTasksForCurrentUser() {
        Long tenantId = TenantContext.getTenantId();
        String role = TenantContext.getRole();
        Long currentUserId = TenantContext.getUserId();

        List<Task> tasks;
        if (Role.USER.name().equals(role)) {
            // USER sees only tasks assigned to them, within their own tenant.
            tasks = taskRepository.findAllByTenantIdAndAssignedTo(tenantId, currentUserId);
        } else {
            // ADMIN / MANAGER see all tasks, but strictly scoped to their own tenant.
            tasks = taskRepository.findAllByTenantId(tenantId);
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .tenantId(task.getTenantId())
                .build();
    }
}
