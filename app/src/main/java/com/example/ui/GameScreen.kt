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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
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

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }
    
    var lastDragTime by remember { mutableStateOf(0L) }
    var dragVelocity by remember { mutableStateOf(0f) }
    var inertiaJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    val wobbleAnim = remember { Animatable(0f) }
    val rippleAnim = remember { Animatable(0f) }
    val animatedScore = remember { Animatable(0f) }
    
    // Add logic to clear message after a delay
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(submissionResult) {
        if (submissionResult != null) {
            val result = submissionResult!!
            val currentSpins = result.preciseSpins
            val targetSpins = result.targetSpins.toFloat()
            val diffSpins = abs(currentSpins - targetSpins)
            val score = maxOf(0f, (1f - diffSpins) * 100f)
            
            animatedScore.snapTo(0f)
            launch {
                animatedScore.animateTo(
                    targetValue = score,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                )
            }
            
            delay(4000)
            viewModel.clearSubmissionResult()
        } else {
            animatedScore.snapTo(0f)
        }
    }

    val currentSpinsCount = abs((gameState?.totalRotation ?: 0f) / 360f).toInt()
    LaunchedEffect(currentSpinsCount) {
        if (currentSpinsCount != 0) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
            )
        }
    }

    if (gameState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val state = gameState!!

    val bottomDeckAlpha by animateFloatAsState(
        targetValue = if (isDragging || isSpinning) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "BottomDeckAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 80.dp),
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
        val spinnerConfig = com.example.data.SpinnerProvider.getSpinner(currentSpinner)
        val displayedAngle = state.currentAngle + wobbleAnim.value

        val handleDragRelease = {
            isDragging = false
            lastAngle = null
            
            if (abs(dragVelocity) > 0.05f) {
                inertiaJob = coroutineScope.launch {
                    isSpinning = true
                    var velocity = dragVelocity.coerceIn(-3f, 3f)
                    val friction = 0.96f
                    var lastTime = System.currentTimeMillis()
                    while (abs(velocity) > 0.01f) {
                        val now = System.currentTimeMillis()
                        val dt = (now - lastTime).coerceIn(1, 50)
                        lastTime = now
                        val deltaAngle = velocity * dt
                        viewModel.updateAngle(deltaAngle)
                        velocity *= friction
                        
                        try {
                            androidx.compose.runtime.withFrameMillis { }
                        } catch (e: Exception) {
                            kotlinx.coroutines.delay(16)
                        }
                    }
                    isSpinning = false
                    
                    wobbleAnim.snapTo(if (dragVelocity > 0) 6f else -6f)
                    wobbleAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            } else {
                coroutineScope.launch {
                    wobbleAnim.snapTo(if (state.totalRotation > 0) 2.5f else -2.5f)
                    wobbleAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(250.dp)
                .background(if (spinnerConfig.isCanvasBased && currentSpinner != "DEFAULT") MaterialTheme.colorScheme.surface else Color.Transparent, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            inertiaJob?.cancel()
                            viewModel.clearSubmissionResult()
                            viewModel.clearMessage()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(offset.y - center.y, offset.x - center.x)
                            lastAngle = Math.toDegrees(angleRad.toDouble()).toFloat()
                            lastDragTime = System.currentTimeMillis()
                            dragVelocity = 0f
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
                                
                                val now = System.currentTimeMillis()
                                val dt = now - lastDragTime
                                if (dt > 0) {
                                    val instantVelocity = angleDelta / dt
                                    dragVelocity = if (abs(dragVelocity) < 0.001f) {
                                        instantVelocity
                                    } else {
                                        dragVelocity * 0.6f + instantVelocity * 0.4f
                                    }
                                }
                                lastDragTime = now
                            }
                        },
                        onDragEnd = { handleDragRelease() },
                        onDragCancel = { handleDragRelease() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Ripple visualizer layer
            if (rippleAnim.value > 0f && rippleAnim.value < 1f) {
                val indicatorColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val startRadius = size.width / 2f
                    val maxRadius = startRadius + 60.dp.toPx()
                    val currentRadius = startRadius + (maxRadius - startRadius) * rippleAnim.value
                    val alpha = 1f - rippleAnim.value
                    drawCircle(
                        color = indicatorColor.copy(alpha = alpha * 0.45f),
                        radius = currentRadius,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            if (spinnerConfig.isCanvasBased) {
                if (currentSpinner == "DEFAULT") {
                    BlueDial(angle = displayedAngle, modifier = Modifier.fillMaxSize())
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
                        val angleRad = Math.toRadians((displayedAngle - 90).toDouble())
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
                        modifier = Modifier.fillMaxSize().rotate(displayedAngle)
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
                        text = String.format("%.1f%%", animatedScore.value),
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
    }

    // FLOATING BOTTOM ACTION DECK
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer { 
                alpha = bottomDeckAlpha
                translationY = (1f - bottomDeckAlpha) * 50f
            },
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { 
                    viewModel.updateAngle(-state.totalRotation) 
                },
                enabled = !isDragging && !isSpinning && state.totalRotation != 0f
            ) {
                Text(
                    text = "RESET",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (state.totalRotation != 0f) 0.8f else 0.4f)
                )
            }

            Button(
                onClick = { viewModel.submit(onWin = onWin, onFail = {}) },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .height(44.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(22.dp),
                enabled = !isDragging && !isSpinning
            ) {
                Text(
                    text = "SUBMIT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    }
}
}
