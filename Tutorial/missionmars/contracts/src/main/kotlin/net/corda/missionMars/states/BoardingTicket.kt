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
import kotlin.time.hours

@BelongsToContract(BoardingTicketContract::class)
data class BoardingTicket(
        var description : String, //Brand or type
        var marsExpress : Party, //Origin of the ticket
        var owner: Party, //The person who owns the space trip ticket.
        var launchDate: LocalDate)
    : ContractState, JsonRepresentable {

    //Secondary Constructor
    constructor(description: String, marsExpress: Party, launchDate: LocalDate) : this(
            description = description,
            marsExpress = marsExpress,
            owner = marsExpress,
            launchDate = launchDate
    )

    fun changeOwner(buyer: Party): BoardingTicket {
        return BoardingTicket(description, marsExpress, buyer, launchDate)
    }

    override val participants: List<AbstractParty> get() = listOf<AbstractParty>(marsExpress,owner)

    fun toDto(): BoardingTicketDto {



        return BoardingTicketDto(
                description,
                marsExpress.name.toString(),
                owner.name.toString(),
                launchDate.toString()
        )
    }

    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }
}

data class BoardingTicketDto(
        var description : String,
        var marsExpress : String, 
        var owner: String, 
        var launchDate: String
)
