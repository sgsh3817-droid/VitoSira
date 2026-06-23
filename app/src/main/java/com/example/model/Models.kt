package com.example.model

import com.squareup.moshi.JsonClass

enum class LayerType {
    IMAGE, TEXT, STICKER, SHAPE, DRAWING
}

enum class ShapeType {
    RECTANGLE, CIRCLE, TRIANGLE, STAR, ARROW, LINE
}

@JsonClass(generateAdapter = true)
data class Point2D(val x: Float, val y: Float)

@JsonClass(generateAdapter = true)
data class Vector2D(val x: Float, val y: Float)

@JsonClass(generateAdapter = true)
data class ColorData(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int,
    val isGradient: Boolean = false,
    val gradientColors: List<ColorData>? = null
) {
    fun toColor(): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color(red, green, blue, alpha)
    }

    companion object {
        val White = ColorData(255, 255, 255, 255)
        val Black = ColorData(0, 0, 0, 255)
        val Transparent = ColorData(0, 0, 0, 0)
        val Red = ColorData(255, 0, 0, 255)
        val Green = ColorData(0, 255, 0, 255)
        val Blue = ColorData(0, 0, 255, 255)
        val Yellow = ColorData(255, 255, 0, 255)

        fun fromColor(color: androidx.compose.ui.graphics.Color): ColorData {
            return ColorData(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
                (color.alpha * 255).toInt()
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class TextProperties(
    val text: String = "Double Tap to Edit",
    val fontSize: Float = 24f,
    val color: ColorData = ColorData.Black,
    val gradientColors: List<ColorData>? = null,
    val fontFamilyName: String = "Sans-Serif",
    val opacity: Float = 1.0f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val isUppercase: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 28f,
    val strokeWidth: Float = 0f,
    val strokeColor: ColorData = ColorData.Transparent,
    val shadowColor: ColorData = ColorData.Transparent,
    val shadowRadius: Float = 0f,
    val shadowOffsetX: Float = 0f,
    val shadowOffsetY: Float = 0f,
    val backgroundColor: ColorData = ColorData.Transparent,
    val curvingRadius: Float = 0f, // 0 means no curve
    val textEffect: String = "Normal" // Normal, Glow, Outline, Neon, 3D, Shadow
)

@JsonClass(generateAdapter = true)
data class ShapeProperties(
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val fillColor: ColorData = ColorData.Blue,
    val borderColor: ColorData = ColorData.Black,
    val borderWidth: Float = 2f,
    val opacity: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class StrokePath(
    val points: List<Point2D>,
    val color: ColorData = ColorData.Black,
    val brushSize: Float = 5f,
    val opacity: Float = 1.0f,
    val isEraser: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DrawingProperties(
    val strokes: List<StrokePath> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ImageProperties(
    val imageSrc: String, // String representation e.g. base64 or a standard template resource identifier
    val isLocalAsset: Boolean = false
)

@JsonClass(generateAdapter = true)
data class StickerProperties(
    val type: String, // "emoji" or "sticker"
    val codeOrName: String, // Emoji UTF character or Sticker shape path/name
    val tintColor: ColorData? = null
)

@JsonClass(generateAdapter = true)
data class FilterProperties(
    val name: String = "Original", // Original, Vintage, Retro, Warm, Cool, Black & White, HDR, etc.
    val brightness: Float = 0f, // -1f to 1f
    val contrast: Float = 0f, // -1f to 1f
    val saturation: Float = 0f, // -1f to 1f
    val exposure: Float = 0f, // -1f to 1f
    val temperature: Float = 0f, // -1f to 1f
    val tint: Float = 0f, // -1f to 1f
    val vibrance: Float = 0f, // -1f to 1f
    val highlights: Float = 0f, // -1f to 1f
    val shadows: Float = 0f, // -1f to 1f
    val opacity: Float = 1f
)

@JsonClass(generateAdapter = true)
data class DesignLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val textProperties: TextProperties? = null,
    val shapeProperties: ShapeProperties? = null,
    val drawingProperties: DrawingProperties? = null,
    val imageProperties: ImageProperties? = null,
    val stickerProperties: StickerProperties? = null
)

@JsonClass(generateAdapter = true)
data class ProjectMetadata(
    val id: Int = 0,
    val name: String,
    val width: Int,
    val height: Int,
    val unit: String = "Pixels", // Pixels, Inches, CM, MM
    val thumbnailBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)
