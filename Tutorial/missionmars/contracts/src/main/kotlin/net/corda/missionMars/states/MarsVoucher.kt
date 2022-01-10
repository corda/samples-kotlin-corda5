package net.corda.missionMars.states

import com.google.gson.Gson
import net.corda.missionMars.contracts.MarsVoucherContract
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState

@BelongsToContract(MarsVoucherContract::class)
data class MarsVoucher (
        val voucherDesc : String,//For example: "One stamp can exchange for a basket of HoneyCrispy Apple"
        val issuer: Party, //The person who issued the stamp
        val holder: Party, //The person who currently owns the stamp
        override val linearId: UniqueIdentifier,//LinearState required variable.
) : LinearState, JsonRepresentable{

    override val participants: List<AbstractParty> get() = listOf<AbstractParty>(issuer,holder)

    fun toDto(): MarsVoucherDto {
        return MarsVoucherDto(
                voucherDesc,
                issuer.name.toString(),
                holder.name.toString(),
                linearId.toString()
        )
    }

    fun changeOwner(newOwner: Party): MarsVoucher {
        return MarsVoucher(voucherDesc, issuer, newOwner, linearId)
    }


    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }
}

data class MarsVoucherDto(
        val voucherDesc : String,//For example: "One stamp can exchange for a basket of HoneyCrispy Apple"
        val issuer: String, //The person who issued the stamp
        val holder: String, //The person who currently owns the stamp
        val linearId: String,//LinearState required variable.
)