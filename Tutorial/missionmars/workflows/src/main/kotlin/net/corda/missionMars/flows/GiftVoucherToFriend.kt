package net.corda.missionMars.flows

import net.corda.missionMars.contracts.BoardingTicketContract
import net.corda.missionMars.contracts.MarsVoucherContract
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
import net.corda.v5.base.util.seconds
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.StateAndRef
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.services.vault.StateStatus
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.util.*
import kotlin.NoSuchElementException

@InitiatingFlow
@StartableByRPC
class GiftVoucherToFriendInitiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<SignedTransactionDigest> {

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
        val inputMarsVoucher = marsVoucherStateAndRef.state.data

        if (inputMarsVoucher.holder != flowIdentity.ourIdentity){
            throw FlowException("Only the voucher current holder can initiate a gifting transaction")
        }

        //Building the output
        val outputMarsVoucher = inputMarsVoucher.changeOwner(recipientParty)

        //Get the Notary from inputRef
        val notary = marsVoucherStateAndRef.state.notary

        //Building the transaction
        val signers = (inputMarsVoucher.participants + recipientParty).map { it.owningKey }
        val txCommand = Command(MarsVoucherContract.Commands.Transfer(), signers)
        val txBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addInputState(marsVoucherStateAndRef)
                .addOutputState(outputMarsVoucher, MarsVoucherContract.ID)
                .addCommand(txCommand)

        // Verify that the transaction is valid.
        txBuilder.verify()

        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Send the state to the counterparty, and receive it back with their signature.
        val sessions = (inputMarsVoucher.participants - flowIdentity.ourIdentity + recipientParty).map { flowMessaging.initiateFlow(it) }.toSet()
        val fullySignedTx = flowEngine.subFlow(
                CollectSignaturesFlow(
                        partSignedTx, sessions,
                )
        )
        // Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(
                FinalityFlow(
                        fullySignedTx, sessions,
                )
        )

        return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { it -> jsonMarshallingService.formatJson(it) },
                notarisedTx.sigs
        )
    }


    }

@InitiatedBy(GiftVoucherToFriendInitiator::class)
class GiftVoucherToFriendResponder(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
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