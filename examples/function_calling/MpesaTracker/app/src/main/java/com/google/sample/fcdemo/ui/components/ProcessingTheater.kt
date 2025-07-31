package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.agents.AgentStatus
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * Real-Time Processing Theater - Split-screen view showing what each agent sees
 * Left: FinanceIQ's raw SMS input
 * Right: SpendWise's structured data input
 */
@OptIn(androidx.paging.ExperimentalPagingApi::class)
@Composable
fun ProcessingTheater(
    viewModel: MpesaViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val financeIQState by viewModel.financeIQState.collectAsStateWithLifecycle()
    val spendWiseState by viewModel.spendWiseState.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rotationAngle"
    )
    
    // Show theater when agents are processing or recently processed
    val shouldShow = remember(financeIQState.status, spendWiseState.status) {
        financeIQState.status in listOf(
            AgentStatus.ACTIVE, 
            AgentStatus.THINKING, 
            AgentStatus.COMPLETE,
            AgentStatus.HANDOFF
        ) || spendWiseState.status in listOf(
            AgentStatus.ACTIVE, 
            AgentStatus.THINKING, 
            AgentStatus.COMPLETE
        )
    }
    
    if (shouldShow || isExpanded) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .glass()
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with expand/collapse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Processing Theater",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "🎬 Processing Theater",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color.White,
                            modifier = Modifier.rotate(rotationAngle)
                        )
                    }
                }
                
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Live view of what each AI agent sees during processing",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Split-screen view
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // FinanceIQ's View (Raw SMS)
                        AgentView(
                            agentType = AgentType.FINANCE_IQ,
                            agentState = financeIQState,
                            currentSession = currentSession,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // SpendWise's View (Structured Data)
                        AgentView(
                            agentType = AgentType.SPEND_WISE,
                            agentState = spendWiseState,
                            currentSession = currentSession,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentView(
    agentType: AgentType,
    agentState: com.google.sample.fcdemo.agents.AgentState,
    currentSession: com.google.sample.fcdemo.agents.CollaborationSession?,
    modifier: Modifier = Modifier
) {
    val agentName = when (agentType) {
        AgentType.FINANCE_IQ -> "FinanceIQ"
        AgentType.SPEND_WISE -> "SpendWise"
    }
    
    val emoji = when (agentType) {
        AgentType.FINANCE_IQ -> "💎"
        AgentType.SPEND_WISE -> "🧠"
    }
    
    Card(
        modifier = modifier.glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Agent header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = emoji,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = "$agentName's View",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status indicator
                StatusBadge(agentState.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Data view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp)
                    .background(
                        Color.Black.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (agentType) {
                        AgentType.FINANCE_IQ -> {
                            RawSMSView(
                                smsData = currentSession?.rawInput,
                                status = agentState.status
                            )
                        }
                        AgentType.SPEND_WISE -> {
                            StructuredDataView(
                                structuredData = currentSession?.structuredData,
                                status = agentState.status
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RawSMSView(
    smsData: String?,
    status: AgentStatus
) {
    if (smsData != null && smsData.isNotBlank()) {
        Column {
            Text(
                text = "📱 Raw SMS Input:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = smsData,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (status) {
                AgentStatus.ACTIVE -> {
                    Text(
                        text = "🔍 Analyzing SMS structure...",
                        fontSize = 10.sp,
                        color = Color.Yellow,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                AgentStatus.THINKING -> {
                    Text(
                        text = "💭 Extracting transaction details...",
                        fontSize = 10.sp,
                        color = Color.Yellow,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                AgentStatus.COMPLETE -> {
                    Text(
                        text = "✅ Extraction complete!",
                        fontSize = 10.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
                AgentStatus.HANDOFF -> {
                    Text(
                        text = "🔄 Passing data to SpendWise...",
                        fontSize = 10.sp,
                        color = Color.Magenta,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "⏳",
                fontSize = 24.sp
            )
            Text(
                text = "Waiting for SMS data...",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StructuredDataView(
    structuredData: Map<String, Any>?,
    status: AgentStatus
) {
    if (structuredData != null && structuredData.isNotEmpty()) {
        Column {
            Text(
                text = "🗃️ Structured Data:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            structuredData.forEach { (key, value) ->
                Row(
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "$key:",
                        fontSize = 10.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = value.toString(),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            when (status) {
                AgentStatus.ACTIVE -> {
                    Text(
                        text = "🧠 Analyzing spending patterns...",
                        fontSize = 10.sp,
                        color = Color.Yellow,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                AgentStatus.THINKING -> {
                    Text(
                        text = "💭 Determining category...",
                        fontSize = 10.sp,
                        color = Color.Yellow,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                AgentStatus.COMPLETE -> {
                    Text(
                        text = "✅ Categorization complete!",
                        fontSize = 10.sp,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {}
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "⏳",
                fontSize = 24.sp
            )
            Text(
                text = "Waiting for extracted data...",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatusBadge(status: AgentStatus) {
    val (statusText, statusColor) = when (status) {
        AgentStatus.IDLE -> "IDLE" to Color.Gray
        AgentStatus.INITIALIZING -> "INIT" to Color.Blue
        AgentStatus.ACTIVE -> "ACTIVE" to Color.Green
        AgentStatus.THINKING -> "THINK" to Color.Yellow
        AgentStatus.COMPLETE -> "DONE" to Color.Green
        AgentStatus.ERROR -> "ERROR" to Color.Red
        AgentStatus.HANDOFF -> "PASS" to Color.Magenta
    }
    
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(4.dp),
        color = statusColor.copy(alpha = 0.2f)
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}