package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Difficulty
import kotlin.math.atan2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onLeaderboardClick: () -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWin: (Int) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val message by viewModel.message.collectAsState()
    val currentDifficulty by viewModel.currentDifficulty.collectAsState()
    val submissionResult by viewModel.submissionResult.collectAsState()
    val currentSpinner by viewModel.prefs.currentSpinner.collectAsState(initial = "DEFAULT")
    val points by viewModel.prefs.points.collectAsState(initial = 0)
    
    // Add logic to clear message after a delay
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(submissionResult) {
        if (submissionResult != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSubmissionResult()
        }
    }

    if (gameState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val state = gameState!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (Streak and Difficulty)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "STREAK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${state.streak}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${state.level}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "POINTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "$points",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShopClick) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Shop", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onLeaderboardClick) {
                    Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "TARGET",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 2.sp
        )
        Text(
            text = "${state.targetValue}",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Difficulty.values().forEach { diff ->
                TextButton(
                    onClick = { viewModel.setDifficulty(diff) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (currentDifficulty == diff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(diff.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Minimalist Dial
        var lastAngle by remember { mutableStateOf<Float?>(null) }
        var isDragging by remember { mutableStateOf(false) }

        val spinnerConfig = com.example.data.SpinnerProvider.getSpinner(currentSpinner)
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(if (spinnerConfig.isCanvasBased && currentSpinner != "DEFAULT") MaterialTheme.colorScheme.surface else Color.Transparent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            viewModel.clearSubmissionResult()
                            viewModel.clearMessage()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                            lastAngle = Math.toDegrees(angleRad.toDouble()).toFloat()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val pos = change.position
                            
                            val dx = pos.x - center.x
                            val dy = pos.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                            
                            if (distance > 0) {
                                // Normalized tangent vector
                                val tangentX = -dy / distance
                                val tangentY = dx / distance
                                
                                // Project drag amount onto the tangent vector
                                val tangentialDrag = dragAmount.x * tangentX + dragAmount.y * tangentY
                                
                                // Convert tangential drag to angle delta (assuming grab is at the edge of the wheel)
                                val assumedRadius = size.width / 2f
                                val angleDelta = (tangentialDrag / assumedRadius) * (180f / Math.PI.toFloat())
                                
                                viewModel.updateAngle(angleDelta)
                            }
                        },
                        onDragEnd = { 
                            lastAngle = null
                            isDragging = false
                        },
                        onDragCancel = { 
                            lastAngle = null
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (spinnerConfig.isCanvasBased) {
                if (currentSpinner == "DEFAULT") {
                    BlueDial(angle = state.currentAngle, modifier = Modifier.fillMaxSize())
                } else {
                    val indicatorColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.width / 2 - 20.dp.toPx()
                        
                        // Track
                        drawCircle(
                            color = Color.DarkGray,
                            radius = radius,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        
                        // Indicator line based on angle
                        val angleRad = Math.toRadians((state.currentAngle - 90).toDouble())
                        val startX = center.x + (radius - 30.dp.toPx()) * Math.cos(angleRad).toFloat()
                        val startY = center.y + (radius - 30.dp.toPx()) * Math.sin(angleRad).toFloat()
                        val endX = center.x + (radius + 15.dp.toPx()) * Math.cos(angleRad).toFloat()
                        val endY = center.y + (radius + 15.dp.toPx()) * Math.sin(angleRad).toFloat()
                        
                        drawLine(
                            color = indicatorColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            } else {
                spinnerConfig.shellResId?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Shell",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                spinnerConfig.stickResId?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Stick",
                        modifier = Modifier.fillMaxSize().rotate(state.currentAngle)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Attempts: ${state.attempts}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        AnimatedVisibility(
            visible = submissionResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val result = submissionResult ?: return@AnimatedVisibility
            val currentSpins = result.preciseSpins
            val targetSpins = result.targetSpins.toFloat()
            val diffSpins = abs(currentSpins - targetSpins)
            val precisionScore = maxOf(0f, (1f - diffSpins) * 100f)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("precision_score_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Precision Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val scoreColor = when {
                        precisionScore >= 95f -> MaterialTheme.colorScheme.primary
                        precisionScore >= 80f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                    
                    val scoreText = when {
                        precisionScore >= 99f -> "Flawless!"
                        precisionScore >= 95f -> "Excellent!"
                        precisionScore >= 90f -> "Very Close!"
                        precisionScore >= 75f -> "On the right track"
                        else -> "Keep adjusting!"
                    }
                    
                    Text(
                        text = String.format("%.1f%%", precisionScore),
                        style = MaterialTheme.typography.displaySmall,
                        color = scoreColor,
                        fontWeight = FontWeight.Black
                    )
                    
                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scoreColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Subtitle with details
                    Text(
                        text = "You spun ${String.format("%.2f", currentSpins)} / ${result.targetSpins}.00 spins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val diffFormatted = if (currentSpins < targetSpins) {
                        String.format("-%.2f spins needed", diffSpins)
                    } else {
                        String.format("+%.2f spins over", diffSpins)
                    }
                    
                    Text(
                        text = if (diffSpins == 0f) "Perfect match!" else diffFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diffSpins == 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        if (message != null) {
            Text(
                text = message!!,
                color = if (message!!.startsWith("Perfect!")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.submit(onWin = onWin, onFail = {}) },
                modifier = Modifier
                    .width(200.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SUBMIT", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
