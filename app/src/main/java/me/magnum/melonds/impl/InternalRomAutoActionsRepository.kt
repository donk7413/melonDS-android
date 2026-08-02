package me.magnum.melonds.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.magnum.melonds.domain.model.autoaction.RomAutoAction
import me.magnum.melonds.domain.repositories.RomAutoActionsRepository
import me.magnum.melonds.impl.dtos.autoaction.RomAutoActionDto
import java.io.File
import java.io.FileReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type

class InternalRomAutoActionsRepository(private val context: Context, private val gson: Gson) : RomAutoActionsRepository {

    companion object {
        private const val DATA_FILE = "rom_auto_actions.json"
        private const val IMAGES_DIR = "auto_actions"
        private val actionListType: Type = object : TypeToken<List<RomAutoActionDto>>(){}.type
    }

    private val actionsLoadLock = Mutex()
    private var areActionsLoaded = false
    private val actions = MutableStateFlow<List<RomAutoAction>>(emptyList())

    override fun getRomAutoActions(romUri: Uri): Flow<List<RomAutoAction>> {
        return actions
            .onStart { ensureActionsAreLoaded() }
            .map { list -> list.filter { it.romUri == romUri } }
    }

    override suspend fun saveAutoAction(action: RomAutoAction, referenceImage: Bitmap?) {
        ensureActionsAreLoaded()
        actions.update { list ->
            val index = list.indexOfFirst { it.id == action.id }
            list.toMutableList().apply {
                if (index >= 0) {
                    set(index, action)
                } else {
                    add(action)
                }
            }
        }

        if (referenceImage != null) {
            withContext(Dispatchers.IO) {
                try {
                    getReferenceImageFile(action, createDirectories = true)?.outputStream()?.use {
                        referenceImage.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        saveActions()
    }

    override suspend fun deleteAutoAction(action: RomAutoAction) {
        ensureActionsAreLoaded()
        actions.update { list ->
            list.filter { it.id != action.id }
        }
        withContext(Dispatchers.IO) {
            getReferenceImageFile(action, createDirectories = false)?.delete()
        }
        saveActions()
    }

    override suspend fun loadReferenceImagePixels(action: RomAutoAction): IntArray? = withContext(Dispatchers.IO) {
        val imageFile = getReferenceImageFile(action, createDirectories = false)
        if (imageFile == null || !imageFile.isFile) {
            return@withContext null
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath) ?: return@withContext null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()
        pixels
    }

    private suspend fun ensureActionsAreLoaded() = withContext(Dispatchers.IO) {
        actionsLoadLock.withLock {
            if (areActionsLoaded) {
                return@withLock
            }
            actions.value = loadActions()
            areActionsLoaded = true
        }
    }

    private fun loadActions(): List<RomAutoAction> {
        val dataFile = File(context.filesDir, DATA_FILE)
        if (!dataFile.isFile) {
            return emptyList()
        }

        return try {
            gson.fromJson<List<RomAutoActionDto>>(FileReader(dataFile), actionListType)?.map {
                it.toModel()
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveActions() = withContext(Dispatchers.IO) {
        try {
            val actionDtos = actions.value.map { RomAutoActionDto.fromModel(it) }
            OutputStreamWriter(File(context.filesDir, DATA_FILE).outputStream()).use {
                it.write(gson.toJson(actionDtos))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getReferenceImageFile(action: RomAutoAction, createDirectories: Boolean): File? {
        val romDirectory = File(File(context.filesDir, IMAGES_DIR), action.romUri.hashCode().toString())
        if (!romDirectory.isDirectory && createDirectories && !romDirectory.mkdirs()) {
            return null
        }
        return File(romDirectory, "${action.id}.png")
    }
}
