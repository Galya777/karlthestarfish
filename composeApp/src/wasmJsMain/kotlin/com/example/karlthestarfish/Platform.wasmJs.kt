package com.example.karlthestarfish

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun getOpenAiApiKey(): String = "" // Wasm would need custom implementation