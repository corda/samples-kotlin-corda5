package net.corda.solarsystem.flows

import java.util.UUID
import net.corda.solarsystem.message.ProbeMessage
import net.corda.solarsystem.messaging.ProbeMessageSendStatus
import net.corda.solarsystem.schema.ProbeSchemaV1
import net.corda.v5.application.flows.BadRpcStartFlowRequestException
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowSession
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.JsonConstructor
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.StartableByRPC
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.flows.unwrap
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.util.contextLogger

/**
 * This flow allows two parties (the [Launcher] and the [Target]) to say hello to one another via the [ProbeMessage].
 *
 * In our simple example, the [Acceptor] will only accepts a valid Probe.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice, we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
@InitiatingFlow
@StartableByRPC
class LaunchProbeFlow @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<ProbeMessage> {
    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var identityService: IdentityService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    private companion object {
        private val logger = contextLogger()
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    override fun call(): ProbeMessage {
        // parse parameters
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)

        val message =
            with(mapOfParams["message"] ?: throw BadRpcStartFlowRequestException("Parameter \"message\" missing.")) {
                this
            }

        val target =
            with(mapOfParams["target"] ?: throw BadRpcStartFlowRequestException("Parameter \"target\" missing.")) {
                CordaX500Name.parse(this)
            }
        val recipientParty = identityService.partyFromName(target)
            ?: throw NoSuchElementException("No party found for X500 name $target")

        // Stage 1.
        // Generate a message.
        val probeMessage = ProbeMessage(UUID.randomUUID(), message, flowIdentity.ourIdentity, recipientParty)

        // Stage 2.
        // Initiate a communication session with the counterparty.
        val flowSession = flowMessaging.initiateFlow(recipientParty)

        logger.info("Sending probe message '${probeMessage.message}' to '${probeMessage.target.name}'")

        // Stage 3.
        // Send the probe message to the counterparty and await result.
        val untrustworthyResponse = flowSession.sendAndReceive(ProbeMessageSendStatus::class.java, probeMessage)

        val success = untrustworthyResponse.unwrap {
            it == ProbeMessageSendStatus.SUCCESS
        }
        if(!success) throw CordaRuntimeException("Response from acceptor was not a success.")

        logger.info("Sent probe message with ID '${probeMessage.linearId}' and received success response.")

        return probeMessage;
    }
}

@InitiatedBy(LaunchProbeFlow::class)
class LaunchProbeFlowAcceptor(val otherPartySession: FlowSession) : Flow<Unit> {
    @CordaInject
    lateinit var flowIdentity: FlowIdentity

    @CordaInject
    lateinit var persistenceService: PersistenceService

    private companion object {
        private val logger = contextLogger()
    }

    @Suspendable
    override fun call() {
        val untrustworthyData = otherPartySession.receive(ProbeMessage::class.java)

        logger.info("Received probe message, attempting to unwrap it.")

        // Request may not be trustworthy, make some assertions on the request that should be true for valid ProbeMessages.
        val probeMessage = try {
            untrustworthyData.unwrap {
                if (it.message.isEmpty()) throw CordaRuntimeException("FAILURE - request from counterparty did not contain a message.")
                if (it.target != flowIdentity.ourIdentity) throw CordaRuntimeException("FAILURE - request from counterparty did not have our identity as the target.")
                if (it.participants.size != 2) throw CordaRuntimeException("FAILURE - request from counterparty had more participants than allowed.")
                if (!it.participants.contains(otherPartySession.counterparty)) throw CordaRuntimeException("FAILURE - request from counterparty participants did not contain the counterparty.")
                it
            }
        } catch (e: CordaRuntimeException){
            logger.error("Exception during unwrapping of untrustworthy data from counterparty.", e)
            // Send the success to the other party.
            otherPartySession.send(ProbeMessageSendStatus.FAILURE)
            return
        }

        logger.info("Probe message '${probeMessage.message}' from sender '${probeMessage.launcher.name}' successfully unwrapped.")

        // Record the message in our data store.
        persistenceService.persist(createPersistentProbeMessage(probeMessage))

        logger.info("Probe message with ID '${probeMessage.linearId}' persisted into data storage.")

        // Send the success to the other party.
        otherPartySession.send(ProbeMessageSendStatus.SUCCESS)
    }

    private fun createPersistentProbeMessage(probeMessage: ProbeMessage): ProbeSchemaV1.PersistentProbeMessage {
        return ProbeSchemaV1.PersistentProbeMessage(
            probeMessage.linearId.toString(),
            probeMessage.message,
            probeMessage.launcher.name.toString(),
            probeMessage.target.name.toString()
        )
    }
}


