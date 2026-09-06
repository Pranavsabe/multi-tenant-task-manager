package com.techpulse.taskmanager.service.impl;

import com.techpulse.taskmanager.dto.request.AssignTaskRequest;
import com.techpulse.taskmanager.dto.request.CreateTaskRequest;
import com.techpulse.taskmanager.dto.request.UpdateTaskStatusRequest;
import com.techpulse.taskmanager.dto.response.TaskResponse;
import com.techpulse.taskmanager.entity.Role;
import com.techpulse.taskmanager.entity.Task;
import com.techpulse.taskmanager.entity.TaskStatus;
import com.techpulse.taskmanager.entity.User;
import com.techpulse.taskmanager.exception.ResourceNotFoundException;
import com.techpulse.taskmanager.exception.TenantAccessDeniedException;
import com.techpulse.taskmanager.repository.TaskRepository;
import com.techpulse.taskmanager.repository.UserRepository;
import com.techpulse.taskmanager.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createTask_usesTenantIdFromContext() {
        TenantContext.setTenantId(1L);
        CreateTaskRequest request = new CreateTaskRequest("Fix bug", "desc");
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        TaskResponse response = taskService.createTask(request);

        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Fix bug");
    }

    @Test
    void assignTask_success_whenTaskAndUserBelongToSameTenant() {
        TenantContext.setTenantId(1L);
        Task task = Task.builder().id(10L).title("t").tenantId(1L).status(TaskStatus.PENDING).build();
        User assignee = User.builder().id(20L).tenantId(1L).role(Role.USER).build();

        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findByIdAndTenantId(20L, 1L)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.assignTask(10L, new AssignTaskRequest(20L));

        assertThat(response.getAssignedTo()).isEqualTo(20L);
    }

    @Test
    void assignTask_taskFromAnotherTenant_throwsNotFound() {
        TenantContext.setTenantId(1L);
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(10L, new AssignTaskRequest(20L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignTask_userFromAnotherTenant_throwsTenantAccessDenied() {
        TenantContext.setTenantId(1L);
        Task task = Task.builder().id(10L).title("t").tenantId(1L).status(TaskStatus.PENDING).build();
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(task));
        when(userRepository.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assignTask(10L, new AssignTaskRequest(99L)))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void updateStatus_ownAssignedTask_succeeds() {
        TenantContext.setTenantId(1L);
        TenantContext.setUserId(20L);
        Task task = Task.builder().id(10L).title("t").tenantId(1L).assignedTo(20L).status(TaskStatus.PENDING).build();
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.updateStatus(10L, new UpdateTaskStatusRequest(TaskStatus.COMPLETED));

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void updateStatus_taskNotAssignedToCaller_throwsTenantAccessDenied() {
        TenantContext.setTenantId(1L);
        TenantContext.setUserId(20L);
        Task task = Task.builder().id(10L).title("t").tenantId(1L).assignedTo(99L).status(TaskStatus.PENDING).build();
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(10L, new UpdateTaskStatusRequest(TaskStatus.COMPLETED)))
                .isInstanceOf(TenantAccessDeniedException.class);
    }

    @Test
    void updateStatus_taskFromOtherTenant_throwsNotFound() {
        TenantContext.setTenantId(1L);
        TenantContext.setUserId(20L);
        when(taskRepository.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(10L, new UpdateTaskStatusRequest(TaskStatus.COMPLETED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getTasksForCurrentUser_userRole_returnsOnlyAssignedTasks() {
        TenantContext.setTenantId(1L);
        TenantContext.setUserId(20L);
        TenantContext.setRole("USER");
        Task t1 = Task.builder().id(1L).title("t1").tenantId(1L).assignedTo(20L).status(TaskStatus.PENDING).build();
        when(taskRepository.findAllByTenantIdAndAssignedTo(1L, 20L)).thenReturn(List.of(t1));

        List<TaskResponse> tasks = taskService.getTasksForCurrentUser();

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssignedTo()).isEqualTo(20L);
    }

    @Test
    void getTasksForCurrentUser_adminRole_returnsAllTenantTasks() {
        TenantContext.setTenantId(1L);
        TenantContext.setRole("ADMIN");
        Task t1 = Task.builder().id(1L).title("t1").tenantId(1L).status(TaskStatus.PENDING).build();
        Task t2 = Task.builder().id(2L).title("t2").tenantId(1L).status(TaskStatus.PENDING).build();
        when(taskRepository.findAllByTenantId(1L)).thenReturn(List.of(t1, t2));

        List<TaskResponse> tasks = taskService.getTasksForCurrentUser();

        assertThat(tasks).hasSize(2);
        assertThat(tasks).allMatch(t -> t.getTenantId().equals(1L));
    }
}
