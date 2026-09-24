package com.moneyfamily.app.data

import io.github.jan.supabase.postgrest.from

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class CloudSyncRepository(
    private val room: RoomRepository,
    private val supabase: SupabaseRepository = SupabaseRepository(),
    private val budgetStore: BudgetStore? = null
) {
    suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseClientProvider.isConfigured || supabase.currentUserId() == null) return@runCatching
            val familyId = supabase.familiesForCurrentUser().firstOrNull()?.id
                ?: supabase.createFamily("La mia famiglia")
            syncReferenceData(familyId)
            syncOperations(familyId)
            syncBudgets(familyId)
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

        // Import reference data created on another device before pushing local changes.
        for (remote in remoteTypes) {
            if (localTypes.none { it.name.equals(remote.name, true) }) {
                room.addType(remote.name)
            }
        }
        for (remote in remoteCategories) {
            if (localCategories.none { it.name.equals(remote.name, true) }) {
                room.addCategory(remote.name)
            }
        }
        for (remote in remoteMembers) {
            if (localMembers.none { it.name.equals(remote.displayName, true) }) {
                room.addMember(remote.displayName)
            }
        }

        // Re-read local reference data after imports so remote-only values can be mapped locally.
        val syncedTypes = room.allTypes()
        val syncedCategories = room.allCategories()
        val syncedMembers = room.allMembers()
        val syncedTypeIds = syncedTypes.associateBy { it.name.lowercase() }
        val syncedCategoryIds = syncedCategories.associateBy { it.name.lowercase() }

        for (remoteLink in supabase.client.from("type_category_links").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudTypeCategoryLinkDto>()) {
            val remoteType = remoteTypes.firstOrNull { it.id == remoteLink.typologyId } ?: continue
            val remoteCategory = remoteCategories.firstOrNull { it.id == remoteLink.categoryId } ?: continue
            val localType = syncedTypeIds[remoteType.name.lowercase()] ?: continue
            val localCategory = syncedCategoryIds[remoteCategory.name.lowercase()] ?: continue
            room.setTypeCategory(localType.id, localCategory.id)
        }

        // Push local mappings as well, including mappings created after the import.
        for (mapping in room.allMappings()) {
            val type = syncedTypes.firstOrNull { it.id == mapping.typeId } ?: continue
            val category = syncedCategories.firstOrNull { it.id == mapping.categoryId } ?: continue
            val typeId = remoteTypes.firstOrNull { it.name.equals(type.name, true) }?.id
                ?: UUID.nameUUIDFromBytes((familyId + ":type:" + type.name.lowercase()).toByteArray()).toString()
            val categoryId = remoteCategories.firstOrNull { it.name.equals(category.name, true) }?.id
                ?: UUID.nameUUIDFromBytes((familyId + ":category:" + category.name.lowercase()).toByteArray()).toString()
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

        // Read the remote state before publishing local deletions. A tombstone must never
        // overwrite a newer remote edit.
        val remoteOperationsBeforePush = supabase.client.from("operations").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudOperationDto>()
        val remoteById = remoteOperationsBeforePush.associateBy { it.id }

        for (tombstone in room.allTombstones()) {
            val remote = remoteById[tombstone.cloudId]
            if (remote != null && remote.updatedAt.isAfterTimestamp(tombstone.deletedAt)) {
                // A newer remote version wins; leave the tombstone consumed and let the
                // normal remote-import phase restore the operation if it is still active.
                room.removeTombstone(tombstone.cloudId)
                continue
            }

            supabase.client.from("operations").upsert(
                CloudOperationDto(
                    id = tombstone.cloudId,
                    familyId = familyId,
                    amount = 0.0,
                    description = "",
                    operationDate = "1970-01-01",
                    paymentMethod = "",
                    createdBy = supabase.currentUserId(),
                    updatedAt = tombstone.deletedAt,
                    deletedAt = tombstone.deletedAt
                )
            )
            room.removeTombstone(tombstone.cloudId)
        }

        local.forEach { movement ->
            val cloudId = movement.cloudId ?: UUID.nameUUIDFromBytes(
                (familyId + ":operation:" + movement.id).toByteArray()
            ).toString()
            val remote = remoteById[cloudId]

            if (remote != null && remote.deletedAt != null) {
                if (!movement.updatedAt.isAfterTimestamp(remote.updatedAt)) {
                    room.deleteByCloudId(cloudId)
                    return@forEach
                }
            }

            if (remote != null && remote.updatedAt.isAfterTimestamp(movement.updatedAt)) {
                val typeName = remoteTypes.firstOrNull { it.id == remote.typologyId }?.name.orEmpty()
                val category = remoteCategories.firstOrNull { it.id == remote.categoryId }?.name.orEmpty()
                val member = remoteMembers.firstOrNull { it.id == remote.memberId }?.displayName.orEmpty()
                if (remote.deletedAt != null) {
                    room.deleteByCloudId(cloudId)
                } else {
                    room.updateCloud(
                        Movement(
                            id = movement.id,
                            type = if (remote.amount >= 0) MovementType.INCOME else MovementType.EXPENSE,
                            amount = remote.amount,
                            category = category,
                            description = remote.description,
                            date = remote.operationDate.fromSupabaseDate(),
                            member = member,
                            paymentMethod = remote.paymentMethod,
                            typeName = typeName
                        ),
                        cloudId,
                        remote.updatedAt
                    )
                }
                return@forEach
            }

            val typologyId = remoteTypes.firstOrNull { it.name.equals(movement.typeName, true) }?.id
            val categoryId = remoteCategories.firstOrNull { it.name.equals(movement.category, true) }?.id
            val memberId = remoteMembers.firstOrNull { it.displayName.equals(movement.member, true) }?.id
            val timestamp = if (movement.updatedAt.isAfterTimestamp("2000-01-01T00:00:00Z")) {
                movement.updatedAt
            } else {
                java.time.Instant.now().toString()
            }

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
                    updatedAt = timestamp,
                    deletedAt = null
                )
            )
            room.setCloudId(movement.id, cloudId, timestamp)
        }

        val remoteOperations = supabase.client.from("operations").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudOperationDto>()

        val existingCloudIds = room.allEntities().mapNotNull { it.cloudId }.toSet()
        for (remote in remoteOperations) {
            if (remote.deletedAt != null) {
                if (remote.id in existingCloudIds) room.deleteByCloudId(remote.id)
                room.removeTombstone(remote.id)
                continue
            }
            if (remote.id in existingCloudIds) continue

            val typeName = remoteTypes.firstOrNull { it.id == remote.typologyId }?.name.orEmpty()
            val category = remoteCategories.firstOrNull { it.id == remote.categoryId }?.name.orEmpty()
            val member = remoteMembers.firstOrNull { it.id == remote.memberId }?.displayName.orEmpty()
            val usedIds = room.allEntities().map { it.id }.toHashSet()
            var generatedId = -(remote.id.hashCode().toLong() and Long.MAX_VALUE).coerceAtLeast(1L)
            while (generatedId in usedIds) generatedId--

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
                remote.id,
                remote.updatedAt
            )
        }
    }

    private suspend fun syncBudgets(familyId: String) {
        val store = budgetStore ?: return
        val localBudgets = store.all()
        val remoteTypes = supabase.client.from("typologies").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudTypologyDto>()
        val typeByName = remoteTypes.associateBy { it.name.lowercase() }

        // Push local budgets using deterministic IDs so each account/month/type has one row.
        localBudgets.forEach { (monthKey, entries) ->
            val parts = monthKey.split("-")
            if (parts.size != 2) return@forEach
            val year = parts[0].toIntOrNull() ?: return@forEach
            val month = parts[1].toIntOrNull() ?: return@forEach
            entries.forEach { (typeName, amount) ->
                val remoteType = typeByName[typeName.lowercase()] ?: return@forEach
                val id = UUID.nameUUIDFromBytes(
                    (familyId + ":budget:" + monthKey + ":" + typeName.lowercase()).toByteArray()
                ).toString()
                supabase.client.from("budgets").upsert(
                    CloudBudgetDto(
                        id = id,
                        familyId = familyId,
                        typologyId = remoteType.id,
                        categoryId = null,
                        year = year,
                        month = month,
                        amount = amount,
                        createdBy = supabase.currentUserId(),
                        createdAt = java.time.Instant.now().toString(),
                        updatedAt = java.time.Instant.now().toString()
                    )
                )
            }
        }

        // Import budgets created/updated on another device.
        val remoteBudgets = supabase.client.from("budgets").select {
            filter { eq("family_id", familyId) }
        }.decodeList<CloudBudgetDto>()
        val merged = localBudgets.toMutableMap().mapValues { it.value.toMutableMap() }.toMutableMap()
        remoteBudgets.forEach { remote ->
            val typeName = remoteTypes.firstOrNull { it.id == remote.typologyId }?.name ?: return@forEach
            val key = "%04d-%02d".format(remote.year, remote.month)
            merged.getOrPut(key) { mutableMapOf() }[typeName] = remote.amount
        }
        store.replaceAll(merged.mapValues { it.value.toMap() })
    }

    private fun String.isAfterTimestamp(other: String): Boolean =
        runCatching { java.time.Instant.parse(this).isAfter(java.time.Instant.parse(other)) }
            .getOrElse { this > other }


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
private data class CloudBudgetDto(
    val id: String,
    @SerialName("family_id") val familyId: String,
    @SerialName("typology_id") val typologyId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val year: Int,
    val month: Int,
    val amount: Double,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String
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
