package com.techpulse.taskmanager.service;

import com.techpulse.taskmanager.dto.request.AssignTaskRequest;
import com.techpulse.taskmanager.dto.request.CreateTaskRequest;
import com.techpulse.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.dto.response.TaskResponse;

import java.util.List;

public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse assignTask(Long taskId, AssignTaskRequest request);
    TaskResponse updateStatus(Long taskId, UpdateTaskStatusRequest request);
    List<TaskResponse> getTasksForCurrentUser();
}
