package com.basistheory.threeds.example.model

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeResult(
    val panTokenId: String,

    val threedsVersion: String,

    val acsTransactionId: String,

    val dsTransactionId: String,

    val sdkTransactionId: String,

    val acsReferenceNumber: String,

    val dsReferenceNumber: String,

    val authenticationValue: String = "",

    val authenticationStatus: String,

    val authenticationStatusCode: String,

    val eci: String = "",

    val purchaseAmount: String? = null,

    val merchantName: String? = null,

    val currency: String? = null,

    val acsChallengeMandated: String? = null,

    val authenticationChallengeType: String? = null,

    val authenticationStatusReason: String? = null,

    val acsSignedContent: String? = null,

    val messageExtensions: List<String> = emptyList(),

    val acsRenderingType: AcsRenderingType? = null
)

@Serializable
data class AcsRenderingType(
    val acsInterface: String,
    val acsUiTemplate: String
)
