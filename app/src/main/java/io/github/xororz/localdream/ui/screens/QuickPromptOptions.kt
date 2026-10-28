package io.github.xororz.localdream.ui.screens

internal enum class QuickPromptCategory { PORTRAIT, ANIME, REALISM }

internal data class QuickPrompt(val prompt: String, val negativePrompt: String)

private data class PromptCollection(
    val scenes: List<String>,
    val finishes: List<String>,
    val negativePrompt: String,
)

private val quickPromptCollections = mapOf(
    QuickPromptCategory.PORTRAIT to PromptCollection(
        scenes = listOf(
            "a close-up portrait of a traveler, thoughtful expression",
            "an artist in a sunlit studio, holding a paintbrush",
            "a musician backstage, holding a violin",
            "a scientist in a modern laboratory, confident expression",
            "a writer by a window, surrounded by books",
            "a dancer in a rehearsal room, elegant pose",
            "a gardener among blooming flowers, warm smile",
            "a barista in a cozy cafe, candid moment",
            "a fashion model in a minimalist studio, poised expression",
            "a sailor on the shore, wind in their hair",
            "a photographer exploring an old city, camera in hand",
            "an adventurer in a mountain landscape, wearing a weathered jacket",
        ),
        finishes = listOf(
            "soft window light, natural skin texture, shallow depth of field",
            "golden hour light, warm tones, gentle background blur",
            "dramatic side lighting, deep shadows, cinematic composition",
            "soft overcast light, muted colors, delicate details",
            "studio lighting, neutral backdrop, crisp focus",
            "colorful neon reflections, atmospheric night lighting",
            "backlit by the evening sun, subtle rim light",
            "black and white portrait, rich tonal contrast",
        ),
        negativePrompt = "blurry, low quality, distorted face, bad anatomy, extra fingers, watermark",
    ),
    QuickPromptCategory.ANIME to PromptCollection(
        scenes = listOf(
            "anime illustration of a traveler walking through a lantern-lit street",
            "anime illustration of a student reading in a quiet library",
            "anime illustration of a swordswoman beneath cherry blossoms",
            "anime illustration of a musician playing on a rooftop",
            "anime illustration of a mage exploring an ancient forest",
            "anime illustration of a pilot beside a futuristic aircraft",
            "anime illustration of friends at a summer festival",
            "anime illustration of a detective in a rainy city",
            "anime illustration of a cook preparing food in a cozy kitchen",
            "anime illustration of an adventurer crossing a mountain bridge",
            "anime illustration of a guardian standing in a moonlit garden",
            "anime illustration of a girl watching the ocean from a train",
        ),
        finishes = listOf(
            "clean linework, expressive eyes, vibrant colors",
            "soft pastel palette, gentle light, delicate details",
            "cinematic lighting, atmospheric depth, detailed background",
            "watercolor texture, airy colors, soft edges",
            "retro anime aesthetic, cel shading, warm film grain",
            "dramatic shadows, rich colors, dynamic composition",
            "dreamlike light, luminous highlights, intricate scenery",
            "manga-inspired ink lines, limited color palette, high contrast",
        ),
        negativePrompt = "blurry, low quality, distorted face, bad anatomy, extra fingers, watermark",
    ),
    QuickPromptCategory.REALISM to PromptCollection(
        scenes = listOf(
            "a small cafe on a quiet street after rain",
            "a mountain lake surrounded by pine trees",
            "an old stone alley with flowers in the windows",
            "a lighthouse standing above a rugged coast",
            "a modern train passing through a snowy valley",
            "a market stall filled with colorful fruit",
            "a cabin in a misty forest",
            "a city skyline reflected in a river",
            "a wild horse running across an open field",
            "a cozy room with books and a cup of tea",
            "a sailboat drifting near a rocky island",
            "a botanical garden filled with tropical plants",
        ),
        finishes = listOf(
            "realistic photograph, natural light, detailed textures",
            "realistic photograph, golden hour, warm colors",
            "realistic photograph, overcast sky, soft diffuse light",
            "realistic photograph, blue hour, subtle atmospheric haze",
            "realistic photograph, dramatic storm light, rich contrast",
            "realistic photograph, morning mist, subdued colors",
            "realistic photograph, clear midday light, crisp detail",
            "realistic photograph, cinematic composition, natural colors",
        ),
        negativePrompt = "blurry, low quality, oversaturated, distorted details, watermark",
    ),
)

internal enum class PromptFormat { UNIVERSAL, TAGS, DESCRIPTION }
internal enum class PromptFormatChoice { AUTO, UNIVERSAL, TAGS, DESCRIPTION }

internal fun promptFormatChoice(saved: String?): PromptFormatChoice =
    PromptFormatChoice.values().firstOrNull { it.name.equals(saved, ignoreCase = true) }
        ?: PromptFormatChoice.AUTO

internal fun resolvePromptFormat(
    choice: PromptFormatChoice,
    metadata: String?,
    isKnownQwen: Boolean,
): PromptFormat {
    if (choice != PromptFormatChoice.AUTO) return PromptFormat.valueOf(choice.name)
    val explicit = PromptFormat.values().firstOrNull { it.name.equals(metadata, ignoreCase = true) }
    return explicit ?: if (isKnownQwen) PromptFormat.DESCRIPTION else PromptFormat.UNIVERSAL
}

