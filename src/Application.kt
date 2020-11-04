package com.example

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    routing {
        post("/multipart") {
            val multipartData = call.receiveMultipart()
            multipartData.forEachPart {
                if (it is PartData.FileItem) {
                    println(
                        withContext(Dispatchers.IO) {
                            it.streamProvider().readAllBytes().size
                        }
                    )
                }
            }
            call.respondText("test")
        }
        get("/execute/cio") {
            repeat(10) {
                request(HttpClient(CIO))
            }
            call.respondText("success")
        }
        get("/execute/apache") {
            repeat(10) {
                request(HttpClient(Apache))
            }
            call.respondText("success")
        }
    }
}

suspend fun request(client: HttpClient) {
    val body = formData {
        val headers = headersOf(
            HttpHeaders.ContentDisposition,
            ContentDisposition.File
                .withParameter(ContentDisposition.Parameters.Name, "file")
                .withParameter(ContentDisposition.Parameters.FileName, "test.jpg")
                .toString()
        )
        appendInput("file", headers) {
            // dummy 10MB binary file
            ByteArray(10_000_000).inputStream().asInput()
        }
    }
    client.use {
        it.submitFormWithBinaryData<Unit>(
            url = "http://localhost:8080/multipart",
            formData = body
        )
    }
}
