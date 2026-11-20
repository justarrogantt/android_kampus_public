# Admin Backend Contract Checklist

Use this checklist before switching `AdminViewModel` from `FakeAdminRepository` to `RemoteAdminRepository`.

Runtime status: the app still uses `FakeAdminRepository`. `RemoteAdminRepository`, `AdminApi`, DTOs, and mappers are prepared only as a backend integration skeleton.

## Base URL and auth assumptions

- `BuildConfig.BASE_URL` ends with `/api/`.
- `AdminApi` paths do not start with `/` and do not include the `/api/` prefix.
- Admin endpoints use the authorized client created by `createAdminApi()`.
- Admin requests send `X-Device-Id` and `Authorization` through existing interceptors.
- Token refresh continues to use `TokenAuthentificator`.

## DTO fields expected by Android

### User

`AdminUserDto` expects:

- `id: Int`
- `username: String?`
- `first_name: String`
- `roles: List<UserRoleLinkDto>`

`UserRoleLinkDto` expects:

- `id: Int`
- `user_id: Int`
- `role_id: Int`
- `created_at: String?`
- `deleted_at: String?`
- `role: AdminRoleDto`

Manual checks:

- `GET ref/user/` returns `id`, `first_name`, `username`, and `roles`.
- Every role link includes `deleted_at`.
- Every active role link includes a non-null `role` object.
- Confirm whether `first_name` can be missing or null. Android currently treats it as required.

### Role

`AdminRoleDto` expects:

- `id: Int`
- `name: String`
- `slug: String`
- `description: String?`
- `created_at: String?`
- `deleted_at: String?`

Manual checks:

- `GET ref/policy/role` returns `id`, `name`, `slug`, `description`, and `deleted_at`.
- Soft-deleted roles have non-null `deleted_at`.

### Permission

`AdminPermissionDto` expects:

- `id: Int`
- `name: String`
- `slug: String`
- `description: String?`
- `created_at: String?`
- `deleted_at: String?`

Manual checks:

- `GET ref/policy/permission` returns `id`, `name`, `slug`, `description`, and `deleted_at`.
- Soft-deleted permissions have non-null `deleted_at`.

## Request DTOs expected by Android

- `CreateRoleRequestDto`: `name`, `slug`, `description`
- `UpdateRoleRequestDto`: `name`, `slug`, `description`
- `CreatePermissionRequestDto`: `name`, `slug`, `description`
- `UpdatePermissionRequestDto`: `name`, `slug`, `description`
- `AssignRoleToUserRequestDto`: `role_id`
- `AssignPermissionToRoleRequestDto`: `permission_id`

Manual checks:

- Confirm backend accepts `description: null`.
- Confirm backend accepts empty restore request bodies `{}`.

## Endpoint matrix

| Operation | Android endpoint | Expected response | Status |
|---|---|---|---|
| Get users | `GET ref/user/` | `List<AdminUserDto>` | Confirmed from `docs/API_MAPPING_ADMIN.md` |
| Get roles | `GET ref/policy/role` | `List<AdminRoleDto>` | Confirmed |
| Create role | `POST ref/policy/role` | `AdminRoleDto` | Confirmed |
| Update role | `PUT ref/policy/role/{roleId}` | `AdminRoleDto` | Confirmed |
| Soft delete role | `DELETE ref/policy/role/{roleId}/soft` | `AdminRoleDto` | Confirmed |
| Hard delete role | `DELETE ref/policy/role/{roleId}` | `SuccessMessageDto` | Confirmed |
| Restore role | `POST ref/policy/role/{roleId}/restore` | `AdminRoleDto` | Confirmed |
| Get permissions | `GET ref/policy/permission` | `List<AdminPermissionDto>` | Confirmed |
| Create permission | `POST ref/policy/permission` | `AdminPermissionDto` | Confirmed |
| Update permission | `PUT ref/policy/permission/{permissionId}` | `AdminPermissionDto` | Confirmed |
| Soft delete permission | `DELETE ref/policy/permission/{permissionId}/soft` | `AdminPermissionDto` | Confirmed |
| Hard delete permission | `DELETE ref/policy/permission/{permissionId}` | `SuccessMessageDto` | Confirmed |
| Restore permission | `POST ref/policy/permission/{permissionId}/restore` | `AdminPermissionDto` | Confirmed |
| Assign role to user | `POST ref/user/{userId}/role` | `AdminRoleDto` | Confirmed |
| Soft remove role from user | `DELETE ref/user/{userId}/role/{roleId}/soft` | `AdminRoleDto` | Confirmed |
| Hard remove role from user | `DELETE ref/user/{userId}/role/{roleId}` | `SuccessMessageDto` | Confirmed |
| Get role permissions | `GET ref/policy/role/{roleId}/permission` | `List<AdminPermissionDto>` | Confirmed |
| Assign permission to role | `POST ref/policy/role/{roleId}/permission` | `AdminPermissionDto` | Confirmed |
| Soft remove permission from role | `DELETE ref/policy/role/{roleId}/permission/{permissionId}/soft` | `SuccessMessageDto` | Uncertain |
| Hard remove permission from role | `DELETE ref/policy/role/{roleId}/permission/{permissionId}` | `SuccessMessageDto` | Confirmed |

