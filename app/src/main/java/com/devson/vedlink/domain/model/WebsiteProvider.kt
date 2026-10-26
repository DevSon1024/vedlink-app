package com.devson.vedlink.domain.model

enum class WebsiteProvider {
    YOUTUBE,
    GITHUB,
    INSTAGRAM,
    REDDIT,
    SPOTIFY,
    IMDB,
    WIKIPEDIA,
    AMAZON,
    MEDIUM,
    THREADS,
    FACEBOOK,
    STEAM,
    PINTEREST,
    LINKEDIN,
    TIKTOK,
    STACK_OVERFLOW,
    DEV_TO,
    PLAY_STORE,
    APP_STORE,
    LETTERBOXD,
    NETFLIX,
    PRIME_VIDEO,
    GENERIC;

    val isVideo: Boolean
        get() = this == YOUTUBE || this == NETFLIX || this == PRIME_VIDEO

    val isCode: Boolean
        get() = this == GITHUB || this == STACK_OVERFLOW

    val isArticle: Boolean
        get() = this == MEDIUM || this == DEV_TO || this == WIKIPEDIA

    val isSocial: Boolean
        get() = this == REDDIT || this == INSTAGRAM || this == THREADS || this == FACEBOOK || this == LINKEDIN || this == PINTEREST || this == TIKTOK
}
