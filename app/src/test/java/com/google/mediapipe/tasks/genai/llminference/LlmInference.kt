package com.google.mediapipe.tasks.genai.llminference

import android.content.Context
import java.io.Closeable

class LlmInference : Closeable {

    fun generateResponse(prompt: String): String {
        return "DECEPTIVE=true\nRISK_SCORE=85\nHEADLINE=Test Dark Pattern\nDETAIL=Test recurring detail."
    }

    override fun close() {}

    class LlmInferenceOptions {
        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder {
            fun setModelPath(path: String): Builder = this
            fun setMaxTokens(tokens: Int): Builder = this
            fun setTemperature(temp: Float): Builder = this
            fun setTopK(topK: Int): Builder = this
            fun build(): LlmInferenceOptions = LlmInferenceOptions()
        }
    }

    companion object {
        fun createFromOptions(context: Context, options: LlmInferenceOptions): LlmInference {
            return LlmInference()
        }
    }
}
