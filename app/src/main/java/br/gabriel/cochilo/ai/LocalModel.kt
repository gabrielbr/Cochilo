package br.gabriel.cochilo.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * Ask Cochilo's optional on-device model (docs/assistant.md, phase 2). One small interface, so the
 * runtime can change without touching the assistant. Since 0.55.0 there are two:
 * - **LiteRT-LM** (Google's newer runtime) for `.litertlm` files: the 1B Gemma, or Gemma 3n on a
 *   phone with the memory for it;
 * - **MediaPipe** for the older `.task` file, which phones that downloaded it before 0.55.0 keep.
 * [LocalModels.open] reads the file's first bytes to pick. Everything runs on the phone; nothing is
 * sent anywhere.
 */
interface LocalModel {
    /** The model's continuation of [prompt]. Slow (seconds): call it off the main thread. */
    fun generate(prompt: String): String
    /** How many tokens [text] takes, so a prompt never overflows the model's window. */
    fun tokens(text: String): Int
    fun close()

    companion object {
        /** Prompt and answer together; the answers are four short sentences. */
        const val MAX_TOKENS = 1280
        /** Room kept for the answer: a prompt may use the rest, and is trimmed to fit (Grounding.fit). */
        const val REPLY_TOKENS = 320
    }
}

object LocalModels {
    fun open(context: Context, file: File): LocalModel {
        val head = file.inputStream().use { s -> ByteArray(ModelFile.LITERTLM_MAGIC.size).also { s.read(it) } }
        return if (ModelFile.isLiteRtLm(head)) LiteRtLmModel(context, file) else MediaPipeModel(context, file)
    }
}

/** MediaPipe's LLM Inference with a `.task` model file (a small Gemma), kept for files from before 0.55.0. */
@Suppress("DEPRECATION") // MediaPipe's LLM Inference is deprecated in favour of LiteRT-LM, which reads .litertlm.
class MediaPipeModel(context: Context, file: File) : LocalModel {
    private val engine: LlmInference = LlmInference.createFromOptions(
        context,
        LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .setMaxTokens(LocalModel.MAX_TOKENS)
            // CPU: the GPU path crashes natively on some phones, and a 1B model is quick enough on CPU.
            .setPreferredBackend(LlmInference.Backend.CPU)
            .build(),
    )

    override fun generate(prompt: String): String = engine.generateResponse(prompt)

    override fun tokens(text: String): Int = engine.sizeInTokens(text)

    override fun close() = engine.close()
}

/**
 * LiteRT-LM with a `.litertlm` file (0.55.0). One session per answer, on the CPU like MediaPipe.
 * It has no tokenizer call of its own, so [tokens] is counted conservatively from the length
 * ([ModelFile.approxTokens]); a prompt is trimmed a little earlier than it needs to be, never later.
 */
class LiteRtLmModel(context: Context, file: File) : LocalModel {
    private val engine = com.google.ai.edge.litertlm.Engine(
        com.google.ai.edge.litertlm.EngineConfig(
            modelPath = file.absolutePath,
            backend = com.google.ai.edge.litertlm.Backend.CPU(),
            maxNumTokens = LocalModel.MAX_TOKENS,
            cacheDir = context.cacheDir.absolutePath,
        )
    ).also { it.initialize() }

    override fun generate(prompt: String): String =
        engine.createSession().use { it.generateContent(listOf(com.google.ai.edge.litertlm.InputData.Text(prompt))) }

    override fun tokens(text: String): Int = ModelFile.approxTokens(text)

    override fun close() = engine.close()
}
