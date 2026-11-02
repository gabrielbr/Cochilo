package br.gabriel.cochilo.domain

/**
 * How the baby looks, for the avatar (docs/roadmap.md 1.2, docs/onboarding.md): what the parent picks
 * at onboarding. The app asks how the baby *looks* (skin tone, hair), not race or ethnicity, and a
 * boy / girl / rather-not-say choice that changes small details only. Stored on the baby as one
 * short text (`skin=3;hair=CURLY;color=BROWN;gender=UNSAID`), so new choices need no migration.
 */
enum class Hair { NONE, LITTLE, CURLY, STRAIGHT, WAVY }
enum class HairColor { BLACK, BROWN, BLOND, RED }
enum class BabyGender { GIRL, BOY, UNSAID }

data class BabyLook(
    /** 1 (lightest) to 6 (deepest), the same range as emoji skin tones. */
    val skin: Int = DEFAULT_SKIN,
    val hair: Hair = Hair.LITTLE,
    val hairColor: HairColor = HairColor.BROWN,
    val gender: BabyGender = BabyGender.UNSAID,
) {
    fun encode(): String = "skin=$skin;hair=${hair.name};color=${hairColor.name};gender=${gender.name}"

    companion object {
        const val SKIN_TONES = 6
        const val DEFAULT_SKIN = 3

        /** Reads what [encode] wrote; anything missing or unknown falls back to the default. */
        fun decode(text: String?): BabyLook {
            if (text.isNullOrBlank()) return BabyLook()
            val m = text.split(';').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i).trim() to part.substring(i + 1).trim()
            }.toMap()
            return BabyLook(
                skin = m["skin"]?.toIntOrNull()?.coerceIn(1, SKIN_TONES) ?: DEFAULT_SKIN,
                hair = m["hair"]?.let { v -> Hair.entries.firstOrNull { it.name == v } } ?: Hair.LITTLE,
                hairColor = m["color"]?.let { v -> HairColor.entries.firstOrNull { it.name == v } } ?: HairColor.BROWN,
                gender = m["gender"]?.let { v -> BabyGender.entries.firstOrNull { it.name == v } } ?: BabyGender.UNSAID,
            )
        }
    }
}
