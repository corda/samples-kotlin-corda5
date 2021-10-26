package net.corda.solarsystem.message

import com.google.gson.Gson
import java.util.UUID
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.application.utilities.JsonRepresentable
import net.corda.v5.base.annotations.CordaSerializable

@CordaSerializable
class ProbeMessage(
    val linearId: UUID,
    val message: String,
    //  Parties Involved
    val launcher: Party,
    val target: Party
) : JsonRepresentable {
    val participants: List<AbstractParty> get() = listOf(launcher, target)

    override fun toJsonString(): String {
        return Gson().toJson(this.toDto())
    }

    fun toDto(): ProbeMessageDto {
        return ProbeMessageDto(
            message,
            launcher.name.toString(),
            target.name.toString(),
            linearId.toString()
        )
    }
}

