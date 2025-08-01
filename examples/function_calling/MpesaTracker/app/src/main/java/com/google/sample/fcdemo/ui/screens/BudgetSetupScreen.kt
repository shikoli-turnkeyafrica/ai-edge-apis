package com.google.sample.fcdemo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.sample.fcdemo.data.EnvelopeEntity
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel
import kotlin.math.roundToInt

/**
 * Phase 4 Part 3: Budget Setup Screen
 * Interactive interface for customizing envelope budgets with sliders, presets, and AI recommendations
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.paging.ExperimentalPagingApi::class)
@Composable
fun BudgetSetupScreen(
    viewModel: MpesaViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val envelopes by viewModel.allEnvelopes.collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Budget editing state
    var editedBudgets by remember { mutableStateOf(mapOf<String, Double>()) }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var showRecommendations by remember { mutableStateOf(false) }
    
    // Add envelope dialog state
    var showAddEnvelopeDialog by remember { mutableStateOf(false) }
    
    // Quick allocation dialog state
    var showQuickAllocationDialog by remember { mutableStateOf(false) }
    
    // Initialize edited budgets from current envelope data
    LaunchedEffect(envelopes) {
        if (editedBudgets.isEmpty() && envelopes.isNotEmpty()) {
            editedBudgets = envelopes.associate { envelope ->
                envelope.envelopeId to envelope.budgetAmountKes
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3A8A),
                        Color(0xFF1E40AF),
                        Color(0xFF2563EB)
                    )
                )
            )
    ) {
        // Top App Bar
        BudgetSetupTopBar(
            onNavigateBack = onNavigateBack,
            onSave = {
                // TODO: Save budget changes
                // viewModel.updateEnvelopeBudgets(editedBudgets, selectedPeriod)
                onNavigateBack()
            },
            onResetToRecommended = {
                showRecommendations = !showRecommendations
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Budget Period Selector
            item {
                BudgetPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
            
            // AI Recommendations Card
            if (showRecommendations) {
                item {
                    BudgetRecommendationsCard(
                        envelopes = envelopes,
                        onApplyRecommendation = { envelopeId, amount ->
                            editedBudgets = editedBudgets + (envelopeId to amount)
                        }
                    )
                }
            }
            
            // Budget Templates
            item {
                BudgetTemplatesCard(
                    onTemplateSelected = { template ->
                        editedBudgets = applyBudgetTemplate(envelopes, template)
                    }
                )
            }
            
            // Quick Actions Section (similar to Budget Templates)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚡ Quick Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Add New Envelope Action
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                                    .clickable { showAddEnvelopeDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Green.copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "➕",
                                        fontSize = 28.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Add Envelope",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            
                            // Transfer Money Action
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 6.dp)
                                    .clickable { showQuickAllocationDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.8f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "💸",
                                        fontSize = 28.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Transfer Money",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Customize Your Budgets Header
            item {
                Text(
                    text = "📊 Customize Your Budgets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            items(
                items = envelopes.filter { it.envelopeId != "suspense" }, // Exclude suspense account
                key = { it.envelopeId }
            ) { envelope ->
                BudgetEditorCard(
                    envelope = envelope,
                    currentBudget = editedBudgets[envelope.envelopeId] ?: envelope.budgetAmountKes,
                    period = selectedPeriod,
                    onBudgetChanged = { newAmount ->
                        editedBudgets = editedBudgets + (envelope.envelopeId to newAmount)
                    }
                )
            }
            
            // Summary Card
            item {
                BudgetSummaryCard(
                    totalBudget = editedBudgets.values.sum(),
                    period = selectedPeriod,
                    envelopeCount = editedBudgets.size
                )
            }
            
            // Spacer for bottom padding
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Add New Envelope Dialog
    if (showAddEnvelopeDialog) {
        AddEnvelopeDialog(
            onDismiss = { showAddEnvelopeDialog = false },
            onCreateEnvelope = { name, icon, budget ->
                viewModel.createNewEnvelope(name, icon, budget)
                showAddEnvelopeDialog = false
            }
        )
    }
    
    // Quick Allocation Dialog
    if (showQuickAllocationDialog) {
        QuickAllocationDialog(
            envelopes = envelopes,
            viewModel = viewModel,
            onDismiss = { showQuickAllocationDialog = false }
        )
    }
}

/**
 * Top app bar for budget setup screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSetupTopBar(
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onResetToRecommended: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Title
            Text(
                text = "⚙️ Budget Setup",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // Action buttons
            Row {
                IconButton(onClick = onResetToRecommended) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "AI Recommendations",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.Green
                    )
                }
            }
        }
    }
}

/**
 * Budget period selector (Monthly, Weekly, Daily)
 */
