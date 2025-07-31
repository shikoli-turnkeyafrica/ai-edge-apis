package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.agents.AgentStatus
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.agents.ProcessingStage
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * EdgeFinance AI Agent Pipeline
 * Shows the beautiful data flow: SMS → FinanceIQ → Data → SpendWise → Category
 */
@androidx.paging.ExperimentalPagingApi
@Composable
fun AgentPipeline(
    viewModel: MpesaViewModel,
    modifier: Modifier = Modifier
) {
    val financeIQState by viewModel.financeIQState.collectAsStateWithLifecycle()
    val spendWiseState by viewModel.spendWiseState.collectAsStateWithLifecycle()
    val timeline by viewModel.processingTimeline.collectAsStateWithLifecycle()
    
    // Determine current processing stage
    val currentStage = timeline.lastOrNull()?.stage ?: ProcessingStage.SMS_RECEIVED
    
    Card(
        modifier = modifier.glass(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Pipeline Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SMS Stage
                PipelineStage(
                    emoji = "📱",
                    label = "SMS",
                    isActive = currentStage in listOf(
                        ProcessingStage.SMS_RECEIVED,
                        ProcessingStage.FINANCE_IQ_ACTIVATED,
                        ProcessingStage.EXTRACTING_DATA
                    ),
                    isCompleted = currentStage.ordinal > ProcessingStage.EXTRACTING_DATA.ordinal
                )
                
                // Connection Line 1
                ConnectionLine(
                    isActive = currentStage.ordinal >= ProcessingStage.FINANCE_IQ_ACTIVATED.ordinal,
                    isCompleted = currentStage.ordinal > ProcessingStage.DATA_EXTRACTED.ordinal
                )
                
                // FinanceIQ Stage
                PipelineStage(
                    emoji = "💎",
                    label = "FinanceIQ",
                    isActive = financeIQState.status in listOf(
                        AgentStatus.ACTIVE, 
                        AgentStatus.THINKING, 
                        AgentStatus.INITIALIZING
                    ),
                    isCompleted = financeIQState.status == AgentStatus.COMPLETE ||
                                 currentStage.ordinal > ProcessingStage.DATA_EXTRACTED.ordinal
                )
                
                // Connection Line 2
                ConnectionLine(
                    isActive = currentStage.ordinal >= ProcessingStage.DATA_EXTRACTED.ordinal,
                    isCompleted = currentStage.ordinal > ProcessingStage.SPEND_WISE_ACTIVATED.ordinal
                )
                
                // Data Stage
                PipelineStage(
                    emoji = "📊",
                    label = "Data",
                    isActive = currentStage in listOf(
                        ProcessingStage.DATA_EXTRACTED,
                        ProcessingStage.SPEND_WISE_ACTIVATED
                    ),
                    isCompleted = currentStage.ordinal > ProcessingStage.SPEND_WISE_ACTIVATED.ordinal
                )
                
                // Connection Line 3
                ConnectionLine(
                    isActive = currentStage.ordinal >= ProcessingStage.CATEGORIZING.ordinal,
                    isCompleted = currentStage.ordinal > ProcessingStage.CATEGORIZED.ordinal
                )
                
                // Category Stage
                PipelineStage(
                    emoji = "🎯",
                    label = "Category",
                    isActive = spendWiseState.status in listOf(
                        AgentStatus.ACTIVE, 
                        AgentStatus.THINKING, 
                        AgentStatus.INITIALIZING
                    ),
                    isCompleted = spendWiseState.status == AgentStatus.COMPLETE ||
                                 currentStage == ProcessingStage.COMPLETE
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Processing Status
            ProcessingStatus(
                currentStage = currentStage,
                financeIQActive = financeIQState.status in listOf(
                    AgentStatus.ACTIVE, AgentStatus.THINKING, AgentStatus.INITIALIZING
                ),
                spendWiseActive = spendWiseState.status in listOf(
                    AgentStatus.ACTIVE, AgentStatus.THINKING, AgentStatus.INITIALIZING
                )
            )
        }
    }
}

@Composable
private fun PipelineStage(
    emoji: String,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    // Color animation based on state
    val stageColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFF50C878) // Green
            isActive -> Color(0xFF4A90E2) // Blue
            else -> Color.Gray.copy(alpha = 0.5f)
        },
        animationSpec = tween(500)
    )
    
    // Pulsing animation for active stages
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Stage Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stageColor.copy(alpha = 0.3f),
                            stageColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = (20 * pulseScale).sp,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Stage Label
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = stageColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConnectionLine(
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    // Line color animation
    val lineColor by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFF50C878) // Green
            isActive -> Color(0xFF4A90E2) // Blue
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        animationSpec = tween(500)
    )
    
    // Flow animation for active lines
    val infiniteTransition = rememberInfiniteTransition()
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive && !isCompleted) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .width(32.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (isActive && !isCompleted) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            lineColor,
                            lineColor.copy(alpha = 0.3f)
                        ),
                        startX = flowOffset * 100f,
                        endX = (flowOffset * 100f) + 50f
                    )
                } else {
                    Brush.horizontalGradient(listOf(lineColor, lineColor))
                }
            )
    )
}

@Composable
private fun ProcessingStatus(
    currentStage: ProcessingStage,
    financeIQActive: Boolean,
    spendWiseActive: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        financeIQActive -> "PROCESSING"
        spendWiseActive -> "CATEGORIZING"
        currentStage == ProcessingStage.COMPLETE -> "COMPLETE"
        else -> "READY"
    }
    
    val statusColor = when {
        financeIQActive || spendWiseActive -> Color(0xFF4A90E2)
        currentStage == ProcessingStage.COMPLETE -> Color(0xFF50C878)
        else -> Color.Gray
    }
    
    // Pulsing animation for processing status
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = if (financeIQActive || spendWiseActive) 1f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Indicator Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor.copy(alpha = pulseAlpha), CircleShape)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Status Text
        Text(
            text = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor.copy(alpha = pulseAlpha),
            letterSpacing = 1.sp
        )
    }
}