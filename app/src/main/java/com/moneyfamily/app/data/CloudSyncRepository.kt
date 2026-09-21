package com.moneyfamily.app.data

import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class CloudSyncRepository(
    private val room: RoomRepository,
    private val supabase: SupabaseRepository = SupabaseRepository()
) {
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseClientProvider.isConfigured || supabase.currentUserId() == null) return@runCatching
            val familyId = supabase.familiesForCurrentUser().firstOrNull()?.id
                ?: supabase.createFamily("La mia famiglia")
            syncReferenceData(familyId)
            syncOperations(familyId)
        }
    }

    private suspend fun syncReferenceData(familyId: String) {
        val localTypes = room.allTypes()
        val localCategories = room.allCategories()
        val localMembers = room.allMembers()

        val remoteTypes = supabase.client.from("typologies").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudTypologyDto>()
        val remoteCategories = supabase.client.from("categories").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudCategoryDto>()
        val remoteMembers = supabase.client.from("family_members").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudMemberDto>()

        val typeIds = mutableMapOf<String, String>()
        localTypes.forEach { local ->
            val remote = remoteTypes.firstOrNull { it.name.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":type:" + local.name.lowercase()).toByteArray()).toString()
            supabase.client.from("typologies").upsert(CloudTypologyDto(id, familyId, local.name, local.active))
            typeIds[local.id.toString()] = id
        }

        val categoryIds = mutableMapOf<String, String>()
        localCategories.forEach { local ->
            val remote = remoteCategories.firstOrNull { it.name.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":category:" + local.name.lowercase()).toByteArray()).toString()
            supabase.client.from("categories").upsert(CloudCategoryDto(id, familyId, local.name, local.active))
            categoryIds[local.id.toString()] = id
        }

        val memberIds = mutableMapOf<String, String>()
        localMembers.forEach { local ->
            val remote = remoteMembers.firstOrNull { it.displayName.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":member:" + local.name.lowercase()).toByteArray()).toString()
            supabase.client.from("family_members").upsert(CloudMemberDto(id, familyId, null, local.name, "member"))
            memberIds[local.id.toString()] = id
        }

        room.allMappings().forEach { mapping ->
            val typeId = typeIds[mapping.typeId.toString()] ?: return@forEach
            val categoryId = categoryIds[mapping.categoryId.toString()] ?: return@forEach
            supabase.client.from("type_category_links").upsert(
                CloudTypeCategoryLinkDto(familyId, typeId, categoryId)
            )
        }
    }

    private suspend fun syncOperations(familyId: String) {
        val local = room.allEntities()
        val remoteTypes = supabase.client.from("typologies").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudTypologyDto>()
        val remoteCategories = supabase.client.from("categories").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudCategoryDto>()
        val remoteMembers = supabase.client.from("family_members").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudMemberDto>()

        local.forEach { movement ->
            val cloudId = movement.cloudId ?: UUID.nameUUIDFromBytes((familyId + ":operation:" + movement.id).toByteArray()).toString()
            val typologyId = remoteTypes.firstOrNull { it.name.equals(movement.typeName, true) }?.id
            val categoryId = remoteCategories.firstOrNull { it.name.equals(movement.category, true) }?.id
            val memberId = remoteMembers.firstOrNull { it.displayName.equals(movement.member, true) }?.id

            supabase.client.from("operations").upsert(
                CloudOperationDto(
                    id = cloudId,
                    familyId = familyId,
                    typologyId = typologyId,
                    categoryId = categoryId,
                    memberId = memberId,
                    amount = movement.amount,
                    description = movement.description,
                    operationDate = movement.date.toSupabaseDate(),
                    paymentMethod = movement.paymentMethod,
                    createdBy = supabase.currentUserId(),
                    updatedAt = "2000-01-01T00:00:00Z",
                    deletedAt = null
                )
            )
            room.setCloudId(movement.id, cloudId)
        }

        val remoteOperations = supabase.client.from("operations").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudOperationDto>()

        val existingCloudIds = room.all().mapNotNull { it.cloudId }.toSet()
        remoteOperations.filter { it.deletedAt == null && it.id !in existingCloudIds }.forEach { remote: CloudOperationDto ->
            val typeName = remoteTypes.firstOrNull { it.id == remote.typologyId }?.name.orEmpty()
            val category = remoteCategories.firstOrNull { it.id == remote.categoryId }?.name.orEmpty()
            val member = remoteMembers.firstOrNull { it.id == remote.memberId }?.displayName.orEmpty()
            val generatedId = (remote.id.hashCode().toLong() and Long.MAX_VALUE)
            room.insertCloud(
                Movement(
                    id = generatedId,
                    type = if (remote.amount >= 0) MovementType.INCOME else MovementType.EXPENSE,
                    amount = remote.amount,
                    category = category,
                    description = remote.description,
                    date = remote.operationDate.fromSupabaseDate(),
                    member = member,
                    paymentMethod = remote.paymentMethod,
                    typeName = typeName
                ),
                remote.id
            )
        }
    }

    private fun String.toSupabaseDate(): String {
        val parts = split("/")
        return if (parts.size == 3) parts[2] + "-" + parts[1].padStart(2, '0') + "-" + parts[0].padStart(2, '0') else this
    }

    private fun String.fromSupabaseDate(): String {
        val parts = split("-")
        return if (parts.size == 3) parts[2] + "/" + parts[1] + "/" + parts[0] else this
    }

    private val SupabaseRepository.client get() = SupabaseClientProvider.client
}

@Serializable
private data class CloudTypologyDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val name: String,
    val active: Boolean = true
)

@Serializable
private data class CloudCategoryDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    val name: String,
    val active: Boolean = true
)

@Serializable
private data class CloudMemberDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("display_name") val displayName: String,
    val role: String = "member"
)

@Serializable
private data class CloudTypeCategoryLinkDto(
    @SerialName("family_id") val familyId: String,
    @SerialName("typology_id") val typologyId: String,
    @SerialName("category_id") val categoryId: String
)

@Serializable
private data class CloudOperationDto(
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
