package com.vkashel.streaming

import org.http4k.client.JettyClient
import org.http4k.core.BodyMode
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

fun main() {
    val port = 8080
    val baseUrl = "http://localhost:$port"
    val config = Jetty(port = port)
    val streamingClient = JettyClient(bodyMode = BodyMode.Stream)
    routes(
        "/receive-stream" bind Method.POST to {
            it.body.stream.bufferedReader().forEachLine { line ->
                println("receive row -> $line")
            }
            Response(Status.OK).body("received")
        },
        "/send-stream" bind Method.POST to {
            val response = streamingClient(Request(Method.POST, "$baseUrl/receive-stream").body(createInput()))
            println(response.bodyString())
            Response(Status.OK).body("done")
        }
    ).asServer(config).start()
}

private fun createInput(): InputStream {
    val input = PipedInputStream()
    val output = PipedOutputStream(input)

    val line = "b".repeat(25) + "\n"

    thread {
        for (it in 1..5) {
            println("data sent")

            output.write(line.toByteArray())
            output.flush()
            Thread.sleep(500)
        }
        output.close()
    }

    return input
}