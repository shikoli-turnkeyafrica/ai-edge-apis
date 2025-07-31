package com.google.sample.fcdemo.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
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
import kotlinx.coroutines.delay
import com.google.sample.fcdemo.agents.AgentChatMessage
import com.google.sample.fcdemo.agents.AgentType
import com.google.sample.fcdemo.ui.theme.glass
import com.google.sample.fcdemo.viewmodel.MpesaViewModel

/**
 * EdgeFinance AI Agent Chat System
 * Shows real-time messages as agents collaborate and process transactions
 */
@androidx.paging.ExperimentalPagingApi
@Composable
fun AgentChatSystem(
    viewModel: MpesaViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.agentChatMessages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    
    // Auto-scroll to latest message
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            delay(100) // Small delay for animation
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Card(
        modifier = modifier.glass(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Chat Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 Agent Communication",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Text(
                    text = "${chatMessages.size} messages",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageBubble(
                        message = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Empty state
                if (chatMessages.isEmpty()) {
                    item {
                        EmptyChatState(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AgentChatMessage,
    modifier: Modifier = Modifier
) {
    // Color scheme based on agent type
    val agentColor = when (message.agentType) {
        AgentType.FINANCE_IQ -> Color(0xFF4A90E2) // Blue
        AgentType.SPEND_WISE -> Color(0xFF9B59B6)  // Purple
    }
    
    val agentEmoji = when (message.agentType) {
        AgentType.FINANCE_IQ -> "💎"
        AgentType.SPEND_WISE -> "🧠"
    }
    
    // Slide-in animation for new messages
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(500))
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = if (message.agentType == AgentType.FINANCE_IQ) {
                Arrangement.Start
            } else {
                Arrangement.End
            }
        ) {
            if (message.agentType == AgentType.FINANCE_IQ) {
                // FinanceIQ messages (left aligned)
                MessageBubble(
                    message = message,
                    agentColor = agentColor,
                    agentEmoji = agentEmoji,
                    isLeftAligned = true,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            } else {
                Spacer(modifier = Modifier.weight(0.15f))
                // SpendWise messages (right aligned)
                MessageBubble(
                    message = message,
                    agentColor = agentColor,
                    agentEmoji = agentEmoji,
                    isLeftAligned = false,
                    modifier = Modifier.weight(0.85f)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: AgentChatMessage,
    agentColor: Color,
    agentEmoji: String,
    isLeftAligned: Boolean,
    modifier: Modifier = Modifier
) {
    // Typing indicator animation
    val typingAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
    ) {
        // Message Bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isLeftAligned) 4.dp else 16.dp,
                        bottomEnd = if (isLeftAligned) 16.dp else 4.dp
                    )
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            agentColor.copy(alpha = 0.3f),
                            agentColor.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                // Agent Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isLeftAligned) Arrangement.Start else Arrangement.End
                ) {
                    if (isLeftAligned) {
                        Text(
                            text = agentEmoji,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    Text(
                        text = when (message.agentType) {
                            AgentType.FINANCE_IQ -> "FinanceIQ"
                            AgentType.SPEND_WISE -> "SpendWise"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = agentColor,
                        textAlign = if (isLeftAligned) TextAlign.Start else TextAlign.End
                    )
                    
                    if (!isLeftAligned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = agentEmoji,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Message Content
                if (message.isThinking) {
                    // Typing indicator
                    Text(
                        text = "thinking...",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = typingAlpha),
                        textAlign = if (isLeftAligned) TextAlign.Start else TextAlign.End,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    // Actual message
                    Text(
                        text = message.message,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = if (isLeftAligned) TextAlign.Start else TextAlign.End,
                        lineHeight = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Timestamp
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = if (isLeftAligned) TextAlign.Start else TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💬",
            fontSize = 48.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Agents will communicate here\nduring processing",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Tap the 🤖 Demo button to see them in action!",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 1000 -> "now"
        diff < 60000 -> "${diff / 1000}s ago"
        diff < 3600000 -> "${diff / 60000}m ago"
        else -> "${diff / 3600000}h ago"
    }
}