## Manual curl/Postman checks

Run these checks with a valid bearer token and device id header before wiring `RemoteAdminRepository`.

1. `GET ref/user/`
   - Response is an array.
   - Each user has `id`, `first_name`, `username`, and `roles`.
   - Each role link has `role`, `role_id`, `user_id`, and `deleted_at`.

2. `GET ref/policy/role`
   - Response is an array.
   - Each role has `id`, `name`, `slug`, `description`, and `deleted_at`.

3. Create/update role
   - `POST ref/policy/role` accepts `name`, `slug`, `description`.
   - `PUT ref/policy/role/{roleId}` accepts the same shape.
   - Both return `AdminRoleDto`.

4. Delete/restore role
   - `DELETE ref/policy/role/{roleId}/soft` returns `AdminRoleDto` with non-null `deleted_at`.
   - `DELETE ref/policy/role/{roleId}` returns `{ "message": "..." }`.
   - `POST ref/policy/role/{roleId}/restore` accepts `{}` and returns `AdminRoleDto`.

5. `GET ref/policy/permission`
   - Response is an array.
   - Each permission has `id`, `name`, `slug`, `description`, and `deleted_at`.

6. Create/update permission
   - `POST ref/policy/permission` accepts `name`, `slug`, `description`.
   - `PUT ref/policy/permission/{permissionId}` accepts the same shape.
   - Both return `AdminPermissionDto`.

7. Delete/restore permission
   - `DELETE ref/policy/permission/{permissionId}/soft` returns `AdminPermissionDto` with non-null `deleted_at`.
   - `DELETE ref/policy/permission/{permissionId}` returns `{ "message": "..." }`.
   - `POST ref/policy/permission/{permissionId}/restore` accepts `{}` and returns `AdminPermissionDto`.

8. User-role assignment
   - `POST ref/user/{userId}/role` accepts `role_id` and returns `AdminRoleDto`.
   - After assignment, `GET ref/user/` includes the role in that user's active `roles`.
   - `DELETE ref/user/{userId}/role/{roleId}/soft` hides the role from active Android UI mapping or returns a link with non-null `deleted_at`.
   - `DELETE ref/user/{userId}/role/{roleId}` removes the link and returns `{ "message": "..." }`.

9. Role-permission assignment
   - `GET ref/policy/role/{roleId}/permission` returns `List<AdminPermissionDto>`.
   - `POST ref/policy/role/{roleId}/permission` accepts `permission_id` and returns `AdminPermissionDto`.
   - `DELETE ref/policy/role/{roleId}/permission/{permissionId}` removes the permission and returns `{ "message": "..." }`.
   - Confirm whether `DELETE ref/policy/role/{roleId}/permission/{permissionId}/soft` exists. If not, adjust `AdminApi` and `AdminRepository` semantics before switching runtime.

## Switch readiness

Do not switch runtime until:

- All endpoint paths above are confirmed.
- `first_name` nullability is confirmed.
- Role link `role` object presence is confirmed.
- Role-permission soft remove behavior is confirmed.
- `RemoteAdminRepository` is tested manually against a configured backend.
- Error bodies are verified to work with `toReadableMessage()` or an admin-specific parser is added.
