# Compose components specification

Цель: собрать UI из переиспользуемых Compose-компонентов, а не писать один огромный экран.

## Data models

Создай простые модели для моковых данных:

```kotlin
data class AdminUser(
    val id: Int,
    val name: String,
    val username: String?,
    val roles: List<AdminRole>
)

data class AdminRole(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val isDeleted: Boolean = false
)

data class AdminPermission(
    val id: Int,
    val name: String,
    val slug: String,
    val description: String? = null,
    val isDeleted: Boolean = false
)

enum class AdminTab(val title: String) {
    Users("Пользователи"),
    Roles("Роли"),
    Permissions("Права"),
    History("История")
}
```

## Mock data

```kotlin
val MockRoles = listOf(
    AdminRole(1, "Admin", "admin"),
    AdminRole(2, "User", "user"),
    AdminRole(3, "Guest", "guest")
)

val MockUsers = listOf(
    AdminUser(1, "Егор", "demo_user", MockRoles)
)

val MockPermissions = listOf(
    AdminPermission(1, "get-list-user", "get-list-user"),
    AdminPermission(2, "read-user", "read-user"),
    AdminPermission(3, "create-user", "create-user"),
    AdminPermission(4, "update-user", "update-user"),
    AdminPermission(5, "delete-user", "delete-user"),
    AdminPermission(6, "restore-user", "restore-user"),
    AdminPermission(7, "get-list-role", "get-list-role"),
    AdminPermission(8, "read-role", "read-role"),
    AdminPermission(9, "create-role", "create-role"),
    AdminPermission(10, "create-permission", "create-permission"),
    AdminPermission(11, "delete-log", "delete-log")
)
```

## Component list

### 1. `YsuAdminTheme`

Wraps Material 3 theme and applies admin colors/typography.

### 2. `AdminHeader`

```kotlin
@Composable
fun AdminHeader(modifier: Modifier = Modifier)
```

Renders:

```text
ysukampus
АДМИН-ПАНЕЛЬ
```

Use `AnnotatedString` for blue `k`.

### 3. `AdminTabHeader`

```kotlin
@Composable
fun AdminTabHeader(
    currentTab: AdminTab,
    totals: Map<AdminTab, Int>,
    onTabSelected: (AdminTab) -> Unit,
    onAddClick: (() -> Unit)?,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Responsibilities:

- active screen title;
- total chip;
- plus button when available;
- refresh button;
- horizontal tab pills.

### 4. `AdminSearchField`

```kotlin
@Composable
fun AdminSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

Visual:

- 35dp height;
- search icon left;
- white background;
- blue border;
- rounded 15dp.

### 5. `AdminEntityCard`

Generic white card for user/role/permission.

```kotlin
@Composable
fun AdminEntityCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    status: AdminStatus? = null,
    chips: List<String> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Use for users, roles, permissions.

### 6. `AdminStatusBadge`

```kotlin
enum class AdminStatus { Active, SoftDeleted, HardDanger, Primary }

@Composable
fun AdminStatusBadge(
    text: String,
    status: AdminStatus,
    modifier: Modifier = Modifier
)
```

Examples:

```text
активна
активно
удалена
удалено
```

### 7. `AdminRoleChip`

Small chip under user card:

```kotlin
@Composable
fun AdminRoleChip(text: String, isDeleted: Boolean = false)
```

### 8. `AdminDialogFrame`

Custom dialog frame matching screenshots.

```kotlin
@Composable
fun AdminDialogFrame(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

Implementation requirements:

- use `Dialog(onDismissRequest = onClose)`;
- inside: `Card` width 90%, height 90%;
- shape 28dp;
- white background;
- close button top-right;
- content area scrollable where needed.

### 9. `AdminSheetBlock`

Rounded bordered white section inside dialog.

```kotlin
@Composable
fun AdminSheetBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

### 10. `AdminSectionLabel`

Uppercase label:

```kotlin
@Composable
fun AdminSectionLabel(text: String, modifier: Modifier = Modifier)
```

### 11. `AdminTextField`

Single-line input.

```kotlin
@Composable
fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

### 12. `AdminTextArea`

Multiline description field.

```kotlin
@Composable
fun AdminTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

### 13. `AdminSelect`

Custom select/dropdown.

```kotlin
data class AdminSelectOption(
    val id: String,
    val title: String
)

@Composable
fun AdminSelect(
    value: AdminSelectOption?,
    placeholder: String,
    options: List<AdminSelectOption>,
    onValueChange: (AdminSelectOption?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

Use `ExposedDropdownMenuBox` if available, or custom `Box` + `DropdownMenu`. Visual must match screenshots:

- 40dp trigger;
- blue border;
- chevron;
- white menu;
- active item blue.

### 14. `AdminPrimaryButton`

```kotlin
@Composable
fun AdminPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
```

Use gradient background if possible.

### 15. `AdminSoftActionButton`, `AdminDangerActionButton`, `AdminRestoreActionButton`

Use for row actions.

```kotlin
@Composable
fun AdminActionButton(
    text: String,
    kind: AdminActionKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)

enum class AdminActionKind { Primary, SoftWarning, Danger, Restore }
```

### 16. `RoleListItem` / `PermissionListItem`

For items inside dialogs:

```kotlin
@Composable
fun RoleListItem(
    role: AdminRole,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun PermissionListItem(
    permission: AdminPermission,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Selected state:

- stronger blue border;
- subtle elevation.

## Screen composables

### `AdminPanelScreen`

Holds current tab and current dialog state.

```kotlin
@Composable
fun AdminPanelScreen(
    modifier: Modifier = Modifier
)
```

State examples:

```kotlin
var currentTab by rememberSaveable { mutableStateOf(AdminTab.Users) }
var activeDialog by remember { mutableStateOf<AdminDialogState?>(null) }
```

### Dialog state

```kotlin
sealed interface AdminDialogState {
    data class UserSettings(val userId: Int) : AdminDialogState
    data class RoleSettings(val roleId: Int) : AdminDialogState
    data object AddRole : AdminDialogState
    data class PermissionSettings(val permissionId: Int) : AdminDialogState
    data object AddPermission : AdminDialogState
}
```

## Preview requirements

Create previews for:

- `AdminPanelUsersPreview`
- `AdminPanelRolesPreview`
- `AdminPanelPermissionsPreview`
- `UserSettingsDialogPreview`
- `UserSettingsDialogSelectedRolePreview`
- `RoleSettingsDialogPreview`
- `AddRoleDialogPreview`
- `AddPermissionDialogPreview`
- `PermissionSettingsDialogPreview`

Use width around 382dp or 390dp and height around 825dp where possible.
