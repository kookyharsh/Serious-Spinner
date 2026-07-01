package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600

@Composable
fun MinimalDial(angle: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 8.dp.toPx()

        // Outer ring
        drawCircle(
            color = Color.White,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner ring
        drawCircle(
            color = Gray600,
            radius = radius - 6.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Tick marks every 15 degrees
        val numTicks = 24
        for (i in 0 until numTicks) {
            val theta = Math.toRadians((i * 360f / numTicks).toDouble())
            val isMajor = i % 6 == 0
            val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
            val startRadius = radius - tickLength
            val startX = center.x + startRadius * kotlin.math.cos(theta).toFloat()
            val startY = center.y + startRadius * kotlin.math.sin(theta).toFloat()
            val endX = center.x + radius * kotlin.math.cos(theta).toFloat()
            val endY = center.y + radius * kotlin.math.sin(theta).toFloat()
            drawLine(
                color = if (isMajor) Color.White else Gray500,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = tickWidth,
                cap = StrokeCap.Round
            )
        }

        // Center dot
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = center
        )

        // Indicator needle
        rotate(angle, center) {
            val needleLength = radius - 10.dp.toPx()
            val needleEndX = center.x
            val needleEndY = center.y - needleLength
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Small indicator circle at tip
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(needleEndX, needleEndY)
            )
        }
    }
}

@Composable
fun BlueDial(angle: Float, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - 12.dp.toPx()

        // Shadow
        drawCircle(
            color = Color(0x33000000),
            radius = radius,
            center = center.copy(x = center.x + 4.dp.toPx(), y = center.y + 6.dp.toPx())
        )
        // White rim
        drawCircle(
            color = Color(0xFFE3F2FD),
            radius = radius + 3.dp.toPx(),
            center = center
        )

        // Ridges
        val numRidges = 36
        for (i in 0 until numRidges) {
            val theta = Math.toRadians((i * 360f / numRidges).toDouble())
            val startX = center.x + (radius - 2.dp.toPx()) * kotlin.math.cos(theta).toFloat()
            val startY = center.y + (radius - 2.dp.toPx()) * kotlin.math.sin(theta).toFloat()
            val endX = center.x + (radius + 6.dp.toPx()) * kotlin.math.cos(theta).toFloat()
            val endY = center.y + (radius + 6.dp.toPx()) * kotlin.math.sin(theta).toFloat()
            drawLine(
                color = Color(0xFF90CAF9),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Main blue body
        drawCircle(
            color = Color(0xFF42A5F5),
            radius = radius,
            center = center
        )
        // Inner gradient highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent),
                center = center.copy(y = center.y - radius * 0.4f),
                radius = radius * 0.8f
            ),
            radius = radius,
            center = center
        )

        // Notch
        rotate(angle, center) {
            val notchCenter = center.copy(y = center.y - radius * 0.7f)
            val notchWidth = 8.dp.toPx()
            val notchHeight = 32.dp.toPx()
            val notchRect = Rect(
                notchCenter.x - notchWidth / 2f,
                notchCenter.y - notchHeight / 2f,
                notchCenter.x + notchWidth / 2f,
                notchCenter.y + notchHeight / 2f
            )
            drawRoundRect(
                color = Color(0xFF1E88E5),
                topLeft = notchRect.topLeft,
                size = notchRect.size,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
