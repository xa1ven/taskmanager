package com.example.todolist.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.data.repository.AuthRepository
import com.example.todolist.data.repository.SettingsRepository
import com.example.todolist.data.repository.ThemeRepository
import com.example.todolist.data.repository.TokenRepository
import com.example.todolist.ui.notifications.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = false,
    val fontScaleIndex: Int = 1,
    val accentScheme: Int = 0,
    val morningNotifEnabled: Boolean = true,
    val eveningNotifEnabled: Boolean = true,
    val login: String = "",
    val isChangingPassword: Boolean = false,
    val passwordChangeError: String? = null,
    val passwordChangeSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(login = tokenRepository.getLogin()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.isDarkTheme.collect { dark ->
                _uiState.update { it.copy(isDarkTheme = dark) }
            }
        }
        viewModelScope.launch {
            settingsRepository.fontScaleIndex.collect { font ->
                _uiState.update { it.copy(fontScaleIndex = font) }
            }
        }
        viewModelScope.launch {
            settingsRepository.accentScheme.collect { accent ->
                _uiState.update { it.copy(accentScheme = accent) }
            }
        }
        viewModelScope.launch {
            settingsRepository.morningNotifEnabled.collect { enabled ->
                _uiState.update { it.copy(morningNotifEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.eveningNotifEnabled.collect { enabled ->
                _uiState.update { it.copy(eveningNotifEnabled = enabled) }
            }
        }
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            themeRepository.setDarkTheme(!_uiState.value.isDarkTheme)
        }
    }

    fun setFontScaleIndex(index: Int) {
        viewModelScope.launch {
            settingsRepository.setFontScaleIndex(index)
        }
    }

    fun setAccentScheme(scheme: Int) {
        viewModelScope.launch {
            settingsRepository.setAccentScheme(scheme)
        }
    }

    fun setMorningNotif(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMorningNotif(enabled)
            if (enabled) NotificationScheduler.scheduleMorningNotification(context)
            else NotificationScheduler.cancelMorningNotification(context)
        }
    }

    fun setEveningNotif(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEveningNotif(enabled)
            if (enabled) NotificationScheduler.scheduleEveningNotification(context)
            else NotificationScheduler.cancelEveningNotification(context)
        }
    }

    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, passwordChangeError = null) }
            val result = authRepository.changePassword(current, new)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isChangingPassword = false, passwordChangeSuccess = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isChangingPassword = false, passwordChangeError = e.message) }
                }
            )
        }
    }

    fun clearPasswordChangeResult() {
        _uiState.update { it.copy(passwordChangeError = null, passwordChangeSuccess = false) }
    }

    fun logout() {
        tokenRepository.clearToken()
    }
}
