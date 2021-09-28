package net.corda.missionMars.flows

import net.corda.missionMars.contracts.BoardingTicketContract
import net.corda.missionMars.contracts.MarsVoucherContract
import net.corda.missionMars.states.BoardingTicket
import net.corda.systemflows.FinalityFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@InitiatingFlow
@StartableByRPC
data class CreateBoardingTicketInitiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService
    @CordaInject
    lateinit var flowEngine: FlowEngine
    @CordaInject
    lateinit var flowIdentity: FlowIdentity
    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory
    @CordaInject
    lateinit var notaryLookup: NotaryLookupService

    @Suspendable
    override fun call(): SignedTransactionDigest {

        // parse parameters
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)
        val ticketDescription = with(mapOfParams["ticketDescription"] ?: throw BadRpcStartFlowRequestException("BoardingTicket State Parameter \"ticketDescription\" missing.")) {
            this
        }
        val daysTillLaunch = with(mapOfParams["daysTillLaunch"] ?: throw BadRpcStartFlowRequestException("BoardingTicket State Parameter \"daysTillLaunch\" missing.")) {
            this.toInt()
        }

        //Find notary
        val notary = notaryLookup.notaryIdentities.first()

        //Building the output BoardingTicket state
        val basket = BoardingTicket(description = ticketDescription,marsExpress = flowIdentity.ourIdentity,daysTillLaunch = daysTillLaunch)


        //building the Transaction
        val txCommand = Command(BoardingTicketContract.Commands.CreateTicket(), listOf(flowIdentity.ourIdentity.owningKey))
        val txBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addOutputState(basket, BoardingTicketContract.ID)
                .addCommand(txCommand)

        // Verify that the transaction is valid.
        txBuilder.verify()

        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(FinalityFlow(partSignedTx, setOf()))

        return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
                notarisedTx.sigs
        )
    }
}