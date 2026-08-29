package com.aicode.feature.git.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicode.R
import com.aicode.core.theme.Radius
import com.aicode.core.theme.Spacing
import com.aicode.core.theme.semanticColors
import com.aicode.feature.git.domain.model.GitFileChange
import com.aicode.feature.git.domain.model.GitStatus
import com.aicode.feature.settings.presentation.component.SettingsDivider
import com.aicode.feature.settings.presentation.component.SettingsGroup
import com.aicode.feature.settings.presentation.component.settingsLightMode
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import compose.icons.feathericons.DownloadCloud
import compose.icons.feathericons.GitBranch
import compose.icons.feathericons.Minus
import compose.icons.feathericons.Plus
import compose.icons.feathericons.UploadCloud

@Composable
internal fun StatusTab(
    status: GitStatus?,
    busy: Boolean,
    hasRemote: Boolean,
    hasIdentity: Boolean,
    scrollState: ScrollState,
    onStage: (String) -> Unit,
    onUnstage: (String) -> Unit,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onCommit: () -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onFileDiff: (String) -> Unit,
    onStagedFileDiff: (String) -> Unit
) {
    val s = status
    val clean = s == null || (s.staged.isEmpty() && s.unstaged.isEmpty() && s.untracked.isEmpty())
    val hasChanges = !clean
    var showUnstageAllConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.lg)
            // 底部留出悬浮 tab bar 高度：滚动时内容可滚过 tab 区域被蒙版渐隐，
            // 滚到底时最后一项停在 tab 上方不被遮挡。
            .padding(bottom = 70.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        StatusOverview(status = s, clean = clean)

        // 主操作：提交。有已暂存改动且已配置署名才可用。
        val commitEnabled = !busy && (s?.staged?.isNotEmpty() == true) && hasIdentity
        FilledTonalButton(
            onClick = onCommit,
            enabled = commitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Icon(FeatherIcons.Check, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.git_commit_changes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!hasIdentity) {
            // 禁用原因提示：用户不知道按钮为什么不可点时给出指引
            Text(
                text = stringResource(R.string.git_commit_needs_identity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // 次级操作：暂存全部 / 拉取 / 推送。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            ActionButton(
                label = stringResource(R.string.git_stage_all),
                icon = FeatherIcons.Plus,
                enabled = !busy && hasChanges,
                onClick = onStageAll,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = stringResource(R.string.git_pull),
                icon = FeatherIcons.DownloadCloud,
                enabled = !busy && hasRemote,
                onClick = onPull,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = stringResource(R.string.git_push),
                icon = FeatherIcons.UploadCloud,
                enabled = !busy && hasRemote,
                onClick = onPush,
                modifier = Modifier.weight(1f)
            )
        }

        if (clean) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.git_status_clean),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val ss = s ?: return@Column
            if (ss.staged.isNotEmpty()) {
                GroupHeaderWithAction(
                    title = stringResource(R.string.git_staged_count, ss.staged.size),
                    actionLabel = stringResource(R.string.git_action_unstage_all),
                    actionEnabled = !busy,
                    onAction = { showUnstageAllConfirm = true }
                )
                SettingsGroup {
                    ss.staged.forEachIndexed { index, f ->
                        if (index > 0) SettingsDivider()
                        FileRow(
                            file = f,
                            actionIcon = FeatherIcons.Minus,
                            actionDesc = stringResource(R.string.git_unstage),
                            onAction = { onUnstage(f.path) },
                            enabled = !busy,
                            onClick = { onStagedFileDiff(f.path) }
                        )
                    }
                }
            }
            if (ss.unstaged.isNotEmpty()) {
                SectionHeader(stringResource(R.string.git_modified_count, ss.unstaged.size))
                SettingsGroup {
                    ss.unstaged.forEachIndexed { index, f ->
                        if (index > 0) SettingsDivider()
                        FileRow(
                            file = f,
                            actionIcon = FeatherIcons.Plus,
                            actionDesc = stringResource(R.string.git_stage),
                            onAction = { onStage(f.path) },
                            enabled = !busy,
                            onClick = { onFileDiff(f.path) }
                        )
                    }
                }
            }
            if (ss.untracked.isNotEmpty()) {
                SectionHeader(stringResource(R.string.git_untracked_count, ss.untracked.size))
                SettingsGroup {
                    ss.untracked.forEachIndexed { index, path ->
                        if (index > 0) SettingsDivider()
                        FileRow(
                            file = GitFileChange(path, "?", staged = false),
                            actionIcon = FeatherIcons.Plus,
                            actionDesc = stringResource(R.string.git_stage),
                            onAction = { onStage(path) },
                            enabled = !busy
                        )
                    }
                }
            }
        }
    }

    if (showUnstageAllConfirm) {
        AlertDialog(
            onDismissRequest = { showUnstageAllConfirm = false },
            title = { Text(stringResource(R.string.git_action_unstage_all)) },
            text = { Text(stringResource(R.string.git_unstage_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnstageAllConfirm = false
                    onUnstageAll()
                }) { Text(stringResource(R.string.git_action_unstage_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnstageAllConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
@Composable
private fun StatusOverview(status: GitStatus?, clean: Boolean) {
    val staged = status?.staged?.size ?: 0
    val modified = status?.unstaged?.size ?: 0
    val untracked = status?.untracked?.size ?: 0

    Surface(
        color = if (settingsLightMode()) Color.White else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.lg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    FeatherIcons.GitBranch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (clean) stringResource(R.string.git_clean) else stringResource(R.string.git_has_changes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = status?.branch ?: stringResource(R.string.git_no_branch),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (status?.isDetached == true) {
                        Text(
                            text = stringResource(R.string.git_detached_head_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (status?.upstream != null) {
                        Text(
                            text = stringResource(R.string.git_tracking_branch, status.upstream),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (status != null) {
                        Text(
                            text = stringResource(R.string.git_no_upstream),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (status != null && (status.ahead > 0 || status.behind > 0)) {
                    Spacer(Modifier.width(Spacing.sm))
                    SyncPill(ahead = status.ahead, behind = status.behind)
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatusMetric(stringResource(R.string.git_staged_label), staged, MaterialTheme.semanticColors.success, Modifier.weight(1f))
                StatusMetric(stringResource(R.string.git_modified_label), modified, MaterialTheme.semanticColors.warning, Modifier.weight(1f))
                StatusMetric(stringResource(R.string.git_untracked_label), untracked, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }
        }
    }
}

/** 领先/落后远程的同步状态胶囊。 */
@Composable
private fun SyncPill(ahead: Int, behind: Int) {
    Surface(
        color = MaterialTheme.semanticColors.mutedSurface,
        shape = RoundedCornerShape(Radius.pill)
    ) {
        Text(
            text = buildString {
                if (ahead > 0) append(stringResource(R.string.git_ahead_count, ahead))
                if (behind > 0) {
                    if (isNotEmpty()) append("  ")
                    append(stringResource(R.string.git_behind_count, behind))
                }
            },
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        )
    }
}

/** 分组小标题 + 右侧操作（已暂存组的「全部取消暂存」）。 */
@Composable
private fun GroupHeaderWithAction(
    title: String,
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.md, top = Spacing.lg, end = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.semanticColors.subtleText,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onAction,
            enabled = actionEnabled,
            contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = 4.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** 分组内文件行：状态徽标 + 文件名/目录 + 行尾暂存操作；未暂存行点击打开 diff。 */
@Composable
private fun FileRow(
    file: GitFileChange,
    actionIcon: ImageVector,
    actionDesc: String,
    onAction: () -> Unit,
    enabled: Boolean,
    onClick: (() -> Unit)? = null
) {
    val fileName = file.path.substringAfterLast('/')
    val directory = file.path.substringBeforeLast('/', missingDelimiterValue = "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(file.statusCode)
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (directory.isNotEmpty()) {
                Text(
                    text = directory,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        IconButton(onClick = onAction, enabled = enabled) {
            Icon(
                actionIcon,
                contentDescription = actionDesc,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 次级操作按钮：柔和背景微卡片按钮，12dp 圆角。 */
@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (enabled) {
        MaterialTheme.semanticColors.buttonMutedBg
    } else {
        MaterialTheme.semanticColors.mutedSurface.copy(alpha = 0.5f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
    }

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = bgColor,
            contentColor = contentColor,
            disabledContainerColor = bgColor,
            disabledContentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = Spacing.sm)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
