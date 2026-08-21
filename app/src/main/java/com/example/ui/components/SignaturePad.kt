package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

data class DrawingPath(
    val path: Path,
    val points: List<Offset>,
    val color: Color = Color(0xFF0F172A),
    val strokeWidth: Float = 6f
)

@Composable
fun SignaturePad(
    paths: MutableList<DrawingPath>,
    onPathAdded: () -> Unit,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color(0xFF0F172A),
    strokeWidth: Float = 6f
) {
    val currentPoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .pointerInput(strokeColor, strokeWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints.clear()
                        currentPoints.add(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints.add(change.position)
                    },
                    onDragEnd = {
                        if (currentPoints.size > 1) {
                            val path = Path().apply {
                                moveTo(currentPoints.first().x, currentPoints.first().y)
                                for (i in 1 until currentPoints.size) {
                                    val prev = currentPoints[i - 1]
                                    val curr = currentPoints[i]
                                    val midX = (prev.x + curr.x) / 2f
                                    val midY = (prev.y + curr.y) / 2f
                                    quadraticTo(prev.x, prev.y, midX, midY)
                                }
                                lineTo(currentPoints.last().x, currentPoints.last().y)
                            }
                            paths.add(DrawingPath(path, currentPoints.toList(), strokeColor, strokeWidth))
                            currentPoints.clear()
                            onPathAdded()
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (drawnPath in paths) {
                drawPath(
                    path = drawnPath.path,
                    color = drawnPath.color,
                    style = Stroke(
                        width = drawnPath.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            if (currentPoints.size > 1) {
                val tempPath = Path().apply {
                    moveTo(currentPoints.first().x, currentPoints.first().y)
                    for (i in 1 until currentPoints.size) {
                        val prev = currentPoints[i - 1]
                        val curr = currentPoints[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    lineTo(currentPoints.last().x, currentPoints.last().y)
                }
                drawPath(
                    path = tempPath,
                    color = strokeColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Converts the drawn paths to a transparent PNG Bitmap with tight bounding box.
 */
fun exportSignatureToBitmap(paths: List<DrawingPath>, width: Int = 800, height: Int = 400): Bitmap? {
    if (paths.isEmpty()) return null

    // Find bounding box across all paths
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (p in paths) {
        for (pt in p.points) {
            minX = min(minX, pt.x)
            minY = min(minY, pt.y)
            maxX = max(maxX, pt.x)
            maxY = max(maxY, pt.y)
        }
    }

    val padding = 16f
    minX = max(0f, minX - padding)
    minY = max(0f, minY - padding)
    maxX = maxX + padding
    maxY = maxY + padding

    val boundW = (maxX - minX).toInt().coerceAtLeast(50)
    val boundH = (maxY - minY).toInt().coerceAtLeast(30)

    val bitmap = Bitmap.createBitmap(boundW, boundH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    for (p in paths) {
        if (p.points.isNotEmpty()) {
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(
                    (p.color.alpha * 255).toInt(),
                    (p.color.red * 255).toInt(),
                    (p.color.green * 255).toInt(),
                    (p.color.blue * 255).toInt()
                )
                style = AndroidPaint.Style.STROKE
                strokeWidth = p.strokeWidth
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }

            val aPath = AndroidPath().apply {
                moveTo(p.points.first().x - minX, p.points.first().y - minY)
                for (i in 1 until p.points.size) {
                    val prev = p.points[i - 1]
                    val curr = p.points[i]
                    val midX = (prev.x + curr.x) / 2f - minX
                    val midY = (prev.y + curr.y) / 2f - minY
                    quadTo(prev.x - minX, prev.y - minY, midX, midY)
                }
                lineTo(p.points.last().x - minX, p.points.last().y - minY)
            }
            canvas.drawPath(aPath, paint)
        }
    }

    return bitmap
}

/**
 * Remove bright/white background from an imported image to produce a crisp transparent signature stamp.
 */
fun processImportedSignature(sourceBitmap: Bitmap, threshold: Int = 220): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val brightness = (r + g + b) / 3

        if (brightness > threshold) {
            pixels[i] = AndroidColor.TRANSPARENT
        } else {
            val alpha = ((255 - brightness) * 255 / (255 - threshold + 1)).coerceIn(0, 255)
            pixels[i] = AndroidColor.argb(alpha, r, g, b)
        }
    }

    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}
