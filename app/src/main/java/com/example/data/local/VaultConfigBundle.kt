package com.example.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VaultConfigBundle(
    val formatVersion: Int = 1,
    val appName: String = "Internet Vault",
    val exportedAtMillis: Long,
    val profiles: List<ExportedServerProfile>
)

@JsonClass(generateAdapter = true)
data class ExportedServerProfile(
    val name: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val tunnelMode: String,
    val dnsPrimary: String,
    val dnsSecondary: String,
    val mtu: Int,
    val preferredCarrier: String,
    val authUsername: String,
    val publicKeyOrToken: String,
    val notes: String
)
