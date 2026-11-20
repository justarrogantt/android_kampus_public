# Catch-up phase 2 prompt — картинки в форуме

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/CATCHUP_PLAN.md.

Контекст.
Бэкенд добавил картинки в посты и комментарии форума. Локальные копии для сверки
контрактов: бэк — /home/sakura/backend, веб-фронт — /home/sakura/frontend
(референс UI — react/src/pages/MainPage/Viewer и Forum).

Контракт:
- POST forum/media, multipart-поле file -> attachment_id
  (см. /home/sakura/backend/app/routers/forum.py:341 и
   app/services/forum_media_service.py)
- GET forum/media/{attachment_id}?size=preview — миниатюра, без size — оригинал
- В CreatePostRequest и CreateCommentRequest content стал необязательным и
  появился image_id: str | null. Пост или комментарий без текста И без картинки
  бэк отклоняет.
- В ответах постов и комментариев добавились image_url, image_preview_url,
  image_width, image_height (все nullable).

Задача.
1. Слой данных:
   - Расширь ForumPostDto и ForumCommentDto в
     data/network/dto/ForumDtos.kt четырьмя новыми полями.
   - CreateForumPostRequestDto и аналог для комментария: content с дефолтом ""
     плюс imageId (@SerialName("image_id")).
   - Добавь в data/auth/AuthorizedService.kt @Multipart POST("forum/media")
     по образцу уже существующего uploadProfilePhoto.
   - В data/forum/ForumRepository.kt — метод загрузки вложения из Uri.
     Логику чтения Uri, имени файла и MIME переиспользуй из
     data/auth/ProfileRepository.kt, не копируй её заново; если проще — вынеси
     в общий хелпер.
2. Модели: добавь image-поля в ForumPost и ForumComment
   (ui/main/forum/ForumModels.kt) и мапперы.
3. UI:
   - ThreadCard и CommentCard (ui/main/forum/components/) показывают картинку
     по image_preview_url, сохраняя пропорции через image_width/image_height,
     чтобы лента не прыгала при загрузке. Coil уже подключён (io.coil-kt:coil-compose).
   - В композере поста и комментария — выбор картинки через
     ActivityResultContracts.PickVisualMedia, превью выбранного файла с
     возможностью убрать, индикатор загрузки, блокировка отправки пока
     вложение грузится. Кнопка отправки активна, если есть текст ИЛИ картинка.
   - Полноэкранный просмотрщик поверх открытого поста: тап по картинке
     открывает оригинал (forum/media/{id} без size), pinch-to-zoom, скачивание
     в галерею, закрытие по системному "назад".
4. Ошибки модерации картинки приходят с бэка как обычная ошибка создания —
   показывай их существующим snackbar-механизмом форума.

Ограничения.
- Сетевых вызовов внутри композаблов нет: только состояние и события.
- @Preview для новых компонентов, независимые от сети.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingDebugKotlin

После изменений обнови docs/PROJECT_MAP.md.
```
