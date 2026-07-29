TODO

1. API gateway
    - Validate TLS
    - check rate limits

2. Authentication
    - tenant id in authentication
    - user part of multiple tenants

3. Requires permission annotation on methods and corresponding interceptor

I do the following

I've these tables currently,
tenant(columns:id), tenant_user(columns: id, role_id , subject_type, subject_id, scope_type,scope_id,expires_at)
scope_type: system, tenant, project

When user is added to a tenant or to the system(to manage the tenants) it will be added to the tenant_user with scope_id(tenant_id)
Now,
user logins into OIDC gets token.
authzservice extracts external_id and gets the valid tenants from tenant_user
If the api request access to the tenant or its resources and the user(external_id) has access to it, the api returns data

I've api-gateway module and core-services(controllers, services business logic) module
Can you give me classes in api-gateway and core-services that can achieve this flow
Also used redis as cache to store these mappings


id           | uuid                     |           | not null | gen_random_uuid() | plain    |             |              | 
 role_id      | uuid                     |           | not null |                   | plain    |             |              | 
 subject_type | text                     |           | not null |                   | extended |             |              | 
 subject_id   | text                     |           | not null |                   | extended |             |              | 
 scope_type   | text                     |           | not null |                   | extended |             |              | 
 scope_id     | uuid                     |           |          |                   | plain    |             |              | 
 expires_at   | timestamp with time zone |           |          |                   | plain    |             |              | 
 created_by   | uuid                     |           |          |                   | plain    |             |              | 
 created_at