# Screen specification: YSU Kampus Admin Panel

Все экраны реализовать на **Jetpack Compose**. Данные моковые. Логика нужна только для переключения вкладок, открытия/закрытия модалок, локального выбора элементов, локального поиска и раскрытия селектов.

## Screenshot map

```text
01_users_tab.png                  -> вкладка Пользователи
02_user_settings_closed.png       -> настройки пользователя, селект закрыт
03_user_settings_select_open.png  -> настройки пользователя, селект открыт
04_user_settings_role_selected.png-> настройки пользователя, выбрана роль
05_roles_tab.png                  -> вкладка Роли
06_role_settings_top.png          -> настройки роли, верхняя часть
07_role_settings_bottom.png       -> настройки роли, нижняя часть после скролла
08_add_role.png                   -> добавление роли
09_permissions_tab.png            -> вкладка Права
10_add_permission.png             -> добавление разрешения
11_permission_settings.png        -> настройки разрешения
```

---

# 1. AdminPanelScreen

Основной экран админ-панели.

## Layout

- Fullscreen container.
- Background: `YsuAdminColors.Background`.
- Content column fills screen.
- Safe area / status bar must be respected.
- Horizontal padding mostly `14.dp`.

## Header

Top-left logo:

```text
ysukampus
АДМИН-ПАНЕЛЬ
```

Logo style:

- large text, bold;
- `k` in blue;
- rest dark;
- subtitle uppercase, muted blue.

## Title row

Under header:

- left: active tab title;
- right: actions row.

Active tab titles:

```text
Пользователи
Роли
Права
История
```

Actions:

- count chip: `Всего: N`;
- add button only for `Роли` and `Права`;
- refresh button always visible.

For `Пользователи` screenshot only count and refresh are visible. For `Роли` and `Права` count, plus, refresh are visible.

## Tab navigation

Horizontal scrollable row of pill buttons:

```text
Пользователи
Роли
Права
История
```

Active tab:

- blue background;
- white text.

Inactive tabs:

- white background;
- blue text;
- soft blue border;
- subtle shadow.

---

# 2. Users tab

Reference: `01_users_tab.png`.

## Search

Placeholder:

```text
Поиск по имени или тегу
```

Search field:

- height 35dp;
- white background;
- rounded 15dp;
- blue border;
- search icon left;
- muted placeholder.

## User card

Card example:

```text
Егор
@demo_user
Admin  User  Guest
```

Card layout:

- white rounded card;
- icon square left, 48dp;
- person outline icon;
- title and username to the right;
- role chips in row under the main row;
- card is clickable and opens `UserSettingsDialog`.

## Mock users

Use at least:

```kotlin
AdminUser(
    id = 1,
    name = "Егор",
    username = "demo_user",
    roles = listOf(Admin, User, Guest)
)
```

---

# 3. UserSettingsDialog

References:

- `02_user_settings_closed.png`
- `03_user_settings_select_open.png`
- `04_user_settings_role_selected.png`

## Dialog frame

- large white panel over dimmed/blurred background;
- width around 90% screen;
- height around 90% screen;
- radius 28dp;
- padding 15dp;
- title row with close button.

Title:

```text
Настройки пользователя
```

Close button:

- square 27dp;
- rounded 7dp;
- pale blue background;
- blue close icon.

## Current roles block

Block label:

```text
ТЕКУЩИЕ РОЛИ
```

Inside list:

```text
Admin  admin  активна
User   user   активна
Guest  guest  активна
```

Role item:

- height about 58dp;
- white background;
- rounded 16dp;
- blue border;
- title bold;
- slug blue;
- status badge on right.

## Selected role state

When role is selected, highlight it:

- border becomes `PrimaryLight`;
- show two action buttons under roles list:

```text
Мягко снять
Снять совсем
```

Button styles:

- `Мягко снять`: warning text and warning background;
- `Снять совсем`: danger text and danger background;
- buttons share row 50/50 with 8dp gap;
- height 44dp;
- radius 12–16dp.

## Assign new role block

Label:

```text
НАЗНАЧИТЬ НОВУЮ РОЛЬ
```

Select closed state:

```text
Выбрать новую роль
```

Primary button:

```text
Назначить роль
```

## Expanded select state

Reference: `03_user_settings_select_open.png`.

When select is open:

- field remains at top;
- arrow rotates up;
- dropdown appears under field;
- dropdown white with blue border/shadow;
- selected/hovered item uses blue background and white text.

Dropdown item text in screenshot:

```text
Выбрать новую роль
```

---

# 4. Roles tab

Reference: `05_roles_tab.png`.

## Header state

Title:

