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
    modifier: Modifier = Modifier
) {
    var currentPoints = remember { mutableStateListOf<Offset>() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
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
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            paths.add(DrawingPath(path, currentPoints.toList()))
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
                        lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                }
                drawPath(
                    path = tempPath,
                    color = Color(0xFF0F172A),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Converts the drawn paths to a transparent PNG Bitmap
 */
fun exportSignatureToBitmap(paths: List<DrawingPath>, width: Int = 600, height: Int = 300): Bitmap? {
    if (paths.isEmpty()) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        style = AndroidPaint.Style.STROKE
        strokeWidth = 6f
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }

    for (p in paths) {
        if (p.points.isNotEmpty()) {
            val aPath = AndroidPath().apply {
                moveTo(p.points.first().x, p.points.first().y)
                for (i in 1 until p.points.size) {
                    lineTo(p.points[i].x, p.points[i].y)
                }
            }
            canvas.drawPath(aPath, paint)
        }
    }

    return bitmap
}
