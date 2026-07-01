package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gray400
import com.example.ui.theme.Gray500
import com.example.ui.theme.Gray600
import com.example.ui.theme.Gray800
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

    val spinners = com.example.data.SpinnerProvider.spinners

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$points",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = " pts",
                            style = MaterialTheme.typography.labelMedium,
                            color = Gray500,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            for (config in spinners) {
                val isUnlocked = unlockedSpinners.contains(config.id)
                val isSelected = currentSpinner == config.id

                val coroutineScope = rememberCoroutineScope()
                ShopListItem(
                    config = config,
                    isUnlocked = isUnlocked,
                    isSelected = isSelected,
                    points = points,
                    onSelect = {
                        coroutineScope.launch {
                            if (isUnlocked) {
                                viewModel.prefs.setCurrentSpinner(config.id)
                            } else if (points >= config.cost) {
                                viewModel.prefs.unlockSpinner(config.id, config.cost)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShopListItem(
    config: com.example.data.SpinnerConfig,
    isUnlocked: Boolean,
    isSelected: Boolean,
    points: Int,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Gray800, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dial preview
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(1.dp, Gray600, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (config.isCanvasBased) {
                MinimalDial(angle = 45f, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    text = config.name.first().toString(),
                    color = Gray500,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isUnlocked) "owned" else "${config.cost} pts",
                style = MaterialTheme.typography.bodySmall,
                color = if (isUnlocked) Gray500 else Gray400
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Action button
        Surface(
            onClick = onSelect,
            shape = RoundedCornerShape(100),
            color = when {
                isSelected -> Color.White
                isUnlocked -> Color.Transparent
                else -> Color.White
            },
            border = if (isUnlocked && !isSelected) BorderStroke(1.dp, Gray600) else null
        ) {
            Text(
                text = when {
                    isSelected -> "equipped"
                    isUnlocked -> "equip"
                    else -> "buy"
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = when {
                    isSelected -> Color.Black
                    isUnlocked -> Gray400
                    else -> Color.Black
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


