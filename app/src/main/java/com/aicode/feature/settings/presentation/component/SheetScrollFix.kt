package com.aicode.feature.settings.presentation.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Velocity

/**
 * 修复 Material3 ModalBottomSheet 已知 bug（issuetracker 486562294，1.4.x 仍存在）：
 * 内容接近全屏高度且有可滚动区域时，快速上滑 fling 到内容边界，未消费的向上速度会传给
 * sheet 的拖拽状态，导致 sheet 反复「向下拖动再弹回」振荡。
 *
 * 挂到可滚动内容外层：sheet 已完全展开时吞掉向上方向的剩余 fling 速度，使速度不再流向
 * sheet 的 AnchoredDraggableState；内容自身的惯性滚动不受影响（只在子滚动消费完后拦截剩余量）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetFlingFix(sheetState: SheetState): NestedScrollConnection =
    remember(sheetState) {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (sheetState.currentValue == SheetValue.Expanded && available.y > 0f) available
                else Velocity.Zero
        }
    }
