package com.example.plantcarecompanion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GradientHeroCard(
    modifier: Modifier = Modifier, // lets the caller add outer margins (applied before the rounded clip)
    contentPadding: Dp = 20.dp, // space between the card edge and its content
    content: @Composable ColumnScope.() -> Unit // whatever should be drawn on top of the gradient
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)) // rounds the corners, same radius as the other screens
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                )
            )
            .padding(contentPadding),
        content = content
    )
}
