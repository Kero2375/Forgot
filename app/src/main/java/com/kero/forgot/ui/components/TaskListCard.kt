package com.kero.forgot.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> TaskListCard(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    cornerRadius: Dp = 20.dp,
    tonalElevation: Dp = 0.dp,
    rowContent: @Composable (T) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = tonalElevation
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
                if (index > 0) {
                    DashedHorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    )
                }
                rowContent(item)
            }
        }
    }
}
