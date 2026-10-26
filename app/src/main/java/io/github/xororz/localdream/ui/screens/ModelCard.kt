package io.github.xororz.localdream.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.Model
import io.github.xororz.localdream.ui.theme.Motion

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModelCard(
    model: Model,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPinned: Boolean = false,
) {
    val isDisabledInSelection = !model.isDownloaded && isSelectionMode

    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 4f else 1f,
        animationSpec = Motion.springExpressiveSpatial(),
        label = "CardElevationAnimation",
    )

    val targetContainer = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        isDisabledInSelection -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val backgroundColor by animateColorAsState(
        targetValue = targetContainer,
        animationSpec = tween(Motion.DurationMedium, easing = Motion.Standard),
        label = "CardBackgroundColorAnimation",
    )

    val primaryContent = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        isDisabledInSelection -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val secondaryContent = when {
        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
        isDisabledInSelection -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor,
            contentColor = primaryContent,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = elevation.dp,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        // The clickable lives inside the card so its press/ripple indication is
        // clipped to the card's rounded shape. On the outer modifier the ripple
        // would render as a rectangle and show square corners on long-press.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (!isSelectionMode || model.isDownloaded) onClick()
                    },
                    onLongClick = {
                        if (model.isDownloaded && !isSelectionMode) onLongClick()
                    },
                ),
        ) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                containerColor = if (model.runOnCpu) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (model.runOnCpu) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            ) {
                Text(
                    text = if (model.runOnCpu) "CPU" else "NPU",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.pin_to_top),
                            tint = primaryContent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = primaryContent,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = secondaryContent,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        InfoChip(
                            icon = Icons.Default.SdStorage,
                            label = model.approximateSize,
                            color = secondaryContent,
                        )
                        InfoChip(
                            icon = Icons.Default.AspectRatio,
                            label = if (model.runOnCpu) {
                                "128~512"
                            } else {
                                "${model.generationSize}×${model.generationSize}"
                            },
                            color = secondaryContent,
                        )
                    }

                    when {
                        model.isDownloaded -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                val statusColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "downloaded",
                                        tint = statusColor,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = stringResource(R.string.downloaded),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor,
                                    )
                                }
                            }
                        }

                        else -> {
                            InfoChip(
                                icon = Icons.Default.CloudDownload,
                                label = stringResource(R.string.download),
                                color = secondaryContent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

