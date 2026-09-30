package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.UsageStatItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class UsagePeriod {
    DAILY,
    WEEKLY,
    MONTHLY
}

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getInstance(context)
    val repository = UsageStatsRepository(context, database)

    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    val selectedCalendar: StateFlow<Calendar> = _selectedCalendar.asStateFlow()

    private val _period = MutableStateFlow(UsagePeriod.DAILY)
    val period: StateFlow<UsagePeriod> = _period.asStateFlow()

    private val _usageItems = MutableStateFlow<List<UsageStatItem>>(emptyList())
    val usageItems: StateFlow<List<UsageStatItem>> = _usageItems.asStateFlow()

    private val _totalScreenTimeMs = MutableStateFlow(0L)
    val totalScreenTimeMs: StateFlow<Long> = _totalScreenTimeMs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(repository.hasUsageAccess())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    init {
        loadUsage()
    }

    fun loadUsage() {
        _hasUsageAccess.value = repository.hasUsageAccess()
        if (!_hasUsageAccess.value) {
            _usageItems.value = emptyList()
            _totalScreenTimeMs.value = 0L
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val items = when (_period.value) {
                UsagePeriod.DAILY -> repository.getUsageStatsForDate(_selectedCalendar.value)
                UsagePeriod.WEEKLY -> repository.getWeeklyUsageStats()
                UsagePeriod.MONTHLY -> repository.getMonthlyUsageStats()
            }
            _usageItems.value = items
            _totalScreenTimeMs.value = items.sumOf { it.usageDurationMs }
            _isLoading.value = false
        }
    }

    fun setPeriod(p: UsagePeriod) {
        _period.value = p
        loadUsage()
    }

    fun nextDay() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, 1)
        _selectedCalendar.value = cal
        loadUsage()
    }

    fun previousDay() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, -1)
        _selectedCalendar.value = cal
        loadUsage()
    }

    fun selectToday() {
        _selectedCalendar.value = Calendar.getInstance()
        loadUsage()
    }

    fun getFormattedSelectedDate(): String {
        return dateFormat.format(_selectedCalendar.value.time)
    }

    fun isTodaySelected(): Boolean {
        val today = Calendar.getInstance()
        val cur = _selectedCalendar.value
        return today.get(Calendar.YEAR) == cur.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == cur.get(Calendar.DAY_OF_YEAR)
    }
}
