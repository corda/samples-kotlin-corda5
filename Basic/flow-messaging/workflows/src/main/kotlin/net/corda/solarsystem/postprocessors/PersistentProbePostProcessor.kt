package net.corda.solarsystem.postprocessors

import java.util.stream.Stream
import net.corda.solarsystem.message.ProbeMessageDto
import net.corda.solarsystem.schema.ProbeSchemaV1
import net.corda.v5.application.services.persistence.CustomQueryPostProcessor

class PersistentProbePostProcessor : CustomQueryPostProcessor<ProbeMessageDto> {
    override val name: String
        get() = "PersistentProbePostProcessor"

    override fun postProcess(inputs: Stream<Any?>): Stream<ProbeMessageDto> {
        return inputs
            .filter { it is ProbeSchemaV1.PersistentProbeMessage }
            .map {
                it as ProbeSchemaV1.PersistentProbeMessage
                ProbeMessageDto(it.linearId, it.message, it.launcherName, it.targetName)
            }
    }
}