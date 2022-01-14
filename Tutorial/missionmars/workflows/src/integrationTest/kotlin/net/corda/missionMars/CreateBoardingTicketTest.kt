package net.corda.missionMars

import com.google.gson.GsonBuilder
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.json.JSONObject
import net.corda.missionMars.flows.CreateBoardingTicketInitiator
import net.corda.missionMars.flows.TemplateFlow
import net.corda.test.dev.network.Credentials
import net.corda.test.dev.network.TestNetwork
import net.corda.test.dev.network.withFlow
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class CreateBoardingTicketTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            TestNetwork.forNetwork("missionmars-network").verify {
                hasNode("PartyA").withFlow<TemplateFlow>()
                hasNode("PartyB").withFlow<TemplateFlow>()
            }
        }
    }

    @Test
    fun `Create Boarding Ticker Test`(){
        TestNetwork.forNetwork("missionmars-network").use {
            getNode("PartyA").httpRpc(Credentials("angelenos","password")){
                val clientId = "Launch Pad 1" + LocalDateTime.now()
                val launchDate = "2023-11-02"
                val flowId = with(startFlow(
                        flowName = CreateBoardingTicketInitiator::class.java.name,
                        clientId = clientId,
                        parametersInJson = CreateBoardingTickerParams(
                                ticketDescription = "Space Shuttle 323 - 16C",
                                launchDate = launchDate,
                        )
                )){
                    Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                    Assertions.assertThat(body.`object`.get("clientId")).isEqualTo(clientId)
                    val flowId = body.`object`.get("flowId") as JSONObject
                    Assertions.assertThat(flowId).isNotNull
                    flowId.get("uuid") as String
                }
                eventually {
                    with(retrieveOutcome(flowId)) {
                        Assertions.assertThat(status).isEqualTo(HttpStatus.SC_OK)
                        Assertions.assertThat(body.`object`.get("status")).isEqualTo("COMPLETED")
                    }
                }
                with(retrieveOutcome(flowId)){
                    val resultString = body.`object`.get("resultJson") as String
                    println("--------------------------------")
                    println("Create Boarding Ticket Result: ")
                    println(resultString)
                    println("--------------------------------")
                }
            }
        }
    }
    //helper method.
    private fun CreateBoardingTickerParams(ticketDescription: String, launchDate: String): String {
        return GsonBuilder()
                .create()
                .toJson(mapOf("ticketDescription" to ticketDescription, "launchDate" to launchDate))
    }





    private fun startFlow(
            flowName: String,
            clientId: String = "client-${UUID.randomUUID()}",
            parametersInJson: String
    ): HttpResponse<JsonNode> {
        val body = mapOf(
                "rpcStartFlowRequest" to
                        mapOf(
                                "flowName" to flowName,
                                "clientId" to clientId,
                                "parameters" to mapOf("parametersInJson" to parametersInJson)
                        )
        )
        val request = Unirest.post("flowstarter/startflow")
                .header("Content-Type", "application/json")
                .body(body)

        return request.asJson()
    }

    private fun retrieveOutcome(flowId: String): HttpResponse<JsonNode> {
        val request = Unirest.get("flowstarter/flowoutcome/$flowId").header("Content-Type", "application/json")
        return request.asJson()
    }

    private inline fun <R> eventually(
            duration: Duration = Duration.ofSeconds(5),
            waitBetween: Duration = Duration.ofMillis(100),
            waitBefore: Duration = waitBetween,
            test: () -> R
    ): R {
        val end = System.nanoTime() + duration.toNanos()
        var times = 0
        var lastFailure: AssertionError? = null

        if (!waitBefore.isZero) Thread.sleep(waitBefore.toMillis())

        while (System.nanoTime() < end) {
            try {
                return test()
            } catch (e: AssertionError) {
                if (!waitBetween.isZero) Thread.sleep(waitBetween.toMillis())
                lastFailure = e
            }
            times++
        }

        throw AssertionError("Test failed with \"${lastFailure?.message}\" after $duration; attempted $times times")
    }

}