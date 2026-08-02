package me.magnum.melonds.impl.dtos.autoaction

import androidx.core.net.toUri
import com.google.gson.annotations.SerializedName
import me.magnum.melonds.domain.model.Rect
import me.magnum.melonds.domain.model.autoaction.AutoActionStep
import me.magnum.melonds.domain.model.autoaction.AutoActionTrigger
import me.magnum.melonds.domain.model.autoaction.RomAutoAction
import me.magnum.melonds.utils.enumValueOfIgnoreCase
import java.util.UUID

data class RomAutoActionDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("romUri")
    val romUri: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("regionX")
    val regionX: Int,
    @SerializedName("regionY")
    val regionY: Int,
    @SerializedName("regionWidth")
    val regionWidth: Int,
    @SerializedName("regionHeight")
    val regionHeight: Int,
    @SerializedName("similarityThreshold")
    val similarityThreshold: Int,
    @SerializedName("repeatWhileVisible")
    val repeatWhileVisible: Boolean,
    @SerializedName("steps")
    val steps: List<AutoActionStepDto>,
    @SerializedName("triggers")
    val triggers: List<AutoActionTriggerDto>?,
) {

    companion object {
        fun fromModel(action: RomAutoAction): RomAutoActionDto {
            return RomAutoActionDto(
                action.id.toString(),
                action.romUri.toString(),
                action.name,
                action.enabled,
                action.region.x,
                action.region.y,
                action.region.width,
                action.region.height,
                action.similarityThreshold,
                action.repeatWhileVisible,
                action.steps.map { AutoActionStepDto(it.input.name, it.pressDurationMs, it.delayAfterMs) },
                action.triggers.map { AutoActionTriggerDto(it.referencedActionId.toString(), it.mode.name) },
            )
        }
    }

    fun toModel(): RomAutoAction {
        return RomAutoAction(
            UUID.fromString(id),
            romUri.toUri(),
            name,
            enabled,
            Rect(regionX, regionY, regionWidth, regionHeight),
            similarityThreshold,
            repeatWhileVisible,
            steps.map { AutoActionStep(enumValueOfIgnoreCase(it.input), it.pressDurationMs, it.delayAfterMs) },
            triggers?.map { AutoActionTrigger(UUID.fromString(it.referencedActionId), enumValueOfIgnoreCase(it.mode)) } ?: emptyList(),
        )
    }

    data class AutoActionStepDto(
        @SerializedName("input")
        val input: String,
        @SerializedName("pressDurationMs")
        val pressDurationMs: Long,
        @SerializedName("delayAfterMs")
        val delayAfterMs: Long,
    )

    data class AutoActionTriggerDto(
        @SerializedName("referencedActionId")
        val referencedActionId: String,
        @SerializedName("mode")
        val mode: String,
    )
}
