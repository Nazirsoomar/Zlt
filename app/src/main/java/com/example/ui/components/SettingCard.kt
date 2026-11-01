package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RiskLevel
import com.example.data.model.RouterSetting
import com.example.data.model.TweakStatus
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SettingCard(
    setting: RouterSetting,
    onCheck: () -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    var showTechDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("setting_card_${setting.id}")
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setting.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = setting.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan
                    )
                }

                StatusBadge(status = setting.status)
            }

            // Roman Urdu Explanation Block (As explicitly requested by user)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF142032))
                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Roman Urdu Wazahat / Explanation",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = setting.romanUrduExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )

                    if (setting.gamingImpact.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Gaming Impact",
                                tint = StatusSuccess,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gaming Asar: ${setting.gamingImpact}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Technical details toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTechDetails = !showTechDetails },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showTechDetails) "Hide technical notes" else "View technical details & risks",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                RiskBadge(risk = setting.riskLevel)
            }

            AnimatedVisibility(visible = showTechDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(
                        text = setting.englishExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = CyberSurfaceBorder)

            // Current Value Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Value (Router pe observed)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = setting.currentValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (setting.status == TweakStatus.APPLIED) NeonCyan else StatusSuccess
                    )
                }

                if (setting.recommendedValue.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                        Text(
                            text = setting.recommendedValue,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }

            // Command Box 1: Check Command
            CommandBox(
                title = "Check command",
                commandText = setting.checkCommand,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(setting.checkCommand))
                    copiedLabel = "Check command copied!"
                },
                actionButton = {
                    IconButton(
                        onClick = onCheck,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_check_${setting.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Check now",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )

            // Command Box 2: Apply Command
            CommandBox(
                title = "Apply tweak command",
                commandText = setting.applyCommand,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(setting.applyCommand))
                    copiedLabel = "Apply command copied!"
                },
                actionButton = {
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_apply_${setting.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )

            // Command Box 3: Restore Command (Verified original value)
            CommandBox(
                title = "Restore command",
                commandText = setting.activeRestoreCommand,
                subtitle = if (setting.isVerifiedRestore) {
                    "Actual router configuration se verified restore"
                } else {
                    "Router se value pehle check karein taake actual configuration save ho sake"
                },
                badgeText = if (setting.isVerifiedRestore) "Verified Original" else "Pending Verification",
                badgeColor = if (setting.isVerifiedRestore) StatusSuccess else StatusWarning,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(setting.activeRestoreCommand))
                    copiedLabel = "Restore command copied!"
                },
                actionButton = {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("btn_restore_${setting.id}"),
                        enabled = setting.isVerifiedRestore,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusWarning
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (setting.isVerifiedRestore) StatusWarning else TextTertiary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Original", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )

            if (!setting.isVerifiedRestore) {
                Text(
                    text = "Button tabhi active hoga jab actual restore operation verify ho.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }

            copiedLabel?.let { msg ->
                Text(
                    text = "✓ $msg",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusSuccess,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CommandBox(
    title: String,
    commandText: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = NeonCyan,
    onCopy: () -> Unit,
    actionButton: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, CyberSurfaceBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            // Command text code block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A0F18))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = commandText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonCyan,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action row: Copy & Execution button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberSurfaceBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy command", fontSize = 11.sp, color = TextSecondary)
                }

                actionButton()
            }
        }
    }
}

@Composable
fun StatusBadge(status: TweakStatus) {
    val (label, color) = when (status) {
        TweakStatus.DISCOVERED -> "Discovered" to ElectricBlue
        TweakStatus.VERIFIED -> "Verified" to NeonCyan
        TweakStatus.APPLIED -> "Tweak Applied" to StatusSuccess
        TweakStatus.DEFAULT -> "Default Active" to TextSecondary
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RiskBadge(risk: RiskLevel) {
    val (color, icon) = when (risk) {
        RiskLevel.SAFE -> StatusSuccess to Icons.Default.CheckCircle
        RiskLevel.MODERATE -> StatusWarning to Icons.Default.Warning
        RiskLevel.CAUTION -> StatusDanger to Icons.Default.Warning
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = risk.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}
