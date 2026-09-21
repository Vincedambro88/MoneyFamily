package com.moneyfamily.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class SupabaseFamilyDto(
    val id: String,
    val name: String,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class SupabaseFamilyMemberDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("display_name") val displayName: String,
    val role: String
)

@Serializable
data class SupabaseOperationDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("typology_id") val typologyId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    val amount: Double,
    val description: String,
    @SerialName("operation_date") val operationDate: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)
