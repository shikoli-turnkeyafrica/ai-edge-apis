package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.agents.FunctionCall
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * Function Call Inspector Panel
 * Expandable developer view showing actual function calls, inputs, outputs, and execution details
 */
@androidx.paging.ExperimentalPagingApi
@Composable
fun FunctionCallInspector(
    viewModel: MpesaViewModel,
    modifier: Modifier = Modifier
) {
    val functionCalls by viewModel.functionCalls.collectAsStateWithLifecycle()
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.glass(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Inspector Header (Always Visible)
            InspectorHeader(
                functionCallCount = functionCalls.size,
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded }
            )
            
            // Expandable Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400)),
                exit = shrinkVertically(
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (functionCalls.isNotEmpty()) {
                        // Function Call List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(functionCalls) { functionCall ->
                                FunctionCallCard(
                                    functionCall = functionCall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Summary Statistics
                        InspectorSummary(functionCalls = functionCalls)
                        
                    } else {
                        // Empty State
                        EmptyInspectorState()
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorHeader(
    functionCallCount: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and Count
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔍",
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Function Calls",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            if (functionCallCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF4A90E2).copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "$functionCallCount",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90E2)
                    )
                }
            }
        }
        
        // Expand/Collapse Action
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "Collapse" else "Tap to expand",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FunctionCallCard(
    functionCall: FunctionCall,
    modifier: Modifier = Modifier
) {
    val agentColor = when (functionCall.agentType) {
        AgentType.FINANCE_IQ -> Color(0xFF4A90E2) // Blue
        AgentType.SPEND_WISE -> Color(0xFF9B59B6)  // Purple
    }
    
    val agentEmoji = when (functionCall.agentType) {
        AgentType.FINANCE_IQ -> "💎"
        AgentType.SPEND_WISE -> "🧠"
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = agentColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Function Call Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = agentEmoji,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Text(
                        text = functionCall.functionName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = agentColor
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Success/Error Indicator
                    Icon(
                        imageVector = if (functionCall.success) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = if (functionCall.success) "Success" else "Error",
                        tint = if (functionCall.success) Color.Green else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Execution Time
                    Text(
                        text = "${functionCall.executionTimeMs}ms",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Function Parameters (Inputs)
            if (!functionCall.inputs.isNullOrEmpty()) {
                Text(
                    text = "Inputs:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Column {
                        functionCall.inputs?.entries?.take(3)?.forEach { (key, value) ->
                            Text(
                                text = "$key: ${formatValue(value)}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 12.sp
                            )
                        }
                        
                        if ((functionCall.inputs?.size ?: 0) > 3) {
                            Text(
                                text = "... and ${(functionCall.inputs?.size ?: 0) - 3} more",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
            }
            
            // Function Results (Outputs)
            if (!functionCall.outputs.isNullOrEmpty()) {
                Text(
                    text = "Outputs:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Column {
                        functionCall.outputs?.entries?.take(3)?.forEach { (key, value) ->
                            Text(
                                text = "$key: ${formatValue(value)}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 12.sp
                            )
                        }
                        
                        if ((functionCall.outputs?.size ?: 0) > 3) {
                            Text(
                                text = "... and ${(functionCall.outputs?.size ?: 0) - 3} more",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
            
            // Confidence Badge
            functionCall.confidence?.let { confidence ->
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confidence:",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            confidence >= 0.8f -> Color.Green.copy(alpha = 0.2f)
                            confidence >= 0.5f -> Color(0xFFFFA500).copy(alpha = 0.2f)
                            else -> Color.Red.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = "${(confidence * 100).toInt()}%",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                confidence >= 0.8f -> Color.Green
                                confidence >= 0.5f -> Color(0xFFFFA500)
                                else -> Color.Red
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorSummary(
    functionCalls: List<FunctionCall>,
    modifier: Modifier = Modifier
) {
    val successfulCalls = functionCalls.count { it.success }
    val avgExecutionTime = if (functionCalls.isNotEmpty()) {
        functionCalls.map { it.executionTimeMs }.average().toLong()
    } else 0L
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Success Rate
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (functionCalls.isNotEmpty()) "${((successfulCalls.toFloat() / functionCalls.size) * 100).toInt()}%" else "0%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green
                )
                Text(
                    text = "Success Rate",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Average Time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${avgExecutionTime}ms",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Text(
                    text = "Avg Time",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Total Calls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${functionCalls.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Total Calls",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun EmptyInspectorState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            fontSize = 32.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "No function calls yet",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Function calls will appear here during AI processing",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

private fun formatValue(value: Any): String {
    return when (value) {
        is String -> if (value.length > 30) "\"${value.take(30)}...\"" else "\"$value\""
        is Double -> "%.2f".format(value)
        is Float -> "%.2f".format(value)
        else -> value.toString()
    }
}