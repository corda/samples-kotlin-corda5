package net.corda.solarsystem.flows

import net.corda.solarsystem.message.ProbeMessageDto
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.seconds

/**
 * This flow lists all ProbeMessages that a party has in its persistent data store.
 */
@InitiatingFlow
@StartableByRPC
class ListProbeMessagesFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<List<ProbeMessageDto>> {
    @CordaInject
    lateinit var persistenceService: PersistenceService

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    override fun call(): List<ProbeMessageDto> {
        // Query for all persistent probe messages, and use the PersistentProbePostProcessor to convert the entities to serializable DTOs.
        val cursor = persistenceService.query<ProbeMessageDto>(
            "ProbeSchemaV1.PersistentProbeMessage.FindAll",
            mapOf(),
            "PersistentProbePostProcessor"
        )

        // poll for all ProbeMessages in batches of 100 with a 10 second timeout.
        val accumulator = mutableListOf<ProbeMessageDto>()
        do {
            val poll = cursor.poll(100, 10.seconds)
            accumulator.addAll(poll.values)
        } while (!poll.isLastResult)

        return accumulator;
    }
}


