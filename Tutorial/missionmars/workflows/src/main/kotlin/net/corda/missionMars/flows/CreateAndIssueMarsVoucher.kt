package net.corda.missionMars.flows

import net.corda.missionMars.contracts.MarsVoucherContract
import net.corda.missionMars.states.MarsVoucher
import net.corda.missionMars.states.TemplateState
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
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.SignedTransactionDigest
import net.corda.v5.ledger.transactions.TransactionBuilderFactory

@InitiatingFlow
@StartableByRPC
class CreateAndIssueMarsVoucherInitiator @JsonConstructor constructor(private val params: RpcStartFlowRequestParameters) : Flow<SignedTransactionDigest> {

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

    @Suspendable
    override fun call(): SignedTransactionDigest {

        // parse parameters
        val mapOfParams: Map<String, String> = jsonMarshallingService.parseJson(params.parametersInJson)
        val voucherDesc = with(mapOfParams["voucherDesc"] ?: throw BadRpcStartFlowRequestException("MarsVoucher State Parameter \"voucherDesc\" missing.")) {
            this
        }
        val target = with(mapOfParams["holder"] ?: throw BadRpcStartFlowRequestException("MarsVoucher State Parameter \"holder\" missing.")) {
            CordaX500Name.parse(this)
        }
        val recipientParty = identityService.partyFromName(target) ?: throw NoSuchElementException("No party found for X500 name $target")

        //Find the notary
        val notary = notaryLookup.notaryIdentities.first()

        //Building the output MarsVoucher state
        val uniqueID = UniqueIdentifier()
        val newVoucher = MarsVoucher(voucherDesc, flowIdentity.ourIdentity, recipientParty, uniqueID)

        //Build transaction
        val txCommand = Command(MarsVoucherContract.Commands.Issue(), listOf(flowIdentity.ourIdentity.owningKey,recipientParty.owningKey))
        val txBuilder = transactionBuilderFactory.create()
                .setNotary(notary)
                .addOutputState(newVoucher, MarsVoucherContract.ID)
                .addCommand(txCommand)

        // Verify that the transaction is valid.
        txBuilder.verify()

        // Sign the transaction.
        val partSignedTx = txBuilder.sign()

        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = flowMessaging.initiateFlow(recipientParty)
        val fullySignedTx = flowEngine.subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession)))

        // Notarise and record the transaction in both parties' vaults.
        val notarisedTx = flowEngine.subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession)))

        return SignedTransactionDigest(
                notarisedTx.id,
                notarisedTx.tx.outputStates.map { output -> jsonMarshallingService.formatJson(output) },
                notarisedTx.sigs
        )
    }
}


@InitiatedBy(CreateAndIssueMarsVoucherInitiator::class)
class CreateAndIssueMarsVoucherResponder(val otherPartySession: FlowSession) : Flow<SignedTransaction> {
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