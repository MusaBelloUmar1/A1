package com.musx.a1.domain.engine

class NarrationEngine {
    /**
     * Processes raw text through the full narration pipeline.
     */
    fun process(rawText: String): List<SpeechInstruction> {
        val normalized = TextNormalizer.normalize(rawText)
        val sentences = SentenceSplitter.split(normalized)

        return sentences.map { sentence ->
            val emotion = EmotionTagger.tag(sentence)
            SpeechPlanner.plan(sentence, emotion)
        }
    }
}
