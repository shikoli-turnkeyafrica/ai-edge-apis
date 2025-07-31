package com.google.sample.fcdemo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.sample.fcdemo.R
import com.google.sample.fcdemo.data.TransactionEntity
import com.google.sample.fcdemo.viewmodel.MpesaViewModel
import com.google.sample.fcdemo.ui.theme.glass
import java.text.NumberFormat
import java.util.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.blur
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import androidx.paging.ExperimentalPagingApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagingApi::class, ExperimentalFoundationApi::class)
@Composable
fun MpesaTrackerScreen(
    viewModel: MpesaViewModel
) {
    // Privacy toggle states
    var showNames by remember { mutableStateOf(true) }
    var showAmounts by remember { mutableStateOf(true) }
    // View mode toggle (paged vs grouped)
    var useGroupedView by remember { mutableStateOf(false) }

    val pagedTransactions = viewModel.pagedTransactions.collectAsLazyPagingItems()
    val groupedTransactions by viewModel.groupedTransactions.collectAsStateWithLifecycle(initialValue = emptyMap())
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val workProgress by viewModel.workProgress.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.wealthwise_grad_start),
                        colorResource(R.color.wealthwise_grad_end)
                    )
                )
            )
            .padding(16.dp)
    ) {
        CashFlowHeader()
        Spacer(modifier = Modifier.height(16.dp))
        
        // Privacy and View toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Privacy toggles (smaller)
            PrivacyToggles(
                modifier = Modifier.weight(2f),
                showNames = showNames,
                showAmounts = showAmounts,
                onToggleNames = { showNames = !showNames },
                onToggleAmounts = { showAmounts = !showAmounts }
            )
            
            // View mode toggle
            ViewModeToggle(
                modifier = Modifier.weight(1f),
                useGroupedView = useGroupedView,
                onToggleView = { useGroupedView = !useGroupedView }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Processing Indicator
        if (isProcessing) {
            ProcessingCard(workProgress)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Transactions List - switch between paged and grouped views
        if (useGroupedView) {
            TransactionsListGrouped(
                groupedTransactions = groupedTransactions,
                showNames = showNames,
                showAmounts = showAmounts
            )
        } else {
            TransactionsListPaged(
                items = pagedTransactions,
                showNames = showNames,
                showAmounts = showAmounts
            )
        }
    }
}

@Composable
private fun ViewModeToggle(
    modifier: Modifier = Modifier,
    useGroupedView: Boolean,
    onToggleView: () -> Unit
) {
    Card(
        modifier = modifier.glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        onClick = onToggleView
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (useGroupedView) Icons.Default.DateRange else Icons.AutoMirrored.Filled.List,
                contentDescription = if (useGroupedView) "Switch to list view" else "Switch to grouped view",
                tint = if (useGroupedView) Color.Cyan else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (useGroupedView) "Grouped" else "List",
                color = if (useGroupedView) Color.Cyan else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PrivacyToggles(
    modifier: Modifier = Modifier,
    showNames: Boolean,
    showAmounts: Boolean,
    onToggleNames: () -> Unit,
    onToggleAmounts: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Names toggle
        Card(
            modifier = Modifier
                .weight(1f)
                .glass(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            onClick = onToggleNames
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (showNames) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (showNames) "Hide names" else "Show names",
                    tint = if (showNames) Color.Green else Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showNames) "Hide Names" else "Show Names",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Amounts toggle
        Card(
            modifier = Modifier
                .weight(1f)
                .glass(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(0.dp),
            onClick = onToggleAmounts
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (showAmounts) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = if (showAmounts) "Hide amounts" else "Show amounts",
                    tint = if (showAmounts) Color.Green else Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showAmounts) "Hide Amounts" else "Show Amounts",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CashFlowHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = colorResource(R.color.cashflow_primary),
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.cashflow_primary)
            )
            
            Text(
                text = stringResource(R.string.tagline),
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProcessingCard(workProgress: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colorResource(R.color.cashflow_secondary),
                strokeWidth = 2.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = workProgress,
                color = colorResource(R.color.cashflow_secondary),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransactionsListPaged(
    items: LazyPagingItems<TransactionEntity>,
    showNames: Boolean,
    showAmounts: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recent_transactions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (items.itemCount == 0 && items.loadState.refresh !is androidx.paging.LoadState.Loading) {
                EmptyTransactionsView()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items.itemCount) { index ->
                        items[index]?.let { tx ->
                            TransactionItem(
                                transaction = tx,
                                showName = showNames,
                                showAmount = showAmounts
                            )
                        }
                    }

                    when (items.loadState.append) {
                        is androidx.paging.LoadState.Loading -> {
                            item { Text("Loading more…", color = Color.Gray, modifier = Modifier.padding(8.dp)) }
                        }
                        is androidx.paging.LoadState.Error -> {
                            item { Text("Error loading", color = Color.Red, modifier = Modifier.padding(8.dp)) }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_transactions),
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.waiting_for_sms),
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionEntity,
    showName: Boolean,
    showAmount: Boolean
) {
    val isSent = transaction.direction.lowercase() == "sent"
    val amountColor = if (isSent) colorResource(R.color.money_sent) else colorResource(R.color.money_received)
    val icon = if (isSent) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showName) transaction.counterparty else "••••••",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = transaction.transactionId,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (showAmount) "KSh ${NumberFormat.getNumberInstance(Locale.US).format(transaction.amountKes)}" else "KSh ••••",
                    color = amountColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = transaction.dateTime,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionsListGrouped(
    groupedTransactions: Map<String, List<TransactionEntity>>,
    showNames: Boolean,
    showAmounts: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recent_transactions),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (groupedTransactions.isEmpty()) {
                EmptyTransactionsView()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groupedTransactions.forEach { (month, transactions) ->
                        stickyHeader {
                            MonthHeader(month = month, transactionCount = transactions.size)
                        }
                        
                        items(transactions) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                showName = showNames,
                                showAmount = showAmounts
                            )
                        }
                        
                        // Add spacing between months
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: String, transactionCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glass(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.cashflow_primary)
            )
            
            Text(
                text = "$transactionCount transaction${if (transactionCount != 1) "s" else ""}",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

 