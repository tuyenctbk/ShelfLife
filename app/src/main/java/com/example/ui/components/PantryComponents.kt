package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation

@Composable
fun DaysLeftBadge(
    days: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val (backgroundColor, textColor) = when {
        days < 0 -> if (isDark) Color(0xFF450A0A) to Color(0xFFF87171) else Color(0xFFFEE2E2) to Color(0xFFDC2626)
        days == 0 -> if (isDark) Color(0xFF450A0A) to Color(0xFFF87171) else Color(0xFFFEF2F2) to Color(0xFFEF4444)
        days == 1 -> if (isDark) Color(0xFF451A03) to Color(0xFFFBBF24) else Color(0xFFFEF3C7) to Color(0xFFD97706)
        days in 2..3 -> if (isDark) Color(0xFF451A03) to Color(0xFFFCD34D) else Color(0xFFFEF3C7) to Color(0xFFB45309)
        else -> if (isDark) Color(0xFF064E3B) to Color(0xFF4ADE80) else Color(0xFFDCFCE7) to Color(0xFF15803D)
    }

    val text = when {
        days < 0 -> stringResource(R.string.expired)
        days == 0 -> stringResource(R.string.expires_today)
        days == 1 -> stringResource(R.string.one_day_left)
        else -> stringResource(R.string.days_left, days)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (days <= 3) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 2.dp)
                )
            }
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NutriScoreBadge(score: String, modifier: Modifier = Modifier) {
    val color = when (score.uppercase()) {
        "A" -> Color(0xFF15803D)
        "B" -> Color(0xFF84CC16)
        "C" -> Color(0xFFEAB308)
        "D" -> Color(0xFFF97316)
        else -> Color(0xFFEF4444)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "Nutri-Score $score",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ExpiryRadarBanner(
    expiringItems: List<PantryItem>,
    onFindRecipesClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = expiringItems.isNotEmpty()) {
        val isDark = isSystemInDarkTheme()
        var isBtnFocused by remember { mutableStateOf(false) }

        val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF261D10) else Color(0xFFFFFBEB)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (isDark) Color(0xFFD97706).copy(alpha = 0.6f) else Color(0xFFFDE68A),
                    RoundedCornerShape(20.dp)
                )
                .testTag("expiry_radar_banner")
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF3B2A14) else Color(0xFFFEF3C7))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar",
                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.radar_active_title),
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.radar_expiring_count, expiringItems.size),
                            color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preview of expiring item names
                Text(
                    text = stringResource(R.string.expiring_prefix) + expiringItems.joinToString(", ") { it.name },
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFF78350F),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onFindRecipesClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
                        contentColor = if (isDark) Color(0xFF1E1508) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isBtnFocused = it.isFocused }
                        .border(
                            if (isBtnFocused) 2.5.dp else 0.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("rescue_recipes_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.radar_query_recipes),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StorageLocationIcon(locationStr: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    val icon: ImageVector = when (locationStr) {
        StorageLocation.FRIDGE.name -> Icons.Default.AcUnit
        StorageLocation.FREEZER.name -> Icons.Default.SevereCold
        else -> Icons.Default.Kitchen
    }
    Icon(
        imageVector = icon,
        contentDescription = locationStr,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun StorageFilterChips(
    selectedLocation: StorageLocation?,
    onLocationSelected: (StorageLocation?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        FilterChip(
            selected = selectedLocation == null,
            onClick = { onLocationSelected(null) },
            label = { Text(stringResource(R.string.filter_all), fontWeight = FontWeight.Medium) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.testTag("filter_all")
        )

        StorageLocation.entries.forEach { loc ->
            val label = when (loc) {
                StorageLocation.FRIDGE -> stringResource(R.string.loc_fridge)
                StorageLocation.PANTRY -> stringResource(R.string.loc_pantry)
                StorageLocation.FREEZER -> stringResource(R.string.loc_freezer)
            }
            FilterChip(
                selected = selectedLocation == loc,
                onClick = { onLocationSelected(loc) },
                leadingIcon = {
                    StorageLocationIcon(
                        locationStr = loc.name,
                        tint = if (selectedLocation == loc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(label, fontWeight = FontWeight.Medium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("filter_${loc.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantryItemCard(
    item: PantryItem,
    onItemClick: () -> Unit,
    onConsumeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val daysLeft = item.daysUntilExpiry()
    val isUrgent = daysLeft in 0..3
    var isFocused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "pantryItemFocusScale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) {
                if (isDark) Color(0xFF261D10) else Color(0xFFFFFBEB)
            } else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 6.dp else 2.dp),
        border = if (isFocused) {
            BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(
                1.dp,
                if (isUrgent) {
                    if (isDark) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .scale(focusScale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onItemClick)
            .testTag("pantry_item_${item.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Food Thumbnail or Placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                        loading = {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.matchParentSize()) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            }
                        },
                        error = {
                            StorageLocationIcon(
                                locationStr = item.storageLocation,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                } else {
                    StorageLocationIcon(
                        locationStr = item.storageLocation,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details Column with FlowRow to eliminate text overflow
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.brand.isNotBlank()) {
                    Text(
                        text = item.brand,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DaysLeftBadge(days = daysLeft)

                    item.nutriScore?.let { score ->
                        NutriScoreBadge(score = score)
                    }

                    Text(
                        text = "${item.quantity.toString().removeSuffix(".0")} ${item.unit}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Quick actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = onConsumeClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("consume_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.btn_consumed),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
