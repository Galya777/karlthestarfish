package com.example.karlthestarfish

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getOpenAiApiKey(): String