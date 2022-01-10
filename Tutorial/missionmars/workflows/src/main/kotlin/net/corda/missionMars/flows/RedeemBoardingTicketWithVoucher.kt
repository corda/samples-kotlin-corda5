package net.corda.missionMars.flows

import net.corda.missionMars.contracts.BoardingTicketContract
import net.corda.missionMars.contracts.MarsVoucherContract
import net.corda.missionMars.states.BoardingTicket
import net.corda.missionMars.states.MarsVoucher
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.systemflows.SignTransactionFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.identity.CordaX500Name
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.IdentityService
import net.corda.v5.application.services.json.JsonMarshallingService
import net.corda.v5.application.services.json.parseJson
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.services.vault.StateStatus
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.util.*
import kotlin.NoSuchElementException
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.services.vault.IdentityContractStatePostProcessor
import net.corda.v5.ledger.services.vault.SetBasedVaultQueryFilter

@InitiatingFlow
@StartableByRPC
class RedeemBoardingTicketWithVoucherInitiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<SignedTransactionDigest> {

    @CordaInject
    lateinit var flowEngine: FlowEngine
    @CordaInject
    lateinit var flowIdentity: FlowIdentity
    @CordaInject
    lateinit var flowMessaging: FlowMessaging
    @CordaInject
    lateinit var transactionBuilderFactory: TransactionBuilderFactory
    @CordaInject
    lateinit var identityService: IdentityService
    @CordaInject
    lateinit var notaryLookup: NotaryLookupService
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService
    @CordaInject
    lateinit var persistenceService: PersistenceService


    @Suspendable
    override fun call(): SignedTransactionDigest {

        // parse parameters
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)
        val voucherID = with(mapOfParams["voucherID"] ?: throw BadRpcStartFlowRequestException("MarsVoucher State Parameter \"voucherID\" missing.")) {
            this
        }
        val holder = with(mapOfParams["holder"] ?: throw BadRpcStartFlowRequestException("BoardingTicket State Parameter \"holder\" missing.")) {
            CordaX500Name.parse(this)
        }
        val recipientParty = identityService.partyFromName(holder) ?: throw NoSuchElementException("No party found for X500 name $holder")

        //Query the MarsVoucher & the boardingTicket
        val cursor = persistenceService.query<StateAndRef<MarsVoucher>>(
                "LinearState.findByUuidAndStateStatus",
                mapOf(
                        "uuid" to UUID.fromString(voucherID),
                        "stateStatus" to StateStatus.UNCONSUMED
                ),
                "Corda.IdentityStateAndRefPostProcessor"
        )
        val marsVoucherStateAndRef = cursor.poll(100, 20.seconds).values.first()

        val cursor2 = persistenceService.query<StateAndRef<BoardingTicket>>(
                queryName = "VaultState.findByStateStatus",
                namedParameters = mapOf("stateStatus" to StateStatus.UNCONSUMED),
                postFilter = SetBasedVaultQueryFilter.Builder()
                        .withContractStateClassNames(setOf(BoardingTicket::class.java.name))
                        .build(),
                postProcessorName = "Corda.IdentityStateAndRefPostProcessor"
        )
        val boardingTicketStateAndRef= cursor2.poll(100, 20.seconds).values.last()
        val originalBoardingTicketState= boardingTicketStateAndRef.state.data

        //Building the output
        val outputBoardingTicket = originalBoardingTicketState.changeOwner(recipientParty)

        //Find notary
        val notary = marsVoucherStateAndRef.state.notary

        //Building the transaction
        val txCommand = Command(BoardingTicketContract.Commands.RedeemTicket(), listOf(flowIdentity.ourIdentity.owningKey,recipientParty.owningKey))
        val txBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addInputState(marsVoucherStateAndRef)
                .addInputState(boardingTicketStateAndRef)
                .addOutputState(outputBoardingTicket, BoardingTicketContract.ID)
                .addCommand(txCommand)

        // Verify that the transaction is valid.
        txBuilder.verify()

        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = flowMessaging.initiateFlow(recipientParty)
        val fullySignedTx = flowEngine.subFlow(
                CollectSignaturesFlow(
                        partSignedTx, setOf(otherPartySession),
                )
        )

        // Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(
                FinalityFlow(
                        fullySignedTx, setOf(otherPartySession),
                )
        )

        return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { it -> jsonMarshallingService.formatJson(it) },
                notarisedTx.sigs
        )
    }
}


@InitiatedBy(RedeemBoardingTicketWithVoucherInitiator::class)
class RedeemBoardingTicketWithVoucherResponder(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {
            override fun checkTransaction(stx: SignedTransaction) {

            }
        }
        val txId = flowEngine.subFlow(signTransactionFlow).id
        return flowEngine.subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))
    }
}