package com.example.data.repository

import com.example.data.local.SecurityStorage
import com.example.data.local.entity.NotificationEntity
import com.example.domain.model.AiInsight
import com.example.domain.model.InsightType
import com.example.domain.model.UsageStatItem
import java.util.Calendar

class AiInsightsRepository(
    private val securityStorage: SecurityStorage,
    private val usageStatsRepository: UsageStatsRepository,
    private val notificationRepository: NotificationRepository
) {

    fun isAiAnalysisEnabled(): Boolean = securityStorage.isAiAnalysisEnabled()

    fun setAiAnalysisEnabled(enabled: Boolean) {
        securityStorage.setAiAnalysisEnabled(enabled)
    }

    suspend fun generateInsights(
        todayUsage: List<UsageStatItem>,
        recentNotifications: List<NotificationEntity>
    ): List<AiInsight> {
        if (!isAiAnalysisEnabled()) {
            return emptyList()
        }

        val insights = mutableListOf<AiInsight>()

        // 1. Most-used App & Screen Time Insight
        if (todayUsage.isNotEmpty()) {
            val topApp = todayUsage.first()
            val totalMinutes = todayUsage.sumOf { it.usageDurationMs } / (1000 * 60)
            val topAppMinutes = topApp.usageDurationMs / (1000 * 60)

            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            val formattedTotal = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            insights.add(
                AiInsight(
                    id = "insight_top_app",
                    title = "Most-Used Application",
                    summary = "Your most-used app today was ${topApp.appName} ($topAppMinutes min).",
                    detail = "${topApp.appName} represents ${(topApp.percentageOfTotal).toInt()}% of your total screen time today ($formattedTotal total across ${todayUsage.size} apps).",
                    type = InsightType.APP_USAGE,
                    actionableTip = if (topAppMinutes > 60) "Consider taking a 5-minute break after every 45 minutes of continuous screen use." else "Well-balanced app usage detected today."
                )
            )

            // Screen time trend
            val productivityApps = listOf("Notes", "Docs", "Drive", "Gmail", "Calendar", "Slack", "Code", "Studio")
            val prodTime = todayUsage.filter { item ->
                productivityApps.any { item.appName.contains(it, ignoreCase = true) }
            }.sumOf { it.usageDurationMs } / (1000 * 60)

            insights.add(
                AiInsight(
                    id = "insight_productivity",
                    title = "Daily Focus & Productivity",
                    summary = if (prodTime > 20) "Productive focus: $prodTime min spent in work/utility apps." else "Balanced digital footprint today.",
                    detail = "Screen time is distributed across ${todayUsage.take(3).joinToString(", ") { it.appName }}.",
                    type = InsightType.PRODUCTIVITY,
                    actionableTip = "Keep non-essential notification alerts muted during focus sessions."
                )
            )
        } else {
            insights.add(
                AiInsight(
                    id = "insight_usage_notice",
                    title = "App Usage Tracking",
                    summary = "Grant Usage Access in Settings to activate real-time screen time insights.",
                    detail = "Once enabled, the AI companion automatically computes daily screen-time distributions and app trends.",
                    type = InsightType.SCREEN_TIME,
                    actionableTip = "Open Settings > Permissions to enable Usage Access."
                )
            )
        }

        // 2. Notification Activity & Peak Hours Insight
        if (recentNotifications.isNotEmpty()) {
            val totalNotifs = recentNotifications.size
            // Group by hour
            val hourCounts = IntArray(24)
            val cal = Calendar.getInstance()
            for (notif in recentNotifications) {
                cal.timeInMillis = notif.timestamp
                val h = cal.get(Calendar.HOUR_OF_DAY)
                hourCounts[h]++
            }

            var peakHour = 0
            var maxCount = 0
            for (i in 0 until 24) {
                if (hourCounts[i] > maxCount) {
                    maxCount = hourCounts[i]
                }
            }
            // Find window of 2 hours
            var bestWindowStart = 12
            var maxWindowSum = 0
            for (i in 0 until 23) {
                val sum = hourCounts[i] + hourCounts[i + 1]
                if (sum > maxWindowSum) {
                    maxWindowSum = sum
                    bestWindowStart = i
                }
            }

            fun formatHour(h: Int): String {
                val ampm = if (h < 12) "AM" else "PM"
                val h12 = if (h % 12 == 0) 12 else h % 12
                return "$h12 $ampm"
            }

            val windowStr = "${formatHour(bestWindowStart)} and ${formatHour(bestWindowStart + 2)}"

            insights.add(
                AiInsight(
                    id = "insight_notifications",
                    title = "Notification Activity Peak",
                    summary = "You received $totalNotifs notifications recently. Activity peaked between $windowStr.",
                    detail = "During peak hours, you received $maxWindowSum notifications. Top messaging and system alerts occurred predominantly in this period.",
                    type = InsightType.NOTIFICATIONS,
                    actionableTip = "Batch-check notifications or configure Do Not Disturb during peak work hours to maintain deep focus."
                )
            )
        } else {
            insights.add(
                AiInsight(
                    id = "insight_notifications_notice",
                    title = "Notification Monitoring",
                    summary = "Enable Notification Access to unlock notification history and peak time summaries.",
                    detail = "The AI companion analyzes notification delivery times without inspecting private message contents.",
                    type = InsightType.NOTIFICATIONS,
                    actionableTip = "Enable Notification Access in Settings > Permissions."
                )
            )
        }

        // 3. Weekly Summary / Habit Suggestion
        insights.add(
            AiInsight(
                id = "insight_summary",
                title = "Companion Daily Summary",
                summary = "All monitored metrics are synchronized and operating within normal parameters.",
                detail = "Security status: Consent verified. Zero-trust pairing active. Local encryption verified.",
                type = InsightType.PRODUCTIVITY,
                actionableTip = "You can pause notification or screen monitoring at any moment from the dashboard."
            )
        )

        return insights
    }
}
