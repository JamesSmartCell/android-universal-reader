package com.kormax.universalreader.jwt
import com.google.gson.annotations.SerializedName // If using Gson

data class LoyaltyPointBalance(
    @SerializedName("string") val stringValue: String
)

data class LoyaltyPoints(
    @SerializedName("balance") val balance: LoyaltyPointBalance,
    @SerializedName("label") val label: String
)

data class Barcode(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

data class LoyaltyObjectPayload(
    @SerializedName("id") val id: String,
    @SerializedName("classId") val classId: String,
    @SerializedName("state") val state: String,
    @SerializedName("accountId") val accountId: String,
    @SerializedName("accountName") val accountName: String,
    @SerializedName("loyaltyPoints") val loyaltyPoints: LoyaltyPoints,
    @SerializedName("barcode") val barcode: Barcode
)

data class JwtPayloadWrapper(
    @SerializedName("loyaltyObjects") val loyaltyObjects: List<LoyaltyObjectPayload>
)