package com.printswithme.badgeverify.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.printswithme.badgeverify.ui.theme.BrandPrimary

/**
 * Full-screen overlay composable that draws:
 * - A semi-transparent dark scrim with a transparent "hole" for the viewfinder
 * - Branded corner brackets
 */
@Composable
fun ViewfinderOverlay(
    viewfinderSize: Dp = 250.dp,
    cornerLength: Dp = 36.dp,
    cornerStrokeWidth: Dp = 4.dp,
    cornerRadius: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val vfSize = viewfinderSize.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val left = cx - vfSize / 2f
            val top = cy - vfSize / 2f
            val right = left + vfSize
            val bottom = top + vfSize

            // Semi-transparent scrim with a cutout
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, Size(size.width, size.height)))
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = left, top = top, right = right, bottom = bottom,
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                )
            }
            drawPath(
                path = scrimPath,
                color = Color(0x99000000)
            )

            // Corner brackets
            val cLen = cornerLength.toPx()
            val sw = cornerStrokeWidth.toPx()
            val cr = cornerRadius.toPx()
            val strokeStyle = Stroke(width = sw, cap = StrokeCap.Round)
            val bColor = BrandPrimary

            // Top-left
            drawPath(Path().apply {
                moveTo(left, top + cLen)
                lineTo(left, top + cr)
                arcTo(Rect(left, top, left + cr * 2, top + cr * 2), 180f, 90f, false)
                lineTo(left + cLen, top)
            }, bColor, style = strokeStyle)

            // Top-right
            drawPath(Path().apply {
                moveTo(right - cLen, top)
                lineTo(right - cr, top)
                arcTo(Rect(right - cr * 2, top, right, top + cr * 2), 270f, 90f, false)
                lineTo(right, top + cLen)
            }, bColor, style = strokeStyle)

            // Bottom-left
            drawPath(Path().apply {
                moveTo(left, bottom - cLen)
                lineTo(left, bottom - cr)
                arcTo(Rect(left, bottom - cr * 2, left + cr * 2, bottom), 180f, -90f, false)
                lineTo(left + cLen, bottom)
            }, bColor, style = strokeStyle)

            // Bottom-right
            drawPath(Path().apply {
                moveTo(right - cLen, bottom)
                lineTo(right - cr, bottom)
                arcTo(Rect(right - cr * 2, bottom - cr * 2, right, bottom), 90f, -90f, false)
                lineTo(right, bottom - cLen)
            }, bColor, style = strokeStyle)
        }
    }
}
