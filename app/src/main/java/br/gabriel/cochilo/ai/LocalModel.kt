package br.gabriel.cochilo.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * Ask Cochilo's optional on-device model (docs/assistant.md, phase 2). One small interface, so the
 * runtime can change (MediaPipe today; LiteRT-LM once the project's Kotlin can read it) without
 * touching the assistant. Everything runs on the phone; nothing is sent anywhere.
 */
interface LocalModel {
    /** The model's continuation of [prompt]. Slow (seconds): call it off the main thread. */
    fun generate(prompt: String): String
    /** How many tokens [text] takes, so a prompt never overflows the model's window. */
    fun tokens(text: String): Int
    fun close()
}

/** MediaPipe's LLM Inference with a `.task` model file (a small Gemma). */
class MediaPipeModel(context: Context, file: File) : LocalModel {
    private val engine: LlmInference = LlmInference.createFromOptions(
        context,
        LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .setMaxTokens(MAX_TOKENS)
            // CPU: the GPU path crashes natively on some phones, and a 1B model is quick enough on CPU.
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build(),
    )

    override fun generate(prompt: String): String = engine.generateResponse(prompt)

    override fun tokens(text: String): Int = engine.sizeInTokens(text)

    override fun close() = engine.close()

    companion object {
        /** Prompt and answer together; the answers are four short sentences. */
        const val MAX_TOKENS = 1280
        /** Room kept for the answer: a prompt may use the rest, and is trimmed to fit (Grounding.fit). */
        const val REPLY_TOKENS = 320
    }
}
