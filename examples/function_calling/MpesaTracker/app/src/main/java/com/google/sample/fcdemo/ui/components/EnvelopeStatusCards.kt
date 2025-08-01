package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.data.EnvelopeEntity
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * Phase 4: Envelope Dashboard UI
 * Visual overview of all budget envelopes with real-time balance tracking
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.paging.ExperimentalPagingApi::class)
@Composable
fun EnvelopeStatusCards(
    viewModel: MpesaViewModel,
    onOpenBudgetSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Safe envelope collection with error handling
    val envelopes by viewModel.allEnvelopes.collectAsStateWithLifecycle(initialValue = emptyList())
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Total Budget Summary - with null safety
        if (envelopes.isNotEmpty()) {
            val totalBudget = envelopes.sumOf { envelope -> 
                envelope.budgetAmountKes.takeIf { !it.isNaN() && it.isFinite() } ?: 0.0 
            }
            val totalSpent = envelopes.sumOf { envelope -> 
                envelope.spentAmountKes.takeIf { !it.isNaN() && it.isFinite() } ?: 0.0 
            }
            val overallUsage = if (totalBudget > 0) {
                ((totalSpent / totalBudget) * 100).toInt().coerceIn(0, 999)
            } else 0
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .glass()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Budget Usage: $overallUsage% used",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Budget Setup Button
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .clickable { onOpenBudgetSetup() }
                            .glass(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚙️",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "Setup",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // Envelope Cards Grid
        if (envelopes.isEmpty()) {
            // Loading/Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .glass(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📊",
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Setting up your envelopes...",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Processing M-PESA transactions will create budget envelopes automatically",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // Fixed grid layout (no nested scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sortedEnvelopes = envelopes.sortedBy { it.sortOrder }
                val pairs = sortedEnvelopes.chunked(2)
                
                pairs.forEach { rowEnvelopes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowEnvelopes.forEach { envelope ->
                            EnvelopeCard(
                                envelope = envelope,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number of cards
                        if (rowEnvelopes.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual envelope card showing balance, progress, and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvelopeCard(
    envelope: EnvelopeEntity,
    modifier: Modifier = Modifier
) {
    // Color scheme based on envelope status - with safety
    val envelopeColor = try {
        if (envelope.color.isNotBlank() && envelope.color.startsWith("#")) {
            Color(android.graphics.Color.parseColor(envelope.color))
        } else {
            Color(0xFF607D8B) // Default gray
        }
    } catch (e: Exception) {
        Color(0xFF607D8B) // Default gray
    }
    
    // Safe budget usage calculation
    val safeUsagePercentage = envelope.budgetUsagePercentage.takeIf { 
        !it.isNaN() && it.isFinite() 
    } ?: 0.0
    
    val warningColor = when {
        envelope.isOverBudget -> Color(0xFFFF5722) // Red
        envelope.isApproachingLimit -> Color(0xFFFF9800) // Orange
        safeUsagePercentage > 0.75 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFF4CAF50) // Green
    }
    
    // Animated progress with safety bounds
    val animatedProgress by animateFloatAsState(
        targetValue = safeUsagePercentage.toFloat().coerceIn(0f, 2f), // Allow up to 200% for over-budget
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "EnvelopeProgress"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(
            containerColor = envelopeColor.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (envelope.isOverBudget) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Icon + Name
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
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = envelope.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Status indicator
                if (envelope.isOverBudget) {
                    Text(
                        text = "⚠️",
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Balance Information
            Column {
                // Current Balance - with safety
                Text(
                    text = "Balance",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                val safeBalance = envelope.currentBalanceKes.takeIf { 
                    !it.isNaN() && it.isFinite() 
                } ?: 0.0
                Text(
                    text = "KSh ${String.format("%.2f", safeBalance)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Budget Progress - with safety
                val safeBudgetAmount = envelope.budgetAmountKes.takeIf { 
                    !it.isNaN() && it.isFinite() && it > 0 
                } ?: 0.0
                val safeSpentAmount = envelope.spentAmountKes.takeIf { 
                    !it.isNaN() && it.isFinite() 
                } ?: 0.0
                
                if (safeBudgetAmount > 0) {
                    Text(
                        text = "Budget Usage",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { animatedProgress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = warningColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Budget Text - with safe values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "KSh ${String.format("%.0f", safeSpentAmount)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "KSh ${String.format("%.0f", safeBudgetAmount)}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Usage percentage - with safe calculation
                    val safeUsagePercent = (safeUsagePercentage * 100).toInt().coerceIn(0, 999)
                    Text(
                        text = "${safeUsagePercent}% used",
                        fontSize = 12.sp,
                        color = warningColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Special handling for Suspense Account
            if (envelope.envelopeId == "suspense") {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "💰 Available for allocation",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}