package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Difficulty
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600
import com.example.ui.theme.Gray700
import com.example.ui.theme.Gray800
import com.example.ui.theme.Gray900
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWin: (Int) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val currentDifficulty by viewModel.currentDifficulty.collectAsState()
    val currentSpinner by viewModel.prefs.currentSpinner.collectAsState(initial = "DEFAULT")
    val points by viewModel.prefs.points.collectAsState(initial = 0)

    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var isSpinning by remember { mutableStateOf(false) }

    var lastDragTime by remember { mutableStateOf(0L) }
    var dragVelocity by remember { mutableStateOf(0f) }
    var inertiaJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val wobbleAnim = remember { Animatable(0f) }

    val message by viewModel.message.collectAsState()
    val submissionResult by viewModel.submissionResult.collectAsState()

    LaunchedEffect(message) {
        if (message != null) {
            delay(2000)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(submissionResult) {
        if (submissionResult != null) {
            delay(4000)
            viewModel.clearSubmissionResult()
        }
    }

    if (gameState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val state = gameState!!

    val hasSpunMoreThanOnce = abs(state.totalRotation) >= 360f

    val bottomDeckAlpha by animateFloatAsState(
        targetValue = if (isDragging || isSpinning || submissionResult != null) 0f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "BottomDeckAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SERIOUS SPINNER",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clickable { onShopClick() }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$points",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gray400
                    )
                    Text(
                        text = " pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray600,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Gray400
                    )
                }
            }
        }

        HorizontalDivider(color = Gray800, thickness = 1.dp)

        Spacer(modifier = Modifier.height(32.dp))

        // Target
        Text(
            text = "target",
            style = MaterialTheme.typography.labelLarge,
            color = Gray600,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${state.targetValue}",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Spinner Dial
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
                        delay(16)
                    }
                    isSpinning = false
                    viewModel.saveCurrentStateToDb()
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
                    viewModel.saveCurrentStateToDb()
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
                .size(220.dp)
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
                        onDrag = { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val pos = change.position
                            val dx = pos.x - center.x
                            val dy = pos.y - center.y
                            val distance = sqrt(dx * dx + dy * dy)
                            if (distance > 0) {
                                val currentAngleRad = atan2(dy, dx)
                                val currentAngleDeg = Math.toDegrees(currentAngleRad.toDouble()).toFloat()
                                val prevAngle = lastAngle ?: currentAngleDeg
                                var angleDelta = currentAngleDeg - prevAngle
                                while (angleDelta > 180f) angleDelta -= 360f
                                while (angleDelta < -180f) angleDelta += 360f
                                viewModel.updateAngle(angleDelta)
                                lastAngle = currentAngleDeg
                                val now = System.currentTimeMillis()
                                val dt = now - lastDragTime
                                if (dt > 0) {
                                    dragVelocity = (dragVelocity * 0.6f) + ((angleDelta / dt) * 0.4f)
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
            if (spinnerConfig.isCanvasBased) {
                if (currentSpinner == "DEFAULT") {
                    BlueDial(angle = displayedAngle, modifier = Modifier.fillMaxSize())
                } else {
                    MinimalDial(angle = displayedAngle, modifier = Modifier.fillMaxSize())
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

        Spacer(modifier = Modifier.height(24.dp))

        // Attempts
        Text(
            text = "attempts  ${state.attempts}",
            style = MaterialTheme.typography.labelLarge,
            color = Gray600,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Streak and Level
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "streak",
                style = MaterialTheme.typography.labelLarge,
                color = Gray600,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${state.streak}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                text = "level",
                style = MaterialTheme.typography.labelLarge,
                color = Gray600,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${state.level}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Precision Score
        AnimatedVisibility(
            visible = submissionResult != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            submissionResult?.let { result ->
                val currentSpins = result.preciseSpins
                val targetValue = result.targetValue
                val diffSpins = abs(currentSpins - targetValue)
                val precisionScore = maxOf(0f, (1f - diffSpins) * 100f)

                val scoreText = when {
                    precisionScore >= 99f -> "flawless"
                    precisionScore >= 95f -> "excellent"
                    precisionScore >= 90f -> "very close"
                    precisionScore >= 75f -> "on the right track"
                    else -> "keep adjusting"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Gray700, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gray900)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "precision score",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray500,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = String.format("%.1f%%", precisionScore),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = scoreText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray400
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "you spun ${String.format("%.2f", currentSpins)} / ${String.format("%.2f", targetValue)} spins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )

                    val diffFormatted = if (currentSpins < targetValue) {
                        String.format("-%.2f spins needed", diffSpins)
                    } else {
                        String.format("+%.2f spins over", diffSpins)
                    }

                    Text(
                        text = if (diffSpins == 0f) "perfect match" else diffFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (diffSpins == 0f) Gray400 else Gray600
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Win/Loss Message
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (msg.startsWith("Perfect")) Color.White else Gray500,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Difficulty Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.values().forEach { diff ->
                val displayName = when (diff) {
                    Difficulty.EASY -> "easy"
                    Difficulty.NORMAL -> "medium"
                    Difficulty.HARD -> "hard"
                    Difficulty.EXTREME -> "extreme"
                }
                val isSelected = currentDifficulty == diff
                Surface(
                    onClick = { viewModel.setDifficulty(diff) },
                    modifier = Modifier.weight(1f),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(100)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.White else Gray600,
                                shape = RoundedCornerShape(100)
                            )
                            .background(
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(100)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName,
                            color = if (isSelected) Color.Black else Gray400,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = { viewModel.submit(onWin = onWin, onFail = {}) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer { alpha = bottomDeckAlpha },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(100),
            enabled = hasSpunMoreThanOnce && !isDragging && !isSpinning
        ) {
            Text(
                text = "submit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
