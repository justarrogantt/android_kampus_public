# Backend context from the existing site

Этот файл нужен только как контекст названий сущностей и будущих действий. В текущей задаче **не подключать backend** и **не писать API-клиент**.

## Current task boundary

Implement UI only:

- mock data;
- local state;
- no network;
- no Retrofit/Ktor;
- no real authorization;
- no database.

## Entities

### User

Fields used by frontend:

```text
id: Int
username: String?
first_name: String
roles: List<UserRoleLink>
```

Details model:

```text
id: Int
username: String?
first_name: String
role: "student" | "teacher"
entity_id: Int
```

### UserRoleLink

```text
id: Int
user_id: Int
role_id: Int
created_at: String
deleted_at: String?
role: Role
```

### Role

```text
id: Int
name: String
slug: String
description: String?
created_at: String
deleted_at: String?
```

### Permission

```text
id: Int
name: String
slug: String
description: String?
created_at: String
deleted_at: String?
```

## UI actions and their backend equivalents

These actions exist on the site. For now, implement them as local mock actions or no-op callbacks.

### Users

```text
GET    /api/ref/user/                         -> list users
GET    /api/ref/user/{userId}                 -> user details
PUT    /api/ref/user/{userId}                 -> update user
GET    /api/ref/user/{userId}/role            -> user roles
POST   /api/ref/user/{userId}/role            -> attach role to user
DELETE /api/ref/user/{userId}/role/{roleId}   -> hard detach role
DELETE /api/ref/user/{userId}/role/{roleId}/soft -> soft detach role
POST   /api/ref/user/{userId}/role/{roleId}/restore -> restore role link
GET    /api/ref/user/{userId}/story           -> user change history
```

### Roles

```text
GET    /api/ref/policy/role                         -> list roles
GET    /api/ref/policy/role/{roleId}                -> role details
POST   /api/ref/policy/role                         -> create role
PUT    /api/ref/policy/role/{roleId}                -> update role
DELETE /api/ref/policy/role/{roleId}                -> hard delete role
DELETE /api/ref/policy/role/{roleId}/soft           -> soft delete role
POST   /api/ref/policy/role/{roleId}/restore        -> restore role
GET    /api/ref/policy/role/{roleId}/permission     -> role permissions
POST   /api/ref/policy/role/{roleId}/permission     -> attach permission
DELETE /api/ref/policy/role/{roleId}/permission/{permissionId} -> detach permission
GET    /api/ref/policy/role/{roleId}/story          -> role change history
```

### Permissions

```text
GET    /api/ref/policy/permission                         -> list permissions
GET    /api/ref/policy/permission/{permissionId}          -> permission details
POST   /api/ref/policy/permission                         -> create permission
PUT    /api/ref/policy/permission/{permissionId}          -> update permission
DELETE /api/ref/policy/permission/{permissionId}          -> hard delete permission
DELETE /api/ref/policy/permission/{permissionId}/soft     -> soft delete permission
POST   /api/ref/policy/permission/{permissionId}/restore  -> restore permission
GET    /api/ref/policy/permission/{permissionId}/story    -> permission change history
```

## Payload shapes

### Create/update role

```json
{
  "name": "Admin",
  "slug": "admin",
  "description": null
}
```

### Create/update permission

```json
{
  "name": "get-list-user",
  "slug": "get-list-user",
  "description": null
}
```

### Attach role to user

```json
{
  "role_id": 1
}
```

### Attach permission to role

```json
{
  "permission_id": 1
}
```

## How to use this context in UI-only implementation

Use backend context only for:

- naming models correctly;
- matching button meanings;
- matching status logic via `deleted_at` / `isDeleted`;
- preparing future extension.

Do not implement actual requests in this task.
