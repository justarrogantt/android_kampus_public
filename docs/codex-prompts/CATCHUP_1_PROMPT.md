# Catch-up phase 1 prompt — WS-ядро и реакция на 1008

```text
Прочитай AGENTS.md, CLAUDE.md, docs/PROJECT_MAP.md и docs/CATCHUP_PLAN.md.

Контекст.
Бэкенд обновил app/routers/ws.py: функция reject() теперь сначала делает accept(),
а потом close(1008), чтобы клиент отличал протухший токен от обрыва сети. Локальная
копия бэка для сверки — /home/sakura/backend.

Наш ModeratorWebSocketClient
(app/src/main/java/com/example/ysukampus/data/network/ws/ModeratorWebSocketClient.kt)
берёт access-токен один раз через TokenStorage.getAccessToken() и при разрыве
переподключается с backoff тем же самым токеном. На 1008 это даёт бесконечный
цикл отказов.

Задача.
1. Вынеси общую механику соединения из ModeratorWebSocketClient в базовый класс
   (предлагаю data/network/ws/BaseWebSocketClient.kt): построение ws/wss-URL из
   BuildConfig.BASE_URL, heartbeat, backoff-реконнект, connectionState,
   isCurrentSocket-защита. Канал (путь и парсер событий) параметризуется —
   в фазе 3 на этой же базе поедет /ws/user.
2. Научи базовый клиент обрабатывать close code 1008: перед следующей попыткой
   обновить access-токен и переподключиться уже с новым. Refresh централизован
   в TokenAuthentificator — НЕ дублируй его логику. Либо переиспользуй
   существующий refresh-путь, либо добавь в него точку входа, которую сможет
   дёрнуть WS-клиент. Если refresh не удался, сообщи об этом через
   SessionManager::onNotAuthorized и не реконнекться дальше.
3. Ограничь число подряд идущих 1008-циклов, чтобы неудачный refresh не крутил
   соединение бесконечно.
4. Добавь в ModeratorWsEvent событие moderation_count (бэк шлёт
   {"type": "moderation_count", ...} — точную форму посмотри в
   /home/sakura/backend/app/services/forum_service.py около строки 578) и
   прокинь его в админскую вкладку модерации как живой счётчик очереди.
   Событие обновляет счётчик, но, как и new_post/new_comment, НЕ вставляет
   элементы в список само.
5. ModeratorWebSocketClient должен остаться работающим ровно как раньше по всем
   остальным сценариям: поднимается только пока открыта вкладка модерации.

Ограничения.
- Не заводи четвёртый Retrofit-клиент. Не меняй BASE_URL, applicationId, уровни
  SDK и подпись.
- Тексты интерфейса — на русском, в res/values/strings.xml.

Проверка.
./gradlew :app:compileStagingDebugKotlin

После изменений обнови docs/PROJECT_MAP.md.
```
