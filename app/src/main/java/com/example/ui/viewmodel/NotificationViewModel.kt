package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityStorage
import com.example.data.local.entity.NotificationEntity
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val securityStorage = SecurityStorage.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    val repository = NotificationRepository(context, database, securityStorage)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedApp = MutableStateFlow<String?>(null)
    val selectedApp: StateFlow<String?> = _selectedApp.asStateFlow()

    private val _sortNewestFirst = MutableStateFlow(true)
    val sortNewestFirst: StateFlow<Boolean> = _sortNewestFirst.asStateFlow()

    private val _isCollectionPaused = MutableStateFlow(repository.isCollectionPaused())
    val isCollectionPaused: StateFlow<Boolean> = _isCollectionPaused.asStateFlow()

    val availableApps: StateFlow<List<String>> = repository.getDistinctApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNotifications: StateFlow<List<NotificationEntity>> = combine(
        repository.getAllNotifications(),
        _searchQuery,
        _selectedApp,
        _sortNewestFirst
    ) { list, query, appFilter, newestFirst ->
        var result = list

        if (query.isNotBlank()) {
            result = result.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.title.contains(query, ignoreCase = true) ||
                        it.text.contains(query, ignoreCase = true)
            }
        }

        if (appFilter != null) {
            result = result.filter { it.appName.equals(appFilter, ignoreCase = true) }
        }

        if (newestFirst) {
            result.sortedByDescending { it.timestamp }
        } else {
            result.sortedBy { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAppFilter(app: String?) {
        _selectedApp.value = if (_selectedApp.value == app) null else app
    }

    fun toggleSort() {
        _sortNewestFirst.value = !_sortNewestFirst.value
    }

    fun togglePause() {
        val newState = !_isCollectionPaused.value
        repository.setCollectionPaused(newState)
        _isCollectionPaused.value = newState
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllNotifications()
        }
    }

    fun isNotificationAccessGranted(): Boolean = repository.isNotificationAccessGranted()
}
