package com.example.karlthestarfish

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getOpenAiApiKey(): String = System.getenv("OPENAI_API_KEY") ?: ""