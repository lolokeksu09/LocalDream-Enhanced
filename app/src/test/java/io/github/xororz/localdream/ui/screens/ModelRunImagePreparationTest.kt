package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** JVM guards for the geometry used by img2img and inpaint uploads. */
class ModelRunImagePreparationTest {
    @Test fun widePhotoUsesCenteredSquareCrop() {
        assertEquals(
            ModelRunCropArea(left = 420, top = 0, width = 1080, height = 1080),
            calculateModelRunCenterCrop(1920, 1080, 512, 512),
        )
    }

    @Test fun portraitPhotoUsesCenteredWideCrop() {
        assertEquals(
            ModelRunCropArea(left = 0, top = 690, width = 1080, height = 540),
            calculateModelRunCenterCrop(1080, 1920, 1024, 512),
        )
    }

    @Test fun matchingAspectRatioDoesNotCropTheSource() {
        assertEquals(
            ModelRunCropArea(left = 0, top = 0, width = 2000, height = 1000),
            calculateModelRunCenterCrop(2000, 1000, 512, 256),
        )
    }

    @Test fun oddPixelDifferenceKeepsOriginalIntegerCenterRounding() {
        assertEquals(
            ModelRunCropArea(left = 0, top = 0, width = 100, height = 100),
            calculateModelRunCenterCrop(101, 100, 512, 512),
        )
    }

    @Test fun referenceImagesAreOnlyRescaledWhenOverOneMegapixel() {
        assertNull(modelRunReferenceTargetSize(1024, 1024))
        assertNull(modelRunReferenceTargetSize(400, 400))
        assertEquals(1024 to 1024, modelRunReferenceTargetSize(4096, 4096))
        assertEquals(1448 to 724, modelRunReferenceTargetSize(2048, 1024))
    }

    @Test(expected = IllegalArgumentException::class)
    fun impossibleGeometryIsRejectedBeforeBitmapOperations() {
        calculateModelRunCenterCrop(1920, 1080, 0, 512)
    }
}
