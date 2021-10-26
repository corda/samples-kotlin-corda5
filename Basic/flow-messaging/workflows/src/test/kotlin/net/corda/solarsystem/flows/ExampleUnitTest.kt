package net.corda.solarsystem.flows

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import java.util.UUID
import net.corda.solarsystem.message.ProbeMessage
import net.corda.solarsystem.messaging.ProbeMessageSendStatus
import net.corda.solarsystem.schema.ProbeSchemaV1
import net.corda.testing.flow.utils.flowTest
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.flows.UntrustworthyData
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.services.json.parseJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ExampleUnitTest {

    @Test
    fun `flow sends message`() {
        flowTest<LaunchProbeFlow> {
            val marsX500 = CordaX500Name.parse("O=Mars, L=FOURTH, C=GB, OU=planet")
            val inputParams = "{\"message\":\"Hey Mars\", \"target\":\"${marsX500}\"}"

            createFlow { LaunchProbeFlow(RpcStartFlowRequestParameters(inputParams)) }

            doReturn(
                mapOf(
                    "message" to "Hey Mars",
                    "target" to "O=Mars, L=FOURTH, C=GB, OU=planet"
                )
            )
                .whenever(flow.jsonMarshallingService)
                .parseJson<Map<String, String>>(inputParams)

            doReturn(otherSide)
                .whenever(flow.identityService)
                .partyFromName(marsX500)

            doReturn(marsX500)
                .whenever(otherSide)
                .name

            doReturn(ourIdentity)
                .whenever(flow.flowIdentity)
                .ourIdentity

            doReturn(otherSideSession)
                .whenever(flow.flowMessaging)
                .initiateFlow(otherSide)

            val probeMessage = argumentCaptor<ProbeMessage>()
            doReturn(UntrustworthyData(ProbeMessageSendStatus.SUCCESS))
                .whenever(otherSideSession)
                .sendAndReceive(eq(ProbeMessageSendStatus::class.java), probeMessage.capture())

            flow.call()

            val createdProbeMessage = probeMessage.firstValue
            assertEquals("Hey Mars", createdProbeMessage.message)
            assertEquals(otherSide, createdProbeMessage.target)
            assertEquals(ourIdentity, createdProbeMessage.launcher)
            assertEquals(setOf(otherSide, ourIdentity), createdProbeMessage.participants.toSet())
            assertNotNull(createdProbeMessage.linearId)
        }
    }

    @Test
    fun `acceptor flow stores message`() {
        flowTest<LaunchProbeFlowAcceptor> {
            val linearId = UUID.randomUUID()
            val probeMessage = ProbeMessage(linearId, "Hey Mars", otherSide, ourIdentity)

            createFlow { LaunchProbeFlowAcceptor(otherSideSession) }

            doReturn(UntrustworthyData(probeMessage))
                .whenever(otherSideSession)
                .receive(ProbeMessage::class.java)

            val persistentProbeCapture = argumentCaptor<ProbeSchemaV1.PersistentProbeMessage>()

            flow.call()

            verify(flow.persistenceService)
                .persist(persistentProbeCapture.capture())

            val createdPersistentProbeMessage = persistentProbeCapture.firstValue
            assertEquals("Hey Mars", createdPersistentProbeMessage.message)
            assertEquals(otherSide.name.toString(), createdPersistentProbeMessage.launcherName)
            assertEquals(ourIdentity.name.toString(), createdPersistentProbeMessage.targetName)
            assertEquals(linearId.toString(), createdPersistentProbeMessage.linearId)
        }
    }
}
