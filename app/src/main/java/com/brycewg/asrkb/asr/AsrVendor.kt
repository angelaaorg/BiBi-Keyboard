package com.brycewg.asrkb.asr

enum class AsrVendor(val id: String) {
    Volc("volc"),
    SiliconFlow("siliconflow"),
    ElevenLabs("elevenlabs"),
    OpenAI("openai"),
    DashScope("dashscope"),
    Gemini("gemini"),
    Soniox("soniox"),
    Zhipu("zhipu"),
    SenseVoice("sensevoice"),
    FunAsrNano("funasr_nano"),
    Qwen3Asr("qwen3_asr"),
    FireRedAsr("firered_asr"),
    Paraformer("paraformer");

    companion object {
        fun fromId(id: String?): AsrVendor = when (id?.lowercase()) {
            SiliconFlow.id -> SiliconFlow
            ElevenLabs.id -> ElevenLabs
            OpenAI.id -> OpenAI
            DashScope.id -> DashScope
            Gemini.id -> Gemini
            Soniox.id -> Soniox
            Zhipu.id -> Zhipu
            SenseVoice.id -> SenseVoice
            FunAsrNano.id -> FunAsrNano
            Qwen3Asr.id, "qwen_asr", "qwen3asr" -> Qwen3Asr
            FireRedAsr.id, "telespeech" -> FireRedAsr
            Paraformer.id -> Paraformer
            "zipformer" -> Paraformer
            "funasr" -> FunAsrNano
            else -> Volc
        }
    }
}
