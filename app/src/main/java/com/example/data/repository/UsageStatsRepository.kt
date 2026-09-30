package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.example.data.local.AppDatabase
import com.example.data.local.entity.UsageEntity
import com.example.domain.model.UsageStatItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UsageStatsRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val usageDao = database.usageDao()
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val packageManager = context.packageManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUsageStatsForDate(calendar: Calendar): List<UsageStatItem> = withContext(Dispatchers.IO) {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        fetchAggregatedStats(startTime, endTime)
    }

    suspend fun getWeeklyUsageStats(): List<UsageStatItem> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = cal.timeInMillis
        fetchAggregatedStats(startTime, endTime)
    }

    suspend fun getMonthlyUsageStats(): List<UsageStatItem> = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val startTime = cal.timeInMillis
        fetchAggregatedStats(startTime, endTime)
    }

    private suspend fun fetchAggregatedStats(startTime: Long, endTime: Long): List<UsageStatItem> = withContext(Dispatchers.IO) {
        if (!hasUsageAccess() || usageStatsManager == null) {
            return@withContext emptyList()
        }

        val rawStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            endTime
        ) ?: return@withContext emptyList()

        // Filter out zero-usage system apps or internal services
        val aggregated = mutableMapOf<String, Long>()
        val lastUsedMap = mutableMapOf<String, Long>()
        val firstUsedMap = mutableMapOf<String, Long>()

        for (stat in rawStats) {
            if (stat.totalTimeInForeground > 1000) { // at least 1 second
                val pkg = stat.packageName
                val existingTime = aggregated[pkg] ?: 0L
                aggregated[pkg] = existingTime + stat.totalTimeInForeground

                val existingLastUsed = lastUsedMap[pkg] ?: 0L
                if (stat.lastTimeUsed > existingLastUsed) {
                    lastUsedMap[pkg] = stat.lastTimeUsed
                }
                val existingFirst = firstUsedMap[pkg] ?: Long.MAX_VALUE
                if (stat.firstTimeStamp < existingFirst) {
                    firstUsedMap[pkg] = stat.firstTimeStamp
                }
            }
        }

        val totalTime = aggregated.values.sum().coerceAtLeast(1L)
        val dateString = dateFormat.format(Date(endTime))

        val entitiesToSave = mutableListOf<UsageEntity>()
        val resultList = mutableListOf<UsageStatItem>()

        for ((pkg, duration) in aggregated) {
            val appName = getAppLabel(pkg)
            val lastUsed = lastUsedMap[pkg] ?: 0L
            val firstUsed = firstUsedMap[pkg] ?: 0L
            val percentage = (duration.toFloat() / totalTime) * 100f

            resultList.add(
                UsageStatItem(
                    packageName = pkg,
                    appName = appName,
                    usageDurationMs = duration,
                    launchCount = 0,
                    lastUsedMs = lastUsed,
                    firstUsedMs = firstUsed,
                    percentageOfTotal = percentage
                )
            )

            entitiesToSave.add(
                UsageEntity(
                    packageName = pkg,
                    appName = appName,
                    date = dateString,
                    usageDuration = duration,
                    lastUsed = lastUsed
                )
            )
        }

        // Cache in Room
        try {
            usageDao.insertAllUsage(entitiesToSave)
        } catch (_: Exception) {}

        resultList.sortedByDescending { it.usageDurationMs }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    fun getCachedUsageForDate(date: String): Flow<List<UsageEntity>> =
        usageDao.getUsageForDate(date)

    fun searchUsage(query: String): Flow<List<UsageEntity>> =
        usageDao.searchUsage(query)

    suspend fun clearUsage() {
        usageDao.clearAllUsage()
    }
}
