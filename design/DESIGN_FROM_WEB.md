# Design extracted from the React website

Источник дизайна: React/Vite frontend админ-панели `ysukampus`. Дизайн нужно переносить как **визуальную систему**, а не как прямой HTML/CSS-код.

## Общий стиль

Админ-панель выглядит как лёгкое мобильное приложение в светлой голубой теме:

- светло-голубой фон всего экрана;
- белые карточки и панели;
- много скруглений;
- мягкие синие бордеры;
- синие CTA-кнопки с лёгким градиентом;
- маленькие pill-badges для статусов;
- компактные карточки сущностей;
- крупная белая модальная панель поверх затемнённого/размытого фона.

## Исходные web-файлы, на которых основан перенос

```text
src/index.css
src/pages/AdminPage/AdminPage.tsx
src/pages/AdminPage/AdminPage.module.css
src/pages/AdminPage/AdminShared.module.css
src/pages/AdminPage/Header/AdminHeader.tsx
src/pages/AdminPage/Header/AdminTabHeader.tsx
src/pages/AdminPage/Header/AdminTabHeader.module.css
src/pages/AdminPage/Search/AdminSearch.tsx
src/pages/AdminPage/Search/AdminSearch.module.css
src/pages/AdminPage/Select/AdminSelect.tsx
src/pages/AdminPage/Select/AdminSelect.module.css
src/pages/AdminPage/UsersTab/UsersTab.tsx
src/pages/AdminPage/RolesTab/RolesTab.tsx
src/pages/AdminPage/PermissionsTab/PermissionsTab.tsx
src/pages/AdminPage/SettingsAlert/SettingsAlert.tsx
src/pages/AdminPage/SettingsAlert/SettingsAlert.module.css
src/pages/AdminPage/SettingsAlert/UserSettingsContent/UserSettingsContent.tsx
src/pages/AdminPage/SettingsAlert/RoleSettingsContent/RoleSettingsContent.tsx
src/pages/AdminPage/SettingsAlert/RoleSettingsContent/AddRoleContent.tsx
src/pages/AdminPage/SettingsAlert/PermissionSettingsContent/PermissionSettingsContent.tsx
src/pages/AdminPage/SettingsAlert/PermissionSettingsContent/AddPermissionContent.tsx
```

## Цветовая система

Использовать эти цвета как дизайн-токены Compose:

```text
Primary:              #3B82F6
PrimaryLight:         #60A5FA
PrimaryDark:          #2D5FA8
PrimaryBright:        rgba(59, 130, 246, 0.10)

Background:           #F0F4FF
BackgroundSecondary:  #E8EEFF
BackgroundTertiary:   #DCE7FE
BackgroundQuaternary: #D0DCFF
Surface:              #FFFFFF
SurfaceSecondary:     rgba(255, 255, 255, 0.60)

Border:               rgba(59, 130, 246, 0.22)
BorderStrong:         #8AABF5

Text:                 #0F1729
TextSecondary:        #2D5FA8
TextMuted:            #8FB4E0

Success:              #059669
SuccessBg:            rgba(0, 153, 102, 0.18)
SuccessBorder:        #6EE7B7

Warning:              #E87000
WarningBg:            rgba(232, 112, 0, 0.15)

Danger:               #E80000
DangerBg:             rgba(232, 0, 0, 0.11)
```

## Типографика

В web-версии используются шрифты:

- `Unbounded` — логотип, заголовки, названия вкладок, заголовки карточек, бейджи;
- `Onest` — основной текст, формы, подписи, плейсхолдеры.

В Android:

- если шрифты уже есть в проекте, подключить их через `FontFamily`;
- если шрифтов нет, использовать системный fallback, но сохранить размеры, веса и иерархию;
- не прикладывать и не копировать font-файлы из web-проекта в ответе, если они не были специально добавлены в Android-проект пользователем.

## Логотип

Логотип в хедере:

```text
ysukampus
АДМИН-ПАНЕЛЬ
```

Визуально:

- `ysu` и `ampus` тёмные;
- буква `k` синяя;
- строка `АДМИН-ПАНЕЛЬ` светло-синяя, uppercase;
- логотип расположен слева сверху.

Можно реализовать `ysukampus` через `AnnotatedString`, где `k` выделена `Primary`.

## Размеры и визуальные паттерны

Базовые значения из сайта:

```text
Screen horizontal padding: 14dp
Header top padding: 20dp
Header bottom spacing: 14dp
Tab navigation height: 35dp
Search height: 35dp
Card padding: 15dp
Card radius: 20dp
Card icon size: 48dp
Card icon radius: 16dp
Badge height: 20dp
Small icon button: 24dp
Dialog width: about 90% of screen
Dialog height: about 90% of screen
Dialog radius: 28dp
Dialog padding: 15dp
Dialog close button: 27dp
Sheet block radius: 18dp
Input height: 40dp
Input radius: 12dp
Textarea min height: 92dp
Primary action height: 44dp
Primary action radius: 16dp
```

## Тени

В web-версии тени очень мягкие и синие:

```text
shadow-sm: 0 6px 8px -4px rgba(59, 130, 246, 0.10)
shadow-md: 0 6px 8px -4px rgba(59, 130, 246, 0.20)
shadow-md1: 0 0px 8px 0px rgba(59, 130, 246, 0.30)
```

В Compose допускается использовать:

- `shadow(elevation = 4.dp, shape = ...)`;
- `CardDefaults.cardElevation(defaultElevation = 2.dp или 4.dp)`;
- можно добавить тонкую рамку, чтобы сохранить ощущение мягкой синей тени.

## Иконки

В web-версии используются SVG:

- пользователь — outline person;
- роль — shield;
- право — key/permission icon;
- поиск — magnifier;
- плюс — add;
- обновление — refresh;
- закрытие — close;
- стрелка выбора — chevron.

В Compose можно использовать Material Icons, если они подключены, или простые векторные аналоги.

## Адаптация под нормальное мобильное приложение

Не нужно копировать web-layout буквально. Нужно сохранить стиль, но сделать UI нативным:

- использовать `Scaffold` / `Box` / `Column` / `LazyColumn`;
- учитывать `WindowInsets.safeDrawing`;
- списки делать прокручиваемыми;
- модалки делать как `Dialog` с большим `Card`, а не как web overlay;
- все кликабельные элементы должны иметь нормальную touch area;
- текст не должен обрезаться критично на маленьких экранах;
- для preview использовать ширину 360–412dp.
