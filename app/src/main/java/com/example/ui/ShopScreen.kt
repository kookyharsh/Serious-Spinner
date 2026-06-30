package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val points by viewModel.prefs.points.collectAsState(initial = 0)
    val unlockedSpinners by viewModel.prefs.unlockedSpinners.collectAsState(initial = setOf("DEFAULT"))
    val currentSpinner by viewModel.prefs.currentSpinner.collectAsState(initial = "DEFAULT")
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "$points PTS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            com.example.data.SpinnerProvider.spinners.forEach { config ->
                ShopItem(
                    id = config.id,
                    name = config.name,
                    cost = config.cost,
                    isUnlocked = unlockedSpinners.contains(config.id),
                    isSelected = currentSpinner == config.id,
                    onSelect = { coroutineScope.launch { viewModel.prefs.setCurrentSpinner(config.id) } },
                    onUnlock = { coroutineScope.launch { viewModel.prefs.unlockSpinner(config.id, config.cost) } }
                ) { modifier ->
                    if (config.isCanvasBased) {
                        if (config.id == "DEFAULT") {
                            BlueDial(angle = 45f, modifier = modifier)
                        } else {
                            MinimalistDialPreview(modifier = modifier)
                        }
                    } else {
                        Box(modifier = modifier) {
                            config.shellResId?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Shell",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            config.stickResId?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Stick",
                                    modifier = Modifier.fillMaxSize().rotate(45f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItem(
    id: String,
    name: String,
    cost: Int,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUnlock: () -> Unit,
    preview: @Composable (Modifier) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked) { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp)) {
                    preview(Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isUnlocked) {
                        Text(
                            text = "Cost: $cost pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (isSelected) {
                Text(
                    text = "EQUIPPED",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } else if (isUnlocked) {
                Button(onClick = onSelect) {
                    Text("EQUIP")
                }
            } else {
                Button(onClick = onUnlock) {
                    Text("UNLOCK")
                }
            }
        }
    }
}

@Composable
fun MinimalistDialPreview(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.width / 2 - 4.dp.toPx()
        val indicatorColor = Color(0xFF00E676)
        
        drawCircle(
            color = Color.DarkGray,
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
        
        val angleRad = Math.toRadians((45 - 90).toDouble())
        val startX = center.x + (radius - 16.dp.toPx()) * Math.cos(angleRad).toFloat()
        val startY = center.y + (radius - 16.dp.toPx()) * Math.sin(angleRad).toFloat()
        val endX = center.x + (radius + 8.dp.toPx()) * Math.cos(angleRad).toFloat()
        val endY = center.y + (radius + 8.dp.toPx()) * Math.sin(angleRad).toFloat()
        
        drawLine(
            color = indicatorColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
