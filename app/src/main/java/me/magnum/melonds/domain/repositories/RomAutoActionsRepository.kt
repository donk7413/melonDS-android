package me.magnum.melonds.domain.repositories

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import me.magnum.melonds.domain.model.autoaction.RomAutoAction

interface RomAutoActionsRepository {
    fun getRomAutoActions(romUri: Uri): Flow<List<RomAutoAction>>
    suspend fun saveAutoAction(action: RomAutoAction, referenceImage: Bitmap?)
    suspend fun deleteAutoAction(action: RomAutoAction)
    suspend fun loadReferenceImagePixels(action: RomAutoAction): IntArray?
}
