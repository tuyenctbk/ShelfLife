package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PantryItem
import com.example.ui.engagement.EngagementManager
import com.example.ui.engagement.PrivacyPolicyDialog
import com.example.ui.engagement.RateAppDialog
import com.example.ui.engagement.ShareAppDialog
import com.example.ui.engagement.UpdateAppDialog

@Composable
fun PantryStatsScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engagementManager = remember { EngagementManager(context) }
    val activeItems by viewModel.rawActiveItems.collectAsState()
    val consumedItems by viewModel.consumedItems.collectAsState()
    val wastedItems by viewModel.wastedItems.collectAsState()

    val totalProcessed = consumedItems.size + wastedItems.size
    val saveRate = if (totalProcessed > 0) (consumedItems.size.toFloat() / totalProcessed.toFloat()) * 100f else 100f
    val dollarsSaved = consumedItems.size * 4.75 // average grocery item cost ~$4.75

    var showRateDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize()
                .padding(16.dp)
                .testTag("stats_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.stats_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.stats_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Hero Savings Card
            item {
                val isDark = isSystemInDarkTheme()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF143823) else Color(0xFFDCFCE7)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF15803D))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = stringResource(R.string.stats_money_saved_title),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$${String.format("%.2f", dollarsSaved)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF14532D)
                            )
                            Text(
                                text = stringResource(R.string.stats_rescued_sub, consumedItems.size),
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                            )
                        }
                    }
                }
            }

            // Key Metrics Grid (Adaptive for narrow phones vs tablets)
            item {
                val isDark = isSystemInDarkTheme()
                val rescuedBg = if (isDark) Color(0xFF172554) else Color(0xFFEFF6FF)
                val rescuedIcon = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                val spoiledBg = if (isDark) Color(0xFF3B1212) else Color(0xFFFEF2F2)
                val spoiledIcon = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 440.dp
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MetricCard(
                                    title = stringResource(R.string.stats_in_stock),
                                    value = "${activeItems.size}",
                                    subtext = stringResource(R.string.stats_in_stock_sub),
                                    icon = Icons.Default.Kitchen,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    title = stringResource(R.string.stats_rescued),
                                    value = "${consumedItems.size}",
                                    subtext = stringResource(R.string.stats_rescued_sub_short),
                                    icon = Icons.Default.CheckCircle,
                                    containerColor = rescuedBg,
                                    iconColor = rescuedIcon,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            MetricCard(
                                title = stringResource(R.string.stats_spoiled),
                                value = "${wastedItems.size}",
                                subtext = stringResource(R.string.stats_spoiled_sub),
                                icon = Icons.Default.Warning,
                                containerColor = spoiledBg,
                                iconColor = spoiledIcon,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MetricCard(
                                title = stringResource(R.string.stats_in_stock),
                                value = "${activeItems.size}",
                                subtext = stringResource(R.string.stats_in_stock_sub),
                                icon = Icons.Default.Kitchen,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = stringResource(R.string.stats_rescued),
                                value = "${consumedItems.size}",
                                subtext = stringResource(R.string.stats_rescued_sub_short),
                                icon = Icons.Default.CheckCircle,
                                containerColor = rescuedBg,
                                iconColor = rescuedIcon,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = stringResource(R.string.stats_spoiled),
                                value = "${wastedItems.size}",
                                subtext = stringResource(R.string.stats_spoiled_sub),
                                icon = Icons.Default.Warning,
                                containerColor = spoiledBg,
                                iconColor = spoiledIcon,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Food Waste Prevention Progress Bar
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.stats_prevention_rate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${saveRate.toInt()}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { (saveRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (saveRate >= 80f) stringResource(R.string.stats_rate_good) else stringResource(R.string.stats_rate_improve),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Community & Settings Section
            item {
                Text(
                    text = stringResource(R.string.settings_section_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Star,
                            iconTint = Color(0xFFD97706),
                            title = stringResource(R.string.btn_rate_app),
                            onClick = { showRateDialog = true }
                        )
                        SettingsRow(
                            icon = Icons.Default.Share,
                            iconTint = Color(0xFF15803D),
                            title = stringResource(R.string.btn_share_app),
                            onClick = { showShareDialog = true }
                        )
                        SettingsRow(
                            icon = Icons.Default.SystemUpdate,
                            iconTint = Color(0xFF0284C7),
                            title = stringResource(R.string.btn_check_updates),
                            onClick = {
                                val currentVer = try {
                                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                                } catch (_: Exception) {
                                    "1.0"
                                }
                                val latestVer = engagementManager.getLatestAvailableVersion()
                                if (latestVer != null && engagementManager.isNewerVersion(currentVer, latestVer)) {
                                    showUpdateDialog = true
                                } else {
                                    Toast.makeText(context, context.getString(R.string.app_up_to_date), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.Policy,
                            iconTint = Color(0xFF64748B),
                            title = stringResource(R.string.btn_privacy_policy),
                            onClick = { showPrivacyDialog = true }
                        )
                    }
                }
            }

            // Rescued Items History
            item {
                Text(
                    text = stringResource(R.string.stats_recently_rescued),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (consumedItems.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.stats_no_rescued_yet),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(consumedItems.take(5)) { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${item.brand} • ${item.storageLocation}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = stringResource(R.string.stats_rescued),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRateDialog) {
        RateAppDialog(
            onDismiss = {
                engagementManager.onUserDismissedRating(false)
                showRateDialog = false
            },
            onRateNow = {
                engagementManager.onUserRated()
                showRateDialog = false
            },
            onNeverAskAgain = {
                engagementManager.onUserDismissedRating(true)
                showRateDialog = false
            }
        )
    }

    if (showShareDialog) {
        ShareAppDialog(
            onDismiss = {
                engagementManager.onUserDismissedShare()
                showShareDialog = false
            },
            onShare = {
                engagementManager.onUserShared()
                showShareDialog = false
            }
        )
    }

    if (showUpdateDialog) {
        val targetVer = engagementManager.getLatestAvailableVersion() ?: "1.1"
        UpdateAppDialog(
            latestVersion = targetVer,
            onDismiss = {
                engagementManager.onUserDismissedUpdate(targetVer)
                showUpdateDialog = false
            },
            onUpdate = {
                engagementManager.onUserUpdated(targetVer)
                showUpdateDialog = false
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                if (isFocused) 2.dp else 0.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(8.dp)
            )
            .background(
                if (isFocused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
