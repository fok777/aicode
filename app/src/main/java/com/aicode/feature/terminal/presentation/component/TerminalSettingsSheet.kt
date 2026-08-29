package com.aicode.feature.terminal.presentation.component

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicode.R
import com.aicode.core.theme.Radius
import com.aicode.core.theme.Spacing
import com.aicode.feature.terminal.data.repository.TerminalSettings
import com.aicode.feature.terminal.domain.model.TerminalThemePreset

/**
 * 终端偏好设置底部抽屉面板。
 * 允许用户实时调整配色主题、字号大小与光标样式，并提供即时预览。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TerminalSettingsSheet(
    settings: TerminalSettings,
    onDismiss: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onChangeFontSize: (Int) -> Unit,
    onChangeCursorStyle: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.terminal_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(Spacing.md))

            // 实时预览卡片
            TerminalPreviewCard(settings = settings)

            Spacer(Modifier.height(Spacing.lg))

            // 配色主题选择
            Text(
                text = stringResource(R.string.terminal_theme_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                TerminalThemePreset.ALL_PRESETS.forEach { preset ->
                    ThemePresetChip(
                        preset = preset,
                        isSelected = preset.id == settings.themeId,
                        onSelect = { onSelectTheme(preset.id) }
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // 字号大小滑块
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.terminal_font_size_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${settings.fontSizeSp} sp",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onChangeFontSize(it.toInt()) },
                valueRange = 10f..22f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.md))

            // 光标样式选择
            Text(
                text = stringResource(R.string.terminal_cursor_style_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                listOf(
                    0 to stringResource(R.string.terminal_cursor_block),
                    1 to stringResource(R.string.terminal_cursor_underline),
                    2 to stringResource(R.string.terminal_cursor_bar)
                ).forEach { (style, label) ->
                    FilterChip(
                        selected = settings.cursorStyle == style,
                        onClick = { onChangeCursorStyle(style) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}

/** 终端实时预览小窗。 */
@Composable
private fun TerminalPreviewCard(settings: TerminalSettings) {
    val theme = settings.theme
    val bgColor = Color(theme.background)
    val fgColor = Color(theme.foreground)
    val cursorColor = Color(theme.cursor)
    val greenColor = Color(theme.ansiColors.getOrElse(2) { theme.foreground })
    val blueColor = Color(theme.ansiColors.getOrElse(4) { theme.foreground })
    val yellowColor = Color(theme.ansiColors.getOrElse(3) { theme.foreground })

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.md),
        color = bgColor,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "root@aicode",
                    color = greenColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ":",
                    color = fgColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp
                )
                Text(
                    text = "~/workspace",
                    color = blueColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$ ./gradlew build",
                    color = fgColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "> Task :app:assembleDebug ",
                    color = yellowColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp
                )
                Text(
                    text = "SUCCESS",
                    color = greenColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$ git status",
                    color = fgColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = settings.fontSizeSp.sp
                )
                Spacer(Modifier.width(2.dp))
                // 模拟光标
                Box(
                    modifier = Modifier
                        .size(
                            width = when (settings.cursorStyle) {
                                1 -> 8.dp
                                2 -> 2.dp
                                else -> 8.dp
                            },
                            height = when (settings.cursorStyle) {
                                1 -> 2.dp
                                2 -> (settings.fontSizeSp + 2).dp
                                else -> (settings.fontSizeSp + 2).dp
                            }
                        )
                        .background(cursorColor)
                )
            }
        }
    }
}

/** 主题选择色块 Chip。 */
@Composable
private fun ThemePresetChip(
    preset: TerminalThemePreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(Radius.sm)
            ),
        shape = RoundedCornerShape(Radius.sm),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(preset.nameRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
