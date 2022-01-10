package net.corda.missionMars.flows

import com.nhaarman.mockito_kotlin.*
import net.corda.missionMars.contracts.BoardingTicketContract
import net.corda.missionMars.states.BoardingTicket
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.testing.flow.utils.flowTest
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.CommandData
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test

class CreateBoardingTicketUnitTest {

//    @Test
//    fun `flow signs state`() {
//        flowTest<CreateBoardingTicketInitiator> {
//
//            // NOTE: this probably should be set up in flowTest
//            val mockNode = CordaX500Name.parse("O=Mars Express, L=London, C=GB, OU=Space Company")
//
//            val inputParams = "{\"ticketDescription\": \"Space Shuttle 323 - Seat 16B\", \"daysUntilLaunch\": \"10\"}"
//            createFlow { CreateBoardingTicketInitiator(RpcStartFlowRequestParameters(inputParams)) }
//
//            doReturn(mockNode)
//                    .whenever(otherSide)
//                    .name
//
//            doReturn(signedTransactionMock)
//                    .whenever(flow.flowEngine)
//                    .subFlow(any<CollectSignaturesFlow>())
//
//            doReturn(signedTransactionMock)
//                    .whenever(flow.flowEngine)
//                    .subFlow(any<FinalityFlow>())
//
//            doReturn(
//                    mapOf(
//                            "ticketDescription" to "Space Shuttle 323 - Seat 16B",
//                            "daysUntilLaunch" to "10"
//                    )
//            )
//                    .whenever(flow.jsonMarshallingService)
//                    .parseJson<Map<String, String>>(inputParams)
//
//            flow.call()
//
//            // verify notary is set
//            verify(transactionBuilderMock).setNotary(notary)
//
//            // verify the correct output state is created
//            argumentCaptor<BoardingTicket>().apply {
//                verify(transactionBuilderMock).addOutputState(capture(), eq(BoardingTicketContract.ID))
//                SoftAssertions.assertSoftly {
//                    it.assertThat(firstValue.owner).isEqualTo(ourIdentity)
//                    it.assertThat(firstValue.marsExpress).isEqualTo(ourIdentity)
//                    it.assertThat(firstValue.description).isEqualTo("Space Shuttle 323 - Seat 16B")
//                    it.assertThat(firstValue.daysUntilLaunch).isEqualTo(10)
//
//                }
//            }
//
//            // verify command is added
//            argumentCaptor<Command<CommandData>>().apply {
//                verify(transactionBuilderMock).addCommand(capture())
//                Assertions.assertThat(firstValue.value).isInstanceOf(BoardingTicketContract.Commands.CreateTicket::class.java)
//                Assertions.assertThat(firstValue.signers).contains(ourIdentity.owningKey)
//            }
//        }
//    }
}