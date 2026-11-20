# Admin API mapping

This file maps the web frontend API contract to Android Retrofit.

## Important Retrofit path rule

Existing Android auth services use paths like:

```kotlin
@POST("auth/code")
```

That implies `BuildConfig.BASE_URL` should normally end with `/api/`.

Therefore Android admin paths should be written without a leading slash and without the `/api/` prefix:

```kotlin
@GET("ref/policy/role")
```

Do not write:

```kotlin
@GET("/api/ref/policy/role")
```

unless `BASE_URL` is deliberately changed to the server root.

## DTOs

### Role DTO

Backend shape:

```json
{
  "id": 1,
  "name": "Admin",
  "slug": "admin",
  "description": null,
  "created_at": "2026-05-17T00:00:00.000000Z",
  "deleted_at": null
}
```

Kotlin DTO:

```kotlin
@Serializable
data class AdminRoleDto(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
```

### Permission DTO

Same structure as role:

```kotlin
@Serializable
data class AdminPermissionDto(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
```

### User DTO

Backend shape:

```json
{
  "id": 1,
  "username": "demo_user",
  "first_name": "Егор",
  "roles": [
    {
      "id": 1,
      "user_id": 1,
      "role_id": 1,
      "created_at": "...",
      "deleted_at": null,
      "role": {
        "id": 1,
        "name": "Admin",
        "slug": "admin",
        "description": null,
        "created_at": "...",
        "deleted_at": null
      }
    }
  ]
}
```

Kotlin DTO:

```kotlin
@Serializable
data class AdminUserDto(
    val id: Int,
    val username: String? = null,
    @SerialName("first_name") val firstName: String,
    val roles: List<UserRoleLinkDto> = emptyList()
)

@Serializable
data class UserRoleLinkDto(
    val id: Int,
    @SerialName("user_id") val userId: Int,
    @SerialName("role_id") val roleId: Int,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val role: AdminRoleDto
)
```

### Payload DTOs

```kotlin
@Serializable
data class RolePayloadDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class PermissionPayloadDto(
    val name: String,
    val slug: String,
    val description: String? = null
)

@Serializable
data class AttachRolePayloadDto(
    @SerialName("role_id") val roleId: Int
)

@Serializable
data class AttachPermissionPayloadDto(
    @SerialName("permission_id") val permissionId: Int
)

@Serializable
data class SuccessMessageDto(
    val message: String
)
```

## Retrofit service

Recommended file:

```text
feature/admin/data/remote/AdminService.kt
```

Recommended service:

```kotlin
interface AdminService {
    @GET("ref/user/")
    suspend fun getUsers(): List<AdminUserDto>

    @GET("ref/policy/role")
    suspend fun getRoles(): List<AdminRoleDto>

    @GET("ref/policy/role/{roleId}")
    suspend fun getRoleById(@Path("roleId") roleId: Int): AdminRoleDto

    @POST("ref/policy/role")
    suspend fun createRole(@Body body: RolePayloadDto): AdminRoleDto

    @PUT("ref/policy/role/{roleId}")
    suspend fun updateRole(
        @Path("roleId") roleId: Int,
        @Body body: RolePayloadDto
    ): AdminRoleDto

    @DELETE("ref/policy/role/{roleId}/soft")
    suspend fun softDeleteRole(@Path("roleId") roleId: Int): AdminRoleDto

    @DELETE("ref/policy/role/{roleId}")
    suspend fun hardDeleteRole(@Path("roleId") roleId: Int): SuccessMessageDto

    @POST("ref/policy/role/{roleId}/restore")
    suspend fun restoreRole(
        @Path("roleId") roleId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): AdminRoleDto

    @GET("ref/policy/permission")
    suspend fun getPermissions(): List<AdminPermissionDto>

    @GET("ref/policy/permission/{permissionId}")
    suspend fun getPermissionById(@Path("permissionId") permissionId: Int): AdminPermissionDto

    @POST("ref/policy/permission")
    suspend fun createPermission(@Body body: PermissionPayloadDto): AdminPermissionDto

    @PUT("ref/policy/permission/{permissionId}")
    suspend fun updatePermission(
        @Path("permissionId") permissionId: Int,
        @Body body: PermissionPayloadDto
    ): AdminPermissionDto

    @DELETE("ref/policy/permission/{permissionId}/soft")
    suspend fun softDeletePermission(@Path("permissionId") permissionId: Int): AdminPermissionDto

    @DELETE("ref/policy/permission/{permissionId}")
    suspend fun hardDeletePermission(@Path("permissionId") permissionId: Int): SuccessMessageDto

    @POST("ref/policy/permission/{permissionId}/restore")
    suspend fun restorePermission(
        @Path("permissionId") permissionId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): AdminPermissionDto

    @GET("ref/user/{userId}/role")
    suspend fun getUserRoles(@Path("userId") userId: Int): List<UserRoleLinkDto>

    @POST("ref/user/{userId}/role")
    suspend fun attachRoleToUser(
        @Path("userId") userId: Int,
        @Body body: AttachRolePayloadDto
    ): AdminRoleDto

    @DELETE("ref/user/{userId}/role/{roleId}/soft")
    suspend fun softDetachRoleFromUser(
        @Path("userId") userId: Int,
        @Path("roleId") roleId: Int
    ): AdminRoleDto

    @DELETE("ref/user/{userId}/role/{roleId}")
    suspend fun hardDetachRoleFromUser(
        @Path("userId") userId: Int,
        @Path("roleId") roleId: Int
    ): SuccessMessageDto

    @POST("ref/user/{userId}/role/{roleId}/restore")
    suspend fun restoreUserRole(
        @Path("userId") userId: Int,
        @Path("roleId") roleId: Int,
        @Body body: Map<String, String> = emptyMap()
    ): AdminRoleDto

    @GET("ref/policy/role/{roleId}/permission")
    suspend fun getRolePermissions(@Path("roleId") roleId: Int): List<AdminPermissionDto>

    @POST("ref/policy/role/{roleId}/permission")
    suspend fun attachPermissionToRole(
        @Path("roleId") roleId: Int,
        @Body body: AttachPermissionPayloadDto
    ): AdminPermissionDto

    @DELETE("ref/policy/role/{roleId}/permission/{permissionId}")
    suspend fun detachPermissionFromRole(
        @Path("roleId") roleId: Int,
        @Path("permissionId") permissionId: Int
    ): SuccessMessageDto
}
```

## Error handling

The web frontend maps common errors:

| Status | User message |
|---|---|
| 400 | Не удалось выполнить запрос. Проверьте данные и попробуйте снова |
| 401 | Необходимо повторно войти в систему |
| 403 | Недостаточно прав для выполнения этого действия |
| 404 | Запрошенные данные не найдены |
| 409 | Данные уже изменились или конфликтуют с текущим состоянием |
| 422 | Не удалось обработать отправленные данные |
| 429 | Слишком много запросов. Попробуйте немного позже |
| 500 | На сервере произошла ошибка. Попробуйте позже |
| 503 | Сервис временно недоступен. Попробуйте позже |
| 504 | Сервер не ответил вовремя. Попробуйте позже |

Android should reuse existing `toReadableMessage()` where possible, or add an admin-specific mapper only if necessary.