private val tagScenes = mapOf(
    QuickPromptCategory.PORTRAIT to listOf(
        "portrait, traveler, thoughtful expression",
        "portrait, artist, paintbrush, art studio",
        "portrait, musician, violin, backstage",
        "portrait, scientist, laboratory, confident expression",
        "portrait, writer, window, books",
        "portrait, dancer, rehearsal room, elegant pose",
        "portrait, gardener, flowers, smile",
        "portrait, barista, cafe, candid",
        "portrait, fashion model, studio, poised expression",
        "portrait, sailor, seashore, windblown hair",
        "portrait, photographer, old city, camera",
        "portrait, adventurer, mountains, weathered jacket",
    ),
    QuickPromptCategory.ANIME to listOf(
        "anime style, traveler, lanterns, street",
        "anime style, student, reading, library",
        "anime style, swordswoman, cherry blossoms",
        "anime style, musician, rooftop",
        "anime style, mage, ancient forest",
        "anime style, pilot, futuristic aircraft",
        "anime style, friends, summer festival",
        "anime style, detective, rainy city",
        "anime style, cook, cozy kitchen",
        "anime style, adventurer, mountain bridge",
        "anime style, guardian, moonlit garden",
        "anime style, girl, train window, ocean",
    ),
    QuickPromptCategory.REALISM to listOf(
        "photograph, cafe, street, after rain",
        "photograph, mountain lake, pine trees",
        "photograph, stone alley, flowers, windows",
        "photograph, lighthouse, rocky coast",
        "photograph, train, snowy valley",
        "photograph, market stall, colorful fruit",
        "photograph, cabin, misty forest",
        "photograph, city skyline, river reflection",
        "photograph, wild horse, open field",
        "photograph, cozy room, books, cup of tea",
        "photograph, sailboat, rocky island",
        "photograph, botanical garden, tropical plants",
    ),
)

private val tagFinishes = mapOf(
    QuickPromptCategory.PORTRAIT to listOf(
        "window light, natural skin texture, shallow depth of field",
        "golden hour, warm colors, background blur",
        "side lighting, deep shadows, cinematic",
        "overcast lighting, muted colors, delicate details",
        "studio lighting, neutral background, sharp focus",
        "neon reflections, night lighting",
        "backlighting, evening sun, rim light",
        "monochrome, tonal contrast",
    ),
    QuickPromptCategory.ANIME to listOf(
        "clean linework, expressive eyes, vibrant colors",
        "pastel colors, soft light, delicate details",
        "cinematic lighting, detailed background, depth",
        "watercolor texture, soft edges, airy colors",
        "retro anime, cel shading, film grain",
        "dramatic shadows, rich colors, dynamic composition",
        "dreamlike light, luminous highlights, intricate scenery",
        "manga, ink lines, limited palette, high contrast",
    ),
    QuickPromptCategory.REALISM to listOf(
        "natural light, detailed textures",
        "golden hour, warm colors",
        "overcast sky, soft light",
        "blue hour, atmospheric haze",
        "storm light, rich contrast",
        "morning mist, subdued colors",
        "midday light, crisp detail",
        "cinematic composition, natural colors",
    ),
)

/** Each format cycles through every scene/finish combination before repeating. */
internal fun quickPromptOptions(
    category: QuickPromptCategory,
    format: PromptFormat = PromptFormat.UNIVERSAL,
): List<QuickPrompt> {
    val collection = quickPromptCollections.getValue(category)
    return collection.finishes.flatMapIndexed { finishIndex, finish ->
        collection.scenes.mapIndexed { sceneIndex, scene ->
            val prompt = when (format) {
                PromptFormat.UNIVERSAL -> "$scene, $finish"
                PromptFormat.TAGS ->
                    "${tagScenes.getValue(category)[sceneIndex]}, ${tagFinishes.getValue(category)[finishIndex]}"
                PromptFormat.DESCRIPTION ->
                    "A scene showing $scene. The image has $finish."
            }
            QuickPrompt(prompt, collection.negativePrompt)
        }
    }
}

internal fun nextQuickPrompt(
    category: QuickPromptCategory,
    currentPrompt: String,
    format: PromptFormat = PromptFormat.UNIVERSAL,
): QuickPrompt {
    val options = quickPromptOptions(category, format)
    val currentIndex = options.indexOfFirst { it.prompt == currentPrompt }
    return options[(currentIndex + 1) % options.size]
}

/** Picks a different ready-made idea across styles without overwriting user text. */
internal fun randomQuickPrompt(
    currentPrompt: String,
    format: PromptFormat = PromptFormat.UNIVERSAL,
    random: kotlin.random.Random = kotlin.random.Random.Default,
): QuickPrompt {
    val candidates = QuickPromptCategory.values()
        .flatMap { quickPromptOptions(it, format) }
        .filterNot { it.prompt == currentPrompt }
    return candidates.random(random)
}
