package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.repository.AiInsightsRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.AiInsight
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AiInsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val securityStorage = SecurityStorage.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    private val usageStatsRepo = UsageStatsRepository(context, database)
    private val notificationRepo = NotificationRepository(context, database, securityStorage)
    val repository = AiInsightsRepository(securityStorage, usageStatsRepo, notificationRepo)

    private val _insights = MutableStateFlow<List<AiInsight>>(emptyList())
    val insights: StateFlow<List<AiInsight>> = _insights.asStateFlow()

    private val _isAiEnabled = MutableStateFlow(repository.isAiAnalysisEnabled())
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        generateInsights()
    }

    fun generateInsights() {
        if (!_isAiEnabled.value) {
            _insights.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val usage = usageStatsRepo.getUsageStatsForDate(Calendar.getInstance())
                val notifications = notificationRepo.getRecentNotifications(100).first()
                val list = repository.generateInsights(usage, notifications)
                _insights.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAiEnabled() {
        val newState = !_isAiEnabled.value
        repository.setAiAnalysisEnabled(newState)
        _isAiEnabled.value = newState
        if (newState) {
            generateInsights()
        } else {
            _insights.value = emptyList()
        }
    }
}
