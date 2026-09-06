# Multi-Tenant Task Management System 

This is my submission for the assignment, It's a Spring Boot backend for a task management app where multiple companies (tenants) use the same app and same database, but no company can ever see another company's data.

I used Spring Boot, Spring Security, JWT and H2 database for this project.

## What I built

- Tenant registration (creates a company + its first admin user)
- Login using JWT
- Admin can create users (Manager/User) inside their own company only
- Manager can create tasks and assign them to users in the same company
- User can see and update only the tasks assigned to them
- Every API checks the JWT token, and the company (tenant) is always taken from the token, never from what the user types in the request

## How I made sure one company can't see another company's data

This was the main point of the assignment so I'll explain it simply.

1. When someone logs in, I generate a JWT token and I put the tenant_id and role inside that token.
2. I made a filter (JwtAuthenticationFilter) that runs on every single request. It reads the token, checks if it's valid, and pulls out the tenant_id, user id and role from it.
3. I store these values in a class called TenantContext. It uses ThreadLocal so I can access the current tenant_id from anywhere in my code (service layer, controller, etc.) without passing it around manually.
4. I made sure that none of my request DTOs (like CreateTaskRequest or CreateUserRequest) have a tenant_id field at all. So even if someone tries to send a fake tenant_id in the request body, it just gets ignored because the field doesn't exist.
5. In my repository layer, I never wrote a plain findById(). Every method is something like findByIdAndTenantId() or findAllByTenantId(), so the database query itself filters out other tenants automatically.
6. Before updating anything (like assigning a task), I first check that the task belongs to the same tenant as the logged-in user, and if it's a normal user updating status, I also check the task is actually assigned to them.
7. At the end of the filter, I clear the ThreadLocal (TenantContext.clear()) so old data doesn't accidentally carry over to the next request.

So basically, tenant_id comes from the JWT only, and every query/update is filtered by that tenant_id.

## JWT Token - what's inside it

When a user logs in, I generate a token that has this info inside (this is just an example, not a real token):

```
{
  "sub": "12",
  "email": "manager@acme.com",
  "tenant_id": 3,
  "role": "MANAGER",
  "iat": 1893456000,
  "exp": 1893542400
}
```

- sub = user id
- tenant_id = which company this user belongs to (most important field for this project)
- role = ADMIN / MANAGER / USER, used to decide what APIs the user can call
- iat/exp = when token was issued and when it expires (I set it to 24 hours)

I used the jjwt library to create and read this token. The secret key used to sign the token is kept in application.yml.

## How the request flow works (filter flow)

1. Request comes in with header: Authorization: Bearer <token>
2. My JwtAuthenticationFilter catches it before it reaches the controller
3. It checks if token is valid, then pulls tenant_id/userId/role from it and saves it in TenantContext + Spring's SecurityContext
4. Then Spring Security checks the role using @PreAuthorize on the controller method (like hasRole('ADMIN'))
5. If everything is fine, request goes to the controller -> service -> repository, and all queries use the tenant_id from step 3
6. If token is missing/invalid, the filter itself sends back a 401 error
7. If role doesn't match, Spring Security sends back a 403 error
8. At the very end, TenantContext is cleared so it doesn't leak into the next request

## APIs I built

- POST /tenant/register - anyone can call this, creates a new company + its admin
- POST /auth/login - anyone can call this, needs tenantName + email + password, returns JWT
- POST /users - only ADMIN, creates a user in their own company
- GET /users - only ADMIN, lists users of their own company
- POST /tasks - only MANAGER, creates a task
- PUT /tasks/{id}/assign - only MANAGER, assigns task to a user (must be same company)
- PUT /tasks/{id}/status - only USER, can update only tasks assigned to them
- GET /tasks - USER/MANAGER/ADMIN, but USER sees only their tasks, ADMIN/MANAGER see all company tasks

## Why login needs a tenant name

Since there's no JWT yet before login (the user hasn't logged in), I can't get the tenant_id from a token at that point. So for login only, I ask for the company name (tenantName) along with email and password, just so I know which company's user table to check. After login, everything else only uses the JWT, never the request body, for the tenant_id.

## Some decisions I made

- Same email can exist in two different companies since I check uniqueness per tenant, not globally
- Passwords are hashed using BCrypt, never stored as plain text
- Used H2 in-memory database as mentioned in the assignment (single DB, single schema)
- Used application.yml instead of application.properties as required
- Added global exception handling so all errors (validation, not found, forbidden, etc.) return a clean JSON response instead of a stack trace
- Added tests using JUnit + Mockito for services, and integration tests using MockMvc that actually test two different companies and prove one can't access the other's data

## How to run this project in Eclipse

1. Unzip the project folder
2. Open Eclipse -> File -> Import -> Maven -> Existing Maven Projects -> select the unzipped folder -> Finish
3. Wait for Maven to download all dependencies (needs internet)
4. Find MultiTenantTaskManagerApplication.java, right-click -> Run As -> Java Application
5. App will start on http://localhost:8080

You can test the APIs using Postman. First call /tenant/register, then /auth/login to get the token, then use that token in the Authorization header (Bearer token) for all other APIs.

## How to check test coverage

1. Right-click the project in Eclipse -> Run As -> Maven test
2. After it finishes, go to the project folder -> target/site/jacoco/index.html
3. Open that file in a browser, it shows a full report with % coverage
4. I used the JaCoCo plugin for this, and I've set a minimum of 80% coverage in the pom.xml, so the build will actually fail if coverage drops below that

## Notes

- H2 console is available at http://localhost:8080/h2-console if you want to check the data directly (JDBC URL: jdbc:h2:mem:taskdb, username: sa, no password)
- I also added a sonar-project.properties file to run SonarQube analysis if needed
