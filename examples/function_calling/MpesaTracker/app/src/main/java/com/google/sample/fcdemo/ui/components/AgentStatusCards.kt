package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FinanceIQ Agent Card
        AgentCard(
            agentState = financeIQState,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.debugAgentStates() }
        )
        
        // SpendWise Agent Card
        AgentCard(
            agentState = spendWiseState,
            modifier = Modifier.weight(1f),
            onClick = { viewModel.debugAgentStates() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentCard(
    agentState: AgentState,
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
                text = agentState.message.ifBlank { getDefaultMessage(agentState.status) },
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                minLines = 2,
                maxLines = 2
            )
            
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
    val emoji = when (agentType) {
        AgentType.FINANCE_IQ -> "💎"
        AgentType.SPEND_WISE -> "🧠"
    }
    
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
    
    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Agent Avatar Background
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.3f),
                            statusColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Emoji Avatar
            Text(
                text = emoji,
                fontSize = 24.sp
            )
        }
        
        // Status Indicator Dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopEnd)
                .background(statusColor, CircleShape)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White, CircleShape)
            )
        }
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
        AgentType.FINANCE_IQ -> "Transaction\nExtraction"
        AgentType.SPEND_WISE -> "Financial\nCategorization"
    }
}

private fun getDefaultMessage(status: AgentStatus): String {
    return when (status) {
        AgentStatus.IDLE -> "Ready for next task"
        AgentStatus.INITIALIZING -> "Starting up..."
        AgentStatus.ACTIVE -> "Processing data..."
        AgentStatus.THINKING -> "Analyzing..."
        AgentStatus.COMPLETE -> "Task completed!"
        AgentStatus.ERROR -> "Error occurred"
        AgentStatus.HANDOFF -> "Passing data..."
    }
}