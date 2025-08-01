package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.agents.AgentState
import com.google.sample.fcdemo.agents.AgentStatus
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * EdgeFinance AI Agents Status Cards
 * Showcases the two AI agents working together in real-time
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.paging.ExperimentalPagingApi::class)
@Composable
fun AgentStatusCards(
    viewModel: MpesaViewModel,
    modifier: Modifier = Modifier
) {
    val financeIQState by viewModel.financeIQState.collectAsStateWithLifecycle()
    val spendWiseState by viewModel.spendWiseState.collectAsStateWithLifecycle()
    val suspenseBalance by viewModel.suspenseBalance.collectAsStateWithLifecycle()
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FinanceIQ Agent Card (with Suspense Account balance)
        AgentCard(
            agentState = financeIQState,
            suspenseBalance = suspenseBalance,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.debugAgentStates() }
        )
        
        // SpendWise Agent Card
        AgentCard(
            agentState = spendWiseState,
            suspenseBalance = null, // SpendWise doesn't show suspense balance
            modifier = Modifier.weight(1f),
            onClick = { viewModel.debugAgentStates() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentCard(
    agentState: AgentState,
    suspenseBalance: Double? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Color animation based on agent status
    val cardColor by animateColorAsState(
        targetValue = when (agentState.status) {
            AgentStatus.IDLE -> Color(0xFF2A2A2A).copy(alpha = 0.3f)
            AgentStatus.INITIALIZING -> Color(0xFF4A90E2).copy(alpha = 0.4f)
            AgentStatus.ACTIVE -> Color(0xFF50E3C2).copy(alpha = 0.4f)
            AgentStatus.THINKING -> Color(0xFFFFD700).copy(alpha = 0.4f)
            AgentStatus.COMPLETE -> Color(0xFF50C878).copy(alpha = 0.4f)
            AgentStatus.ERROR -> Color(0xFFFF6B6B).copy(alpha = 0.4f)
            AgentStatus.HANDOFF -> Color(0xFF9B59B6).copy(alpha = 0.4f)
        },
        animationSpec = tween(500)
    )
    
    // Pulsing animation for active agents
    val scale by animateFloatAsState(
        targetValue = if (agentState.status in listOf(AgentStatus.ACTIVE, AgentStatus.THINKING)) 1.02f else 1f,
        animationSpec = tween(1000)
    )
    
    Card(
        modifier = modifier
            .scale(scale)
            .glass(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (agentState.status == AgentStatus.ACTIVE) 8.dp else 4.dp
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Agent Avatar and Status Indicator
            AgentAvatar(
                agentType = agentState.type,
                status = agentState.status
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Agent Name
            Text(
                text = getAgentDisplayName(agentState.type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Agent Specialty
            Text(
                text = getAgentSpecialty(agentState.type),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status Message
            Text(
                text = agentState.message.ifBlank { getDefaultMessage(agentState.status, agentState.type) },
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                minLines = 2,
                maxLines = 2
            )
            
            // Suspense Account Balance (only for FinanceIQ)
            suspenseBalance?.let { balance ->
                Spacer(modifier = Modifier.height(4.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF607D8B).copy(alpha = 0.3f) // Suspense color
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚖️",
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Suspense",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "KSh ${String.format("%.2f", balance)}",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar (if applicable)
            if (agentState.progress > 0f && agentState.status in listOf(
                AgentStatus.ACTIVE, AgentStatus.THINKING, AgentStatus.INITIALIZING
            )) {
                LinearProgressIndicator(
                    progress = agentState.progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Confidence Badge (if available)
            agentState.confidence?.let { confidence ->
                ConfidenceBadge(confidence)
            }
            
            // Processing Time (if available)
            if (agentState.processingTimeMs > 0) {
                Text(
                    text = "${agentState.processingTimeMs}ms",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AgentAvatar(
    agentType: AgentType,
    status: AgentStatus,
    modifier: Modifier = Modifier
) {
    // Enhanced avatar system with sophisticated animations
    val infiniteTransition = rememberInfiniteTransition(label = "AgentAvatar")
    
    // Status indicator color
    val statusColor = when (status) {
        AgentStatus.IDLE -> Color.Gray
        AgentStatus.INITIALIZING -> Color.Blue
        AgentStatus.ACTIVE -> Color.Green
        AgentStatus.THINKING -> Color.Yellow
        AgentStatus.COMPLETE -> Color.Green
        AgentStatus.ERROR -> Color.Red
        AgentStatus.HANDOFF -> Color.Magenta
    }
    
    // Rotation animation for active states
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (status in listOf(AgentStatus.ACTIVE, AgentStatus.THINKING)) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == AgentStatus.THINKING) 3000 else 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == AgentStatus.ACTIVE) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Breathing effect for thinking state
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = if (status == AgentStatus.THINKING) 1.05f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Enhanced Avatar Background with animations
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    rotationZ = if (agentType == AgentType.FINANCE_IQ && status == AgentStatus.ACTIVE) rotation else 0f
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.4f),
                            statusColor.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Agent-specific animated elements
            when (agentType) {
                AgentType.FINANCE_IQ -> {
                    FinanceIQAvatar(
                        status = status,
                        statusColor = statusColor,
                        rotation = rotation,
                        scale = pulseScale
                    )
                }
                AgentType.SPEND_WISE -> {
                    SpendWiseAvatar(
                        status = status,
                        statusColor = statusColor,
                        breathingScale = breathingScale
                    )
                }
            }
        }
        
        // Enhanced Status Indicator with pulsing
        Box(
            modifier = Modifier
                .size(14.dp)
                .align(Alignment.TopEnd)
                .graphicsLayer {
                    scaleX = if (status == AgentStatus.ACTIVE) pulseScale else 1f
                    scaleY = if (status == AgentStatus.ACTIVE) pulseScale else 1f
                }
                .background(statusColor, CircleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, CircleShape)
            )
        }
        
        // Agent-specific animated indicators
        when (agentType) {
            AgentType.FINANCE_IQ -> {
                if (status in listOf(AgentStatus.ACTIVE, AgentStatus.THINKING)) {
                    // Magnifying glass indicator
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Analyzing",
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.BottomStart)
                            .graphicsLayer {
                                rotationZ = rotation * 0.5f
                                alpha = 0.8f
                            },
                        tint = Color.White
                    )
                }
            }
            AgentType.SPEND_WISE -> {
                if (status == AgentStatus.THINKING) {
                    // Thinking bubbles
                    ThinkingBubbles(
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceIQAvatar(
    status: AgentStatus,
    statusColor: Color,
    rotation: Float,
    scale: Float
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Diamond emoji with enhanced effects
        Text(
            text = "💎",
            fontSize = 28.sp,
            modifier = Modifier.graphicsLayer {
                rotationZ = if (status == AgentStatus.ACTIVE) rotation * 0.3f else 0f
            }
        )
        
        // Extraction rays effect for active state
        if (status == AgentStatus.ACTIVE) {
            repeat(8) { index ->
                Box(
                    modifier = Modifier
                        .size(2.dp, 16.dp)
                        .graphicsLayer {
                            rotationZ = (index * 45f) + (rotation * 0.2f)
                            alpha = 0.6f
                        }
                        .background(
                            statusColor.copy(alpha = 0.4f),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun SpendWiseAvatar(
    status: AgentStatus,
    statusColor: Color,
    breathingScale: Float
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Brain emoji with breathing effect
        Text(
            text = "🧠",
            fontSize = 28.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = if (status == AgentStatus.THINKING) breathingScale else 1f
                scaleY = if (status == AgentStatus.THINKING) breathingScale else 1f
            }
        )
        
        // Neural network effect for thinking state
        if (status == AgentStatus.THINKING) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(1.dp, 8.dp)
                        .graphicsLayer {
                            rotationZ = index * 60f
                            alpha = 0.3f + (breathingScale - 0.9f) * 2f
                        }
                        .background(
                            statusColor.copy(alpha = 0.3f),
                            RoundedCornerShape(0.5.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun ThinkingBubbles(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingBubbles")
    
    val bubble1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble1Alpha"
    )
    
    val bubble2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble2Alpha"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Small thinking bubble
        Box(
            modifier = Modifier
                .size(3.dp)
                .background(
                    Color.White.copy(alpha = bubble1Alpha),
                    CircleShape
                )
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // Medium thinking bubble
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    Color.White.copy(alpha = bubble2Alpha),
                    CircleShape
                )
        )
    }
}

@Composable
private fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val confidenceText = when {
        confidence >= 0.8f -> "HIGH"
        confidence >= 0.5f -> "MED"
        else -> "LOW"
    }
    
    val confidenceColor = when {
        confidence >= 0.8f -> Color.Green
        confidence >= 0.5f -> Color(0xFFFFA500) // Orange
        else -> Color.Red
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = confidenceColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = confidenceText,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = confidenceColor
        )
    }
}

private fun getAgentDisplayName(agentType: AgentType): String {
    return when (agentType) {
        AgentType.FINANCE_IQ -> "FinanceIQ"
        AgentType.SPEND_WISE -> "SpendWise"
    }
}

private fun getAgentSpecialty(agentType: AgentType): String {
    return when (agentType) {
        AgentType.FINANCE_IQ -> "Money-Sense\nAI Agent"
        AgentType.SPEND_WISE -> "Spending Coach\nAI Agent"
    }
}

private fun getDefaultMessage(status: AgentStatus, agentType: AgentType): String {
    return when (status) {
        AgentStatus.IDLE -> getDynamicIdleMessage(agentType)
        AgentStatus.INITIALIZING -> "Starting up..."
        AgentStatus.ACTIVE -> "Processing data..."
        AgentStatus.THINKING -> "Analyzing..."
        AgentStatus.COMPLETE -> "Task completed!"
        AgentStatus.ERROR -> "Error occurred"
        AgentStatus.HANDOFF -> "Passing data..."
    }
}

/**
 * 🎭 Dynamic rotating flavor text for idle agents
 * Cycles through engaging messages to show agent personality
 */
private fun getDynamicIdleMessage(agentType: AgentType): String {
    val currentTime = System.currentTimeMillis()
    val cycleIndex = ((currentTime / 3000) % 3).toInt() // Rotate every 3 seconds
    
    return when (agentType) {
        AgentType.FINANCE_IQ -> {
            val messages = arrayOf(
                "Ready to route income to Suspense Account",
                "Standing by to extract M-PESA data", 
                "Primed to channel cash-flow through Suspense"
            )
            messages[cycleIndex]
        }
        AgentType.SPEND_WISE -> {
            val messages = arrayOf(
                "Ready to allocate to your budget envelopes",
                "Standing by to track envelope spending",
                "Poised to monitor your budget limits"
            )
            messages[cycleIndex]
        }
    }
}