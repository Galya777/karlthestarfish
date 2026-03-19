package com.example.karlthestarfish

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun getOpenAiApiKey(): String = js("process.env.OPENAI_API_KEY || ''") as String