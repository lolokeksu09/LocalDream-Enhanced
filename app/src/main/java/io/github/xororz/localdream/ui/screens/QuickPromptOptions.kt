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

/** A deterministic cycle: all scene/finish combinations appear before repeating. */
internal fun quickPromptOptions(category: QuickPromptCategory): List<QuickPrompt> {
    val collection = quickPromptCollections.getValue(category)
    return collection.finishes.flatMap { finish ->
        collection.scenes.map { scene ->
            QuickPrompt("$scene, $finish", collection.negativePrompt)
        }
    }
}

internal fun nextQuickPrompt(category: QuickPromptCategory, currentPrompt: String): QuickPrompt {
    val options = quickPromptOptions(category)
    val currentIndex = options.indexOfFirst { it.prompt == currentPrompt }
    return options[(currentIndex + 1) % options.size]
}
