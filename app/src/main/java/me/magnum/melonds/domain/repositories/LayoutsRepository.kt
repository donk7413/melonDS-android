package me.magnum.melonds.domain.repositories

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import me.magnum.melonds.domain.model.layout.LayoutConfiguration
import java.util.UUID

interface LayoutsRepository {
    fun getLayouts(): Flow<List<LayoutConfiguration>>
    suspend fun getLayout(id: UUID): LayoutConfiguration?
    suspend fun deleteLayout(layout: LayoutConfiguration)
    fun getGlobalLayoutPlaceholder(): LayoutConfiguration
    fun observeLayout(id: UUID): Flow<LayoutConfiguration>
    suspend fun saveLayout(layout: LayoutConfiguration)
    suspend fun exportLayout(layout: LayoutConfiguration, uri: Uri): Boolean
    suspend fun importLayout(uri: Uri): LayoutConfiguration?
}