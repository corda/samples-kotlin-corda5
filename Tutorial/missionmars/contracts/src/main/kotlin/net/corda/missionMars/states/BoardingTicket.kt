package net.corda.missionMars.states

import com.google.gson.Gson
import net.corda.missionMars.contracts.BoardingTicketContract
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.ContractState
import java.time.LocalDate
import java.util.*

@BelongsToContract(BoardingTicketContract::class)
data class BoardingTicket(
        var description : String, //Brand or type
        var marsExpress : Party, //Origin of the ticket
        var owner: Party, //The person who exchange the basket of apple with the stamp.
        var daysUntilLaunch: Int)
    : ContractState, JsonRepresentable {

    //Secondary Constructor
    constructor(description: String, marsExpress: Party, daysUntilLaunch: Int) : this(
            description = description,
            marsExpress = marsExpress,
            owner = marsExpress,
            daysUntilLaunch = daysUntilLaunch
    )

    fun changeOwner(buyer: Party): BoardingTicket {
        return BoardingTicket(description, marsExpress, buyer, daysUntilLaunch)
    }

    override val participants: List<AbstractParty> get() = listOf<AbstractParty>(marsExpress,owner)

    fun toDto(): BoardingTicketDto {
        return BoardingTicketDto(
                description,
                marsExpress.name.toString(),
                owner.name.toString(),
                daysUntilLaunch.toString()
        )
    }

    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }
}

data class BoardingTicketDto(
        var description : String, //Brand or type
        var marsExpress : String, //Origin of the apple
        var owner: String, //The person who exchange the basket of apple with the stamp.
        var daysUntilLaunch: String
)