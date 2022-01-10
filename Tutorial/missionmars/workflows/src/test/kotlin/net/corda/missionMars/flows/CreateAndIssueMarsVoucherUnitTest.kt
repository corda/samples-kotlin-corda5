package net.corda.missionMars.flows

import com.nhaarman.mockito_kotlin.*
import net.corda.missionMars.contracts.MarsVoucherContract
import net.corda.missionMars.states.MarsVoucher
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.testing.flow.utils.flowTest
import net.corda.v5.application.flows.RpcStartFlowRequestParameters
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.CommandData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.Test

class CreateAndIssueMarsVoucherUnitTest {

    @Test
    fun `flow signs state`() {
        flowTest<CreateAndIssueMarsVoucherInitiator> {

            // NOTE: this probably should be set up in flowTest
            val mockNode = CordaX500Name.parse("O=Mars Express, L=London, C=GB, OU=Space Company")

            val inputParams = "{\"voucherDesc\": \"Space Shuttle 323\", \"holder\": \"${mockNode}\"}"
            createFlow { CreateAndIssueMarsVoucherInitiator(RpcStartFlowRequestParameters(inputParams)) }

            doReturn(notary)
                    .whenever(flow.notaryLookup)
                    .getNotary(CordaX500Name.parse("O=notary, L=London, C=GB"))

            doReturn(mockNode)
                .whenever(otherSide)
                .name

            doReturn(otherSide)
                .whenever(flow.identityService)
                .partyFromName(mockNode)

            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<CollectSignaturesFlow>())

            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<FinalityFlow>())

            doReturn(
                mapOf(
                    "voucherDesc" to "Space Shuttle 323",
                    "holder" to otherSide.name.toString()
                )
            )
                .whenever(flow.jsonMarshallingService)
                .parseJson<Map<String, String>>(inputParams)

            flow.call()

            // verify notary is set
            verify(transactionBuilderMock).setNotary(notary)

            // verify the correct output state is created
            argumentCaptor<MarsVoucher>().apply {
                verify(transactionBuilderMock).addOutputState(capture(), eq(MarsVoucherContract.ID))
                assertSoftly {
                    it.assertThat(firstValue.issuer).isEqualTo(ourIdentity)
                    it.assertThat(firstValue.holder).isEqualTo(otherSide)
                    it.assertThat(firstValue.voucherDesc).isEqualTo("Space Shuttle 323")
                }
            }

            // verify command is added
            argumentCaptor<Command<CommandData>>().apply {
                verify(transactionBuilderMock).addCommand(capture())
                assertThat(firstValue.value).isInstanceOf(MarsVoucherContract.Commands.Issue::class.java)
                assertThat(firstValue.signers).contains(ourIdentity.owningKey)
                assertThat(firstValue.signers).contains(otherSide.owningKey)
            }
        }
    }
}
