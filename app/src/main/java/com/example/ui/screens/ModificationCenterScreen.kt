package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SettingCategory
import com.example.data.model.TweakStatus
import com.example.ui.RouterViewModel
import com.example.ui.components.SettingCard
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceBorder
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ModificationCenterScreen(
    viewModel: RouterViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = settings.filter { item ->
        val matchesCategory = filterCategory == null || item.category == filterCategory
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.romanUrduExplanation.contains(searchQuery, ignoreCase = true) ||
                item.checkCommand.contains(searchQuery, ignoreCase = true) ||
                item.applyCommand.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val appliedCount = settings.count { it.status == TweakStatus.APPLIED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Screen Header & Revert All Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Router Modification Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Har setting ka Roman Urdu matlab + Check, Apply aur Verified Restore",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (appliedCount > 0) {
                    OutlinedButton(
                        onClick = { viewModel.restoreAllDefaults() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusWarning),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusWarning),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revert All ($appliedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings, commands, or Roman Urdu keywords...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_settings_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberSurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Category Filter Chips Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterCategory == null,
                    onClick = { viewModel.setFilterCategory(null) },
                    label = { Text("All (${settings.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = CyberBg,
                        containerColor = CyberSurfaceVariant,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = CyberSurfaceBorder,
                        selectedBorderColor = NeonCyan,
                        enabled = true,
                        selected = filterCategory == null
                    )
                )

                SettingCategory.entries.forEach { cat ->
                    val countInCat = settings.count { it.category == cat }
                    FilterChip(
                        selected = filterCategory == cat,
                        onClick = {
                            viewModel.setFilterCategory(if (filterCategory == cat) null else cat)
                        },
                        label = { Text("${cat.displayName} ($countInCat)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = CyberBg,
                            containerColor = CyberSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = CyberSurfaceBorder,
                            selectedBorderColor = NeonCyan,
                            enabled = true,
                            selected = filterCategory == cat
                        )
                    )
                }
            }
        }

        // Setting Cards List
        items(filteredList, key = { it.id }) { setting ->
            SettingCard(
                setting = setting,
                onCheck = { viewModel.checkSetting(setting.id) },
                onApply = { viewModel.applySetting(setting.id) },
                onRestore = { viewModel.restoreSetting(setting.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
