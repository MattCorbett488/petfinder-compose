package com.matthias.petfinder.api

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.plugin
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class PetFinderClient {
    // TODO: Probably want to set up DI here at some point
    companion object {
        private var _INSTANCE: PetFinderClient? = null
        val INSTANCE: PetFinderClient
            get() = _INSTANCE ?: PetFinderClient().also { _INSTANCE = it }
    }

    private val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
    private val bearerTokens = mutableListOf<String>()

    private val httpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokens.lastOrNull()?.let { BearerTokens(it, "") }
                    }
                    sendWithoutRequest { it.url.buildString().endsWith("/v2/oauth2/token") }
                    refreshTokens {
                        client.submitForm(
                            "https://api.petfinder.com/v2/oauth2/token",
                            formParameters = parameters {
                                append("grant_type", "client_credentials")
                                append("client_id", remoteConfig.getString("client_id"))
                                append("client_secret", remoteConfig.getString("client_secret"))
                            }) { markAsRefreshTokenRequest() }
                            .body<JsonObject>()["access_token"]?.jsonPrimitive?.content
                            ?.let { BearerTokens(it, "") }
                    }
                }
            }
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.ALL
            }
            install(HttpTimeout)
        }
    }

    init {
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate()
    }

    @Serializable
    data class AnimalWrapper(val animals: List<Animal>)

    @Serializable
    data class Animal(val id: Int, val type: String, val species: String)

    suspend fun initialize() {
        val token = httpClient.submitForm(
            "https://api.petfinder.com/v2/oauth2/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("client_id", remoteConfig.getString("client_id"))
                append("client_secret", remoteConfig.getString("client_secret"))
            })
            .body<JsonObject>()["access_token"]?.jsonPrimitive?.content
        token?.let { bearerTokens.add(it) }
        httpClient.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>().firstOrNull()
            ?.clearToken()
    }

    suspend fun animals(): List<Animal> {
        return httpClient.get("https://api.petfinder.com/v2/animals").body<AnimalWrapper>().animals
    }
}
