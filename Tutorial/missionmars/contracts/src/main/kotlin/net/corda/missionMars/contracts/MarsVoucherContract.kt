package net.corda.missionMars.contracts

import net.corda.missionMars.states.MarsVoucher
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction


//Domain Specific Language
class MarsVoucherContract : Contract {

    override fun verify(tx: LedgerTransaction) {

        //Extract the command from the transaction.
        val commandData = tx.commands[0].value

        //Verify the transaction according to the intention of the transaction
        when (commandData) {
            is Commands.Issue -> requireThat {
                val output = tx.outputsOfType(MarsVoucher::class.java)[0]
                "This transaction should only have one MarsVoucher state as output".using(tx.outputs.size == 1)
                "The output MarsVoucher state should have clear description of the type of Space trip information".using(output.voucherDesc != "")
                null
            }
            is BoardingTicketContract.Commands.RedeemTicket-> requireThat {
                //Transaction verification will happen in BoardingTicket Contract
            }
            is Commands.Transfer -> requireThat {
                "This transaction should consume one Marsvoucher states".using(tx.inputStates.size == 1)
                val input = tx.inputsOfType(MarsVoucher::class.java)[0]
                val output = tx.outputsOfType(MarsVoucher::class.java)[0]
                "You cannot gift the voucher to yourself".using(input.holder != output.holder)
                null
            }
        }
    }





    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        //In our hello-world app, We will have two commands.
        class Issue : Commands
        class Transfer: Commands
    }

    companion object {
        // This is used to identify our contract when building a transaction.
        @JvmStatic
        val ID = "net.corda.missionMars.contracts.MarsVoucherContract"
    }
}