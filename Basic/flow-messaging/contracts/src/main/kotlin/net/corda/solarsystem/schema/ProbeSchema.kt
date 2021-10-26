package net.corda.solarsystem.schema

import net.corda.v5.persistence.MappedSchema
import net.corda.v5.persistence.UUIDConverter
import java.util.UUID
import javax.persistence.*
import net.corda.v5.base.annotations.CordaSerializable

/**
 * The family of schemas for Probe messages.
 */
object ProbeSchema

/**
 * A probe message schema.
 */
object ProbeSchemaV1 : MappedSchema(
    schemaFamily = ProbeSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentProbeMessage::class.java)
) {

    override val migrationResource: String
        get() = "probe.changelog-master"

    @Entity
    @NamedQuery(
        name = "ProbeSchemaV1.PersistentProbeMessage.FindAll",
        query = "FROM net.corda.solarsystem.schema.ProbeSchemaV1\$PersistentProbeMessage"
    )
    @Table(name = "probe_message")
    @CordaSerializable
    class PersistentProbeMessage(
        @Id
        @Column(name = "linear_id")
        var linearId: String,

        @Column(name = "message")
        var message: String,

        @Column(name = "launcher")
        var launcherName: String,

        @Column(name = "target")
        var targetName: String,
    )
}