@Composable
private fun BudgetPeriodSelector(
    selectedPeriod: BudgetPeriod,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📅 Budget Period",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetPeriod.values().forEach { period ->
                    PeriodToggleButton(
                        period = period,
                        isSelected = selectedPeriod == period,
                        onSelected = { onPeriodSelected(period) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual period toggle button
 */
@Composable
private fun PeriodToggleButton(
    period: BudgetPeriod,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
        animationSpec = tween(300),
        label = "PeriodButtonBackground"
    )
    
    Card(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = period.displayName,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
        )
    }
}

/**
 * AI budget recommendations card
 */
@Composable
private fun BudgetRecommendationsCard(
    envelopes: List<EnvelopeEntity>,
    onApplyRecommendation: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🤖 AI Budget Recommendations",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "Based on your spending patterns, here are optimized budget suggestions:",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Sample recommendations - in real implementation, this would be AI-generated
            val recommendations = listOf(
                "Housing: Increase by 15% due to recent overspending",
                "Food: Optimal at current level",
                "Transport: Reduce by 20% - you're under budget",
                "Entertainment: Increase by 10% for balanced lifestyle"
            )
            
            recommendations.forEach { recommendation ->
                Text(
                    text = "• $recommendation",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Budget templates card
 */
@Composable
private fun BudgetTemplatesCard(
    onTemplateSelected: (BudgetTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📋 Quick Budget Templates",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetTemplate.values().forEach { template ->
                    TemplateButton(
                        template = template,
                        onSelected = { onTemplateSelected(template) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Individual template button
 */
@Composable
private fun TemplateButton(
    template: BudgetTemplate,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clickable { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = template.icon,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = template.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Individual envelope budget editor
 */
@Composable
private fun BudgetEditorCard(
    envelope: EnvelopeEntity,
    currentBudget: Double,
    period: BudgetPeriod,
    onBudgetChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Envelope header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = envelope.icon,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = envelope.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "KSh ${String.format("%.0f", currentBudget)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Budget slider
            var sliderValue by remember(currentBudget) { 
                mutableFloatStateOf(currentBudget.toFloat()) 
            }
            
            Slider(
                value = sliderValue,
                onValueChange = { 
                    sliderValue = it
                    onBudgetChanged(it.roundToInt().toDouble())
                },
                valueRange = 0f..10000f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.8f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            
            // Current spending vs budget
            val currentSpending = envelope.spentAmountKes
            val usagePercentage = if (currentBudget > 0) {
                (currentSpending / currentBudget * 100).toInt()
            } else 0
            
            Text(
                text = "Current spending: KSh ${String.format("%.0f", currentSpending)} ($usagePercentage% of budget)",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Budget summary card
 */
@Composable
private fun BudgetSummaryCard(
    totalBudget: Double,
    period: BudgetPeriod,
    envelopeCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💰 Total ${period.displayName} Budget",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "KSh ${String.format("%.0f", totalBudget)}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Green,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "$envelopeCount active budget categories",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Budget period enumeration
 */
enum class BudgetPeriod(val displayName: String, val multiplier: Double) {
    DAILY("Daily", 1.0),
    WEEKLY("Weekly", 7.0),
    MONTHLY("Monthly", 30.0)
}

/**
 * Budget template enumeration
 */
enum class BudgetTemplate(val displayName: String, val icon: String) {
    CONSERVATIVE("Conservative", "🛡️"),
    BALANCED("Balanced", "⚖️"),
    AGGRESSIVE("Aggressive", "🚀")
}

/**
 * Apply budget template to envelopes
 */
private fun applyBudgetTemplate(
    envelopes: List<EnvelopeEntity>,
    template: BudgetTemplate
): Map<String, Double> {
    return envelopes.filter { it.envelopeId != "suspense" }.associate { envelope ->
        val baseAmount = envelope.budgetAmountKes
        val adjustedAmount = when (template) {
            BudgetTemplate.CONSERVATIVE -> baseAmount * 0.8  // 20% reduction
            BudgetTemplate.BALANCED -> baseAmount  // No change
            BudgetTemplate.AGGRESSIVE -> baseAmount * 1.3  // 30% increase
        }
        envelope.envelopeId to adjustedAmount
    }
}

/**
 * Add New Envelope Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEnvelopeDialog(
    onDismiss: () -> Unit,
    onCreateEnvelope: (name: String, icon: String, budget: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var envelopeName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💰") }
    var budgetAmount by remember { mutableStateOf("1000") }
    var selectedColor by remember { mutableStateOf("#4CAF50") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "➕ Create New Envelope",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Envelope Name Input
                OutlinedTextField(
                    value = envelopeName,
                    onValueChange = { envelopeName = it },
                    label = { Text("Envelope Name") },
                    placeholder = { Text("e.g., Travel Fund") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Icon Picker
                Text(
                    text = "Choose Icon:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                IconPicker(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )
                
                // Budget Amount Input
                OutlinedTextField(
                    value = budgetAmount,
                    onValueChange = { budgetAmount = it },
                    label = { Text("Monthly Budget (KSh)") },
                    placeholder = { Text("1000") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("KSh ") }
                )
                
                // Color Picker (Simple version)
                Text(
                    text = "Envelope Color:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val budget = budgetAmount.toDoubleOrNull() ?: 0.0
                    if (envelopeName.isNotBlank() && budget > 0) {
                        onCreateEnvelope(envelopeName.trim(), selectedIcon, budget)
                    }
                },
                enabled = envelopeName.isNotBlank() && budgetAmount.toDoubleOrNull() != null
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Icon Picker Grid
 */
@Composable
private fun IconPicker(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableIcons = listOf(
        "💰", "🎯", "✈️", "🏥", "🎓", "📚", "🏃", "🎮", "🎵", "🛒",
        "🚗", "⛽", "🏠", "💡", "📱", "💻", "👕", "👟", "🍔", "☕",
        "🎪", "🎨", "🌟", "💎", "🔧", "📊", "🎁", "🌱", "💊", "🏆"
    )
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableIcons) { icon ->
            Card(
                modifier = Modifier
                    .size(45.dp)
                    .clickable { onIconSelected(icon) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedIcon == icon) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else 
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = icon,
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * Simple Color Picker
 */
@Composable
private fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availableColors = listOf(
        "#4CAF50", "#2196F3", "#FF9800", "#E91E63", "#9C27B0",
        "#F44336", "#00BCD4", "#8BC34A", "#FFC107", "#795548"
    )
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableColors) { color ->
            val isSelected = selectedColor == color
            Card(
                modifier = Modifier
                    .size(35.dp)
                    .clickable { onColorSelected(color) },
                colors = CardDefaults.cardColors(
                    containerColor = Color(android.graphics.Color.parseColor(color))
                ),
                shape = RoundedCornerShape(6.dp),
                border = if (isSelected) BorderStroke(2.dp, Color.Black) else null
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick Allocation Dialog for transferring money between envelopes
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.paging.ExperimentalPagingApi::class)
@Composable
private fun QuickAllocationDialog(
    envelopes: List<EnvelopeEntity>,
    viewModel: MpesaViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fromEnvelopeId by remember { mutableStateOf("") }
    var toEnvelopeId by remember { mutableStateOf("") }
    var transferAmount by remember { mutableStateOf("") }
    var transferResult by remember { mutableStateOf<String?>(null) }
    var isTransferring by remember { mutableStateOf(false) }
    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Filter available envelopes (exclude suspense and ensure they have balance)
    val availableEnvelopes = envelopes.filter { 
        it.envelopeId != "suspense" && it.currentBalanceKes > 0 
    }
    val allTargetEnvelopes = envelopes.filter { it.envelopeId != "suspense" }
    
    val fromEnvelope = envelopes.find { it.envelopeId == fromEnvelopeId }
    val toEnvelope = envelopes.find { it.envelopeId == toEnvelopeId }
    val amount = transferAmount.toDoubleOrNull() ?: 0.0
    
    // Validation
    val canTransfer = fromEnvelopeId.isNotEmpty() && 
                     toEnvelopeId.isNotEmpty() && 
                     fromEnvelopeId != toEnvelopeId &&
                     amount > 0 && 
                     (fromEnvelope?.currentBalanceKes ?: 0.0) >= amount &&
                     !isTransferring
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = "💸 Transfer Money",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // From Envelope Selector
                Text(
                    text = "From Envelope:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                ExposedDropdownMenuBox(
                    expanded = showFromDropdown,
                    onExpandedChange = { showFromDropdown = !showFromDropdown }
                ) {
                    OutlinedTextField(
                        value = fromEnvelope?.displayName ?: "Select source envelope",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showFromDropdown,
                        onDismissRequest = { showFromDropdown = false }
                    ) {
                        availableEnvelopes.forEach { envelope ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${envelope.icon} ${envelope.displayName}",
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Available: KSh ${String.format("%.0f", envelope.currentBalanceKes)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    fromEnvelopeId = envelope.envelopeId
                                    showFromDropdown = false
                                    transferResult = null // Clear previous results
                                }
                            )
                        }
                    }
                }
                
                // Show available balance for selected envelope
                if (fromEnvelope != null) {
                    Text(
                        text = "Available: KSh ${String.format("%.0f", fromEnvelope.currentBalanceKes)}",
                        fontSize = 12.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // To Envelope Selector
                Text(
                    text = "To Envelope:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                ExposedDropdownMenuBox(
                    expanded = showToDropdown,
                    onExpandedChange = { showToDropdown = !showToDropdown }
                ) {
                    OutlinedTextField(
                        value = toEnvelope?.displayName ?: "Select destination envelope",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showToDropdown,
                        onDismissRequest = { showToDropdown = false }
                    ) {
                        allTargetEnvelopes.filter { it.envelopeId != fromEnvelopeId }.forEach { envelope ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${envelope.icon} ${envelope.displayName}",
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    toEnvelopeId = envelope.envelopeId
                                    showToDropdown = false
                                    transferResult = null // Clear previous results
                                }
                            )
                        }
                    }
                }
                
                // Transfer Amount Input
                OutlinedTextField(
                    value = transferAmount,
                    onValueChange = { 
                        transferAmount = it
                        transferResult = null // Clear previous results
                    },
                    label = { Text("Transfer Amount") },
                    placeholder = { Text("0") },
                    prefix = { Text("KSh ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = amount > 0 && (fromEnvelope?.currentBalanceKes ?: 0.0) < amount
                )
                
                // Validation Messages
                if (amount > 0 && fromEnvelope != null && fromEnvelope.currentBalanceKes < amount) {
                    Text(
                        text = "⚠️ Insufficient funds. Available: KSh ${String.format("%.0f", fromEnvelope.currentBalanceKes)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                if (fromEnvelopeId == toEnvelopeId && fromEnvelopeId.isNotEmpty()) {
                    Text(
                        text = "⚠️ Cannot transfer to the same envelope",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // Transfer Result Message
                transferResult?.let { message ->
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = if (message.contains("success", ignoreCase = true)) {
                            Color.Green
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isTransferring = true
                    transferResult = null
                    
                    // Perform transfer using coroutine
                    coroutineScope.launch {
                        try {
                            val result = viewModel.transferBetweenEnvelopes(
                                fromEnvelopeId = fromEnvelopeId,
                                toEnvelopeId = toEnvelopeId,
                                amount = amount
                            )
                            
                            transferResult = if (result.success) {
                                "✅ ${result.message}"
                            } else {
                                "❌ ${result.message}"
                            }
                            
                            // If successful, clear form after short delay
                            if (result.success) {
                                delay(1500)
                                onDismiss()
                            }
                            
                        } catch (e: Exception) {
                            transferResult = "❌ Transfer failed: ${e.message}"
                        } finally {
                            isTransferring = false
                        }
                    }
                },
                enabled = canTransfer
            ) {
                if (isTransferring) {
                    Text("Transferring...")
                } else {
                    Text("Transfer KSh ${String.format("%.0f", amount)}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}