package com.google.sample.fcdemo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush

fun Modifier.glass(
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.1f
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha + 0.05f),
                Color.White.copy(alpha = alpha)
            )
        )
    )
    .border(
        BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        RoundedCornerShape(cornerRadius)
    ) 