package me.magnum.melonds.ui.layouts.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.magnum.melonds.domain.model.layout.LayoutConfiguration
import me.magnum.melonds.domain.repositories.LayoutsRepository
import me.magnum.melonds.ui.layouts.model.SelectedLayout
import java.util.UUID

abstract class BaseLayoutsViewModel(protected val layoutsRepository: LayoutsRepository) : ViewModel() {

    protected val _layouts = MutableStateFlow<List<LayoutConfiguration>?>(null)
    val layouts = _layouts.asStateFlow()

    abstract val selectedLayoutId: StateFlow<SelectedLayout>

    fun addLayout(layout: LayoutConfiguration) {
        viewModelScope.launch {
            layoutsRepository.saveLayout(layout)
        }
    }

    fun deleteLayout(layout: LayoutConfiguration) {
        viewModelScope.launch {
            if (layout.id == selectedLayoutId.value) {
                applyFallbackLayout()
            }
            layoutsRepository.deleteLayout(layout)
        }
    }

    suspend fun exportLayout(layout: LayoutConfiguration, uri: Uri): Boolean {
        return layoutsRepository.exportLayout(layout, uri)
    }

    suspend fun importLayout(uri: Uri): Boolean {
        return layoutsRepository.importLayout(uri) != null
    }

    abstract fun setSelectedLayoutId(id: UUID?)

    protected abstract fun applyFallbackLayout()
}