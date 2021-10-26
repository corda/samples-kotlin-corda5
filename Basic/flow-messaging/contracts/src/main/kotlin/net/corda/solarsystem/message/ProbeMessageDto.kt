package net.corda.solarsystem.message

import net.corda.v5.base.annotations.CordaSerializable

/**
 * A JSON-serializable DTO for returning results from named-queries.
 */
@CordaSerializable
data class ProbeMessageDto(val message: String, val launcher: String, val target: String, val linearId: String)