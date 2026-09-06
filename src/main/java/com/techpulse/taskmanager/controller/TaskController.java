package com.techpulse.taskmanager.controller;

import com.techpulse.taskmanager.dto.request.AssignTaskRequest;
import com.techpulse.taskmanager.dto.request.CreateTaskRequest;
import com.techpulse.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.dto.response.TaskResponse;
import com.techpulse.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<TaskResponse> assignTask(@PathVariable Long id,
                                                    @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskService.assignTask(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(id, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasks() {
        return ResponseEntity.ok(taskService.getTasksForCurrentUser());
    }
}