```text
Роли
```

Count chip:

```text
Всего: 4
```

Buttons:

- plus button;
- refresh button.

## Search

Placeholder:

```text
Поиск по названию роли
```

## Role card

Examples:

```text
Admin
admin
активна
```

```text
User
user
активна
```

```text
Guest
guest
активна
```

There is also an empty-looking fourth role card in screenshot. In Android use a mock role with empty title only if needed to match the screenshot, or omit if it looks broken. Prefer a clean native list with real mock roles.

Card layout:

- shield icon left;
- title and slug center;
- active status badge right;
- click opens `RoleSettingsDialog`.

---

# 5. RoleSettingsDialog

References:

- `06_role_settings_top.png`
- `07_role_settings_bottom.png`

Title:

```text
Настройки роли
```

## Top form block

Labels:

```text
НАЗВАНИЕ
СЛУЖЕБНОЕ ИМЯ
ОПИСАНИЕ
```

Existing values for preview:

```text
Admin
admin
```

Description can be empty.

Fields:

- input height 40dp;
- textarea min height 92dp;
- strong blue border;
- rounded corners.

Button:

```text
Сохранить изменения
```

## Assigned permissions block

Label:

```text
НАЗНАЧЕННЫЕ РАЗРЕШЕНИЯ
```

Search placeholder:

```text
Поиск разрешения
```

Permission items examples:

```text
create-permission
create-permission
активно

create-role
create-role
активно

create-user
create-user
активно

delete-log
delete-log
активно
```

Permission item:

- title bold;
- slug blue;
- status badge right.

## Add new permission section

Label:

```text
ДОБАВИТЬ НОВОЕ РАЗРЕШЕНИЕ
```

Select:

```text
Выбрать разрешение
```

Button:

```text
Добавить разрешение
```

## Role actions block

Label:

```text
ДЕЙСТВИЯ НАД РОЛЬЮ
```

Buttons:

```text
Мягко удалить
Удалить совсем
```

---

# 6. AddRoleDialog

Reference: `08_add_role.png`.

Title:

```text
Добавить роль
```

## Form block

Labels and placeholders:

```text
НАЗВАНИЕ
Введите название

СЛУЖЕБНОЕ ИМЯ
Введите служебное имя

ОПИСАНИЕ
Введите описание
```

## Assigned permissions empty state

Label:

```text
НАЗНАЧЕННЫЕ РАЗРЕШЕНИЯ
```

Empty card text:

```text
У роли нет разрешений
Добавьте новое разрешение ниже
```

## Add new permission section

Label:

```text
ДОБАВИТЬ НОВОЕ РАЗРЕШЕНИЕ
```

Select:

```text
Выбрать разрешение
```

Button:

```text
Добавить разрешение
```

Final button outside/after blocks:

```text
Добавить роль
```

---

# 7. Permissions tab

Reference: `09_permissions_tab.png`.

## Header state

Title:

```text
Права
```

Count chip:

```text
Всего: 26
```

Buttons:

- plus button;
- refresh button.

## Search

Placeholder:

```text
Поиск по названию права
```

## Permission cards

Examples:

```text
get-list-user
get-list-user
активна

read-user
read-user
активна

create-user
create-user
активна

update-user
update-user
активна

delete-user
delete-user
активна

restore-user
restore-user
активна

get-list-role
get-list-role
активна
```

Card layout:

- key/permission icon left;
- title and slug center;
- status badge right;
- click opens `PermissionSettingsDialog`.

---

# 8. AddPermissionDialog

Reference: `10_add_permission.png`.

Title:

```text
Добавить разрешение
```

Labels and placeholders:

```text
НАЗВАНИЕ
Введите название

СЛУЖЕБНОЕ ИМЯ
Введите служебное имя

ОПИСАНИЕ
Введите описание
```

Button:

```text
Добавить разрешение
```

---

# 9. PermissionSettingsDialog

Reference: `11_permission_settings.png`.

Title:

```text
Настройки разрешения
```

Existing values for preview:

```text
get-list-user
get-list-user
```

Description can be empty.

Button:

```text
Сохранить изменения
```

Actions block label:

```text
ДЕЙСТВИЯ НАД РАЗРЕШЕНИЕМ
```

Buttons:

```text
Мягко удалить
Удалить совсем
```

---

# 10. History tab

The screenshots show the `История` tab in navigation, but no separate content screenshot is provided.

Implement one of these options:

1. Recommended for now: simple placeholder with the same search/card style:

```text
История изменений появится здесь
```

2. Or implement a basic list of mock history records if it does not delay the main UI.

Do not invent complex history UI unless a separate reference is provided.
