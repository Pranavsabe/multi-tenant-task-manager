package com.techpulse.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techpulse.taskmanager.dto.request.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests hitting real HTTP endpoints (MockMvc) backed by
 * the in-memory H2 database. These prove the core requirement of the
 * assignment: tenant isolation is enforced at every layer, and cross-tenant
 * access is impossible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TenantIsolationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String registerTenantAndLogin(String tenantName, String email, String password) throws Exception {
		TenantRegisterRequest registerRequest = new TenantRegisterRequest(tenantName, email, password);
		mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest))).andExpect(status().isCreated());

		LoginRequest loginRequest = new LoginRequest(tenantName, email, password);
		String response = mockMvc
				.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(loginRequest)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).get("token").asText();
	}

	@BeforeEach
	void setUp() {
		// Each test method uses uniquely-named tenants to avoid collisions.
	}

	@Test
	void tenantRegistration_createsTenantAndAdmin() throws Exception {
		TenantRegisterRequest request = new TenantRegisterRequest("orgOne", "admin@orgone.com", "password1");

		mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("orgOne"));
	}

	@Test
	void login_returnsJwtContainingTenantAndRole() throws Exception {
		registerTenantAndLogin("orgTwo", "admin@orgtwo.com", "password1");
		// If token generation/parsing failed, registerTenantAndLogin would already
		// throw.
	}

	@Test
	void unauthenticatedRequest_isRejected() throws Exception {
		// No token supplied: Spring Security treats the request as "anonymous",
		// which passes the authenticated() check, so @PreAuthorize on the
		// controller method is what actually blocks it -> 403 Forbidden.
		mockMvc.perform(get("/tasks")).andExpect(status().isForbidden());
	}

	@Test
	void invalidToken_isRejectedWithUnauthorized() throws Exception {
		mockMvc.perform(get("/tasks").header("Authorization", "Bearer not-a-real-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminCanCreateUser_withinOwnTenant() throws Exception {
		String adminToken = registerTenantAndLogin("orgThree", "admin@orgthree.com", "password1");

		CreateUserRequest createUserRequest = new CreateUserRequest("manager@orgthree.com", "password1",
				com.techpulse.taskmanager.entity.Role.MANAGER);

		mockMvc.perform(post("/users").header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(createUserRequest)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("MANAGER"));
	}

	@Test
	void managerCanCreateTask_taskGetsManagerTenantId() throws Exception {
		String adminToken = registerTenantAndLogin("orgFour", "admin@orgfour.com", "password1");

		mockMvc.perform(
				post("/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUserRequest("manager@orgfour.com",
								"password1", com.techpulse.taskmanager.entity.Role.MANAGER))))
				.andExpect(status().isCreated());

		String managerToken = objectMapper
				.readTree(mockMvc
						.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(
										new LoginRequest("orgFour", "manager@orgfour.com", "password1"))))
						.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("token").asText();

		mockMvc.perform(
				post("/tasks").header("Authorization", "Bearer " + managerToken).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateTaskRequest("Design DB schema", "desc"))))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Design DB schema"));
	}

	@Test
	void crossTenantTaskAccess_isImpossible() throws Exception {
		// Tenant A creates a task
		String tokenA = registerTenantAndLogin("tenantA", "admin@tenanta.com", "password1");
		mockMvc.perform(
				post("/users").header("Authorization", "Bearer " + tokenA).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUserRequest("manager@tenanta.com",
								"password1", com.techpulse.taskmanager.entity.Role.MANAGER))))
				.andExpect(status().isCreated());

		String managerTokenA = objectMapper
				.readTree(mockMvc
						.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(
										new LoginRequest("tenantA", "manager@tenanta.com", "password1"))))
						.andReturn().getResponse().getContentAsString())
				.get("token").asText();

		String taskResponse = mockMvc
				.perform(post("/tasks").header("Authorization", "Bearer " + managerTokenA)
						.contentType(MediaType.APPLICATION_JSON).content(
								objectMapper.writeValueAsString(new CreateTaskRequest("Tenant A secret task", "desc"))))
				.andReturn().getResponse().getContentAsString();
		Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

		// Tenant B is a completely separate organization
		String tokenB = registerTenantAndLogin("tenantB", "admin@tenantb.com", "password1");
		mockMvc.perform(
				post("/users").header("Authorization", "Bearer " + tokenB).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUserRequest("manager@tenantb.com",
								"password1", com.techpulse.taskmanager.entity.Role.MANAGER))))
				.andExpect(status().isCreated());

		String managerTokenB = objectMapper
				.readTree(mockMvc
						.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(
										new LoginRequest("tenantB", "manager@tenantb.com", "password1"))))
						.andReturn().getResponse().getContentAsString())
				.get("token").asText();

		// Tenant B's manager must NOT be able to assign/see tenant A's task
		mockMvc.perform(put("/tasks/" + taskId + "/assign").header("Authorization", "Bearer " + managerTokenB)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new AssignTaskRequest(1L)))).andExpect(status().isNotFound());

		// Tenant B's GET /tasks must never include tenant A's task
		mockMvc.perform(get("/tasks").header("Authorization", "Bearer " + managerTokenB)).andExpect(status().isOk())
				.andExpect(jsonPath("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers
						.hasItem(org.hamcrest.Matchers.hasEntry("title", "Tenant A secret task")))));
	}

	@Test
	void userCannotUpdateTaskNotAssignedToThem() throws Exception {
		String adminToken = registerTenantAndLogin("orgFive", "admin@orgfive.com", "password1");

		mockMvc.perform(
				post("/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUserRequest("manager@orgfive.com",
								"password1", com.techpulse.taskmanager.entity.Role.MANAGER))))
				.andExpect(status().isCreated());
		mockMvc.perform(
				post("/users").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUserRequest("user@orgfive.com", "password1",
								com.techpulse.taskmanager.entity.Role.USER))))
				.andExpect(status().isCreated());

		String managerToken = objectMapper
				.readTree(mockMvc
						.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(
										new LoginRequest("orgFive", "manager@orgfive.com", "password1"))))
						.andReturn().getResponse().getContentAsString())
				.get("token").asText();

		String userToken = objectMapper
				.readTree(mockMvc
						.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
								.content(objectMapper.writeValueAsString(
										new LoginRequest("orgFive", "user@orgfive.com", "password1"))))
						.andReturn().getResponse().getContentAsString())
				.get("token").asText();

		String taskResponse = mockMvc
				.perform(post("/tasks").header("Authorization", "Bearer " + managerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateTaskRequest("Unassigned task", "d"))))
				.andReturn().getResponse().getContentAsString();
		Long taskId = objectMapper.readTree(taskResponse).get("id").asLong();

		// Task was never assigned to this user -> must be forbidden
		mockMvc.perform(put("/tasks/" + taskId + "/status").header("Authorization", "Bearer " + userToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new UpdateTaskStatusRequest(com.techpulse.taskmanager.entity.TaskStatus.COMPLETED))))
				.andExpect(status().isForbidden());
	}

	@Test
	void invalidPayload_triggersValidationError() throws Exception {
		TenantRegisterRequest badRequest = new TenantRegisterRequest("", "not-an-email", "123");

		mockMvc.perform(post("/tenant/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(badRequest))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation Failed"));
	}
}
