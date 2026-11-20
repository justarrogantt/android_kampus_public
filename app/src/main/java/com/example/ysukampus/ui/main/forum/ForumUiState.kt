package com.example.ysukampus.ui.main.forum

import android.net.Uri

/**
 * Снимок вкладки «Форум».
 *
 * Первая загрузка и догрузка страницы разведены ([isLoading] против
 * [isLoadingMore]): скелетон показывается только пока лента пуста, а внизу
 * списка при подгрузке крутится индикатор.
 *
 * Тред и лента живут в одном состоянии: [openedPostId] решает, что рисует
 * экран, а всё, что относится к открытому треду, лежит в [thread].
 */
data class ForumUiState(
    val posts: List<ForumPost> = emptyList(),

    /** Первая загрузка: лента ещё пуста, рисуем скелетон. */
    val isLoading: Boolean = true,
    /** Идёт запрос следующей страницы. */
    val isLoadingMore: Boolean = false,
    /** Ошибка первой загрузки — вместо ленты показываем повтор. */
    val error: String? = null,

    /** Курсор следующей страницы. `null` при непустой ленте — постов больше нет. */
    val nextCursor: String? = null,

    /** Пост, для которого открыто подтверждение жалобы/удаления. */
    val reportPostId: String? = null,
    val deletePostId: String? = null,

    /** Открытый тред. `null` — на экране лента. */
    val openedPostId: String? = null,
    /** Состояние открытого треда. Осмысленно только при непустом [openedPostId]. */
    val thread: ForumThreadUiState = ForumThreadUiState(),

    /** Форма нового поста: лист открыт, черновик, вложение и признак отправки. */
    val isComposerOpen: Boolean = false,
    val composerDraft: String = "",
    val composerImage: ForumImageDraft = ForumImageDraft(),
    val isPublishing: Boolean = false,

    /**
     * Картинка, открытая на весь экран (оригинал, без `size=preview`).
     * `null` — просмотрщик закрыт.
     */
    val viewerImageUrl: String? = null,
    /** Идёт сохранение открытой картинки в галерею. */
    val isSavingImage: Boolean = false,

    /** Разовое сообщение для снекбара. Экран гасит его через onMessageShown(). */
    val message: String? = null
) {
    /** Загрузка закончилась без ошибки, но постов нет. */
    val isEmpty: Boolean
        get() = !isLoading && error == null && posts.isEmpty()

    /** Есть ли что догружать: курсор не пуст и предыдущая страница уже пришла. */
    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoading && !isLoadingMore

    /**
     * Черновик поста готов к отправке.
     *
     * Бэк принимает пост с текстом или с картинкой, поэтому хватает одного из
     * двух; пока вложение грузится, отправлять нечего — `image_id` ещё не пришёл.
     */
    val canPublish: Boolean
        get() = !isPublishing &&
            !composerImage.isUploading &&
            (composerDraft.isNotBlank() || composerImage.attachmentId != null) &&
            composerDraft.length <= FORUM_CONTENT_MAX_LENGTH
}

/**
 * Картинка, прикреплённая к черновику поста или комментария.
 *
 * Файл уходит на сервер сразу при выборе: [uri] нужен только для превью, а
 * публикация отправляет уже полученный [attachmentId].
 */
data class ForumImageDraft(
    val uri: Uri? = null,
    val attachmentId: String? = null,
    val isUploading: Boolean = false
) {
    /** Есть что показывать в превью черновика. */
    val hasImage: Boolean
        get() = uri != null
}

/**
 * Снимок открытого треда: сам пост, его комментарии и форма ответа.
 *
 * Комментарии листаются тем же курсором, что и лента, поэтому загрузка
 * первой страницы и догрузка разведены так же.
 */
data class ForumThreadUiState(
    val post: ForumPost? = null,
    val comments: List<ForumComment> = emptyList(),

    /** Первая загрузка треда: ни поста, ни комментариев ещё нет. */
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    /** Ошибка первой загрузки — вместо треда показываем повтор. */
    val error: String? = null,

    val nextCursor: String? = null,

    /** Черновик комментария, вложение и признак отправки. */
    val draft: String = "",
    val image: ForumImageDraft = ForumImageDraft(),
    val isSending: Boolean = false,

    /** Комментарий, для которого открыто подтверждение жалобы/удаления. */
    val reportCommentId: String? = null,
    val deleteCommentId: String? = null,

    /**
     * Комментарий, к которому надо прокрутить тред и подсветить его.
     *
     * Приходит из уведомления: бэк присылает `post_id` и `comment_id`, а
     * комментарий может лежать и не на первой странице — тогда модель
     * дотягивает страницы, пока он не найдётся. Гасится после подсветки.
     */
    val focusCommentId: String? = null,

    /**
     * Счётчик упоров черновика в предел длины.
     *
     * Растёт на каждую попытку набрать 500-й символ и дальше. Именно счётчик,
     * а не флаг: набранный впустую символ черновик не меняет, и по самому
     * тексту отличить вторую попытку от первой нельзя — а отклик нужен на
     * каждую.
     */
    val draftLimitSignal: Int = 0
) {
    /** Тред загружен, а комментариев нет. */
    val isEmpty: Boolean
        get() = !isLoading && error == null && comments.isEmpty()

    val canLoadMore: Boolean
        get() = nextCursor != null && !isLoading && !isLoadingMore

    /** Черновик комментария готов к отправке — как и пост, текстом или картинкой. */
    val canSend: Boolean
        get() = !isSending &&
            !image.isUploading &&
            (draft.isNotBlank() || image.attachmentId != null) &&
            draft.length <= FORUM_CONTENT_MAX_LENGTH
}

/** Предел длины поста и комментария на бэке (`forum_schemas.py`). */
const val FORUM_CONTENT_MAX_LENGTH = 500
