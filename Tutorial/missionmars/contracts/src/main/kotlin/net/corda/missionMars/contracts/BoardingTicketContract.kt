package net.corda.missionMars.contracts

import net.corda.missionMars.states.BoardingTicket
import net.corda.missionMars.states.MarsVoucher
import net.corda.v5.ledger.contracts.CommandData
import net.corda.v5.ledger.contracts.Contract
import net.corda.v5.ledger.contracts.requireThat
import net.corda.v5.ledger.transactions.LedgerTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class BoardingTicketContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        //Extract the command from the transaction.
        val commandData = tx.commands[0].value
        val output = tx.outputsOfType(BoardingTicket::class.java)[0]

        when (commandData) {
            is Commands.CreateTicket -> requireThat {
                "This transaction should only output one BoardingTicket state".using(tx.outputs.size == 1)
                "The output BoardingTicket state should have clear description of space trip information".using(output.description != "")
                "The output BoardingTicket state should have a launching date later then the creation time".using(output.daysUntilLaunch > 0)
                null
            }
            is Commands.RedeemTicket -> requireThat {
                val input = tx.inputsOfType(MarsVoucher::class.java)[0]
                "This transaction should consume two states".using(tx.inputStates.size == 2)
                "The issuer of the BoardingTicket should be the space company which creates the boarding ticket".using(input.issuer == output.marsExpress)
                "The output BoardingTicket state should have a launching date later then the creation time".using(output.daysUntilLaunch > 0)
                null
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class CreateTicket : Commands
        class RedeemTicket : Commands
    }

    companion object {
        // This is used to identify our contract when building a transaction.
        const val ID = "net.corda.missionMars.contracts.BoardingTicketContract"
    }
}