package com.brycewg.asrkb.asr

enum class AsrVendor(val id: String) {
    Volc("volc"),
    SiliconFlow("siliconflow"),
    ElevenLabs("elevenlabs"),
    OpenAI("openai"),
    OpenRouter("openrouter"),
    DashScope("dashscope"),
    Gemini("gemini"),
    Soniox("soniox"),
    StepAudio("stepaudio"),
    Zhipu("zhipu"),
    SenseVoice("sensevoice"),
    FunAsrNano("funasr_nano"),
    Qwen3Asr("qwen3_asr"),
    Parakeet("parakeet"),
    FireRedAsr("firered_asr"),
    Paraformer("paraformer"),
    MiMo("mimo");

    companion object {
        fun fromId(id: String?): AsrVendor = when (id?.lowercase()) {
            SiliconFlow.id -> SiliconFlow
            ElevenLabs.id -> ElevenLabs
            OpenAI.id -> OpenAI
            OpenRouter.id, "open_router" -> OpenRouter
            DashScope.id -> DashScope
            Gemini.id -> Gemini
            Soniox.id -> Soniox
            StepAudio.id, "step_audio", "stepfun" -> StepAudio
            Zhipu.id -> Zhipu
            SenseVoice.id -> SenseVoice
            FunAsrNano.id -> FunAsrNano
            Qwen3Asr.id, "qwen_asr", "qwen3asr" -> Qwen3Asr
            Parakeet.id, "nemo_parakeet" -> Parakeet
            FireRedAsr.id, "telespeech" -> FireRedAsr
            Paraformer.id -> Paraformer
            MiMo.id, "mimo_asr" -> MiMo
            "zipformer" -> Paraformer
            "funasr" -> FunAsrNano
            else -> Volc
        }
    }
}
