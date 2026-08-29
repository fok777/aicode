package com.aicode.feature.settings.domain.model

/**
 * 套餐余量/卡片查询结果。
 */
data class ProviderBalanceResult(
    val card: AdaptiveCardRoot = AdaptiveCardRoot(),
    val rawOutput: String = ""
)

/**
 * 套餐余量状态。
 */
sealed interface ProviderBalanceState {
    data object Idle : ProviderBalanceState
    data object Loading : ProviderBalanceState
    data class Success(val result: ProviderBalanceResult) : ProviderBalanceState
    data class Error(val message: String, val rawOutput: String = "") : ProviderBalanceState
}
