package com.moneyfamily.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID

class CloudSyncRepository(
    private val room: RoomRepository,
    private val supabase: SupabaseRepository = SupabaseRepository(),
    private val budgetStore: BudgetStore? = null
) {
    private suspend fun syncReferenceData(familyId: String) {
        val localTypes = room.allTypesIncludingInactive()
        val localCategories = room.allCategoriesIncludingInactive()
        val localMembers = room.allMembersIncludingInactive()
        val currentUserEmail = supabase.currentUserEmail().orEmpty()

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
        val typePayloads = localTypes.map { local ->
            val remote = remoteTypes.firstOrNull { it.name.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":type:" + local.name.lowercase()).toByteArray()).toString()
            typeIds[local.id.toString()] = id
            CloudTypologyDto(id, familyId, local.name, local.active)
        }
        if (typePayloads.isNotEmpty()) {
            supabase.client.from("typologies").upsert(typePayloads)
        }

        val categoryIds = mutableMapOf<String, String>()
        val categoryPayloads = localCategories.map { local ->
            val remote = remoteCategories.firstOrNull { it.name.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":category:" + local.name.lowercase()).toByteArray()).toString()
            categoryIds[local.id.toString()] = id
            CloudCategoryDto(id, familyId, local.name, local.active)
        }
        if (categoryPayloads.isNotEmpty()) {
            supabase.client.from("categories").upsert(categoryPayloads)
        }

        // A logged-in account is not a household member. Household members are
        // represented separately from authentication users.
        val remoteHouseholdMembers = remoteMembers.filter { it.userId == null }
        remoteMembers.filter { it.userId != null }.forEach { accountMember ->
            room.removeMemberByName(accountMember.displayName)
        }
        val householdLocalMembers = localMembers.filterNot {
            it.name.equals(currentUserEmail.substringBefore("@"), true) ||
            remoteMembers.any { remote -> remote.userId != null && remote.displayName.equals(it.name, true) }
        }

        val memberIds = mutableMapOf<String, String>()
        val memberPayloads = householdLocalMembers.map { local ->
            val remote = remoteHouseholdMembers.firstOrNull { it.displayName.equals(local.name, true) }
            val id = remote?.id ?: UUID.nameUUIDFromBytes((familyId + ":member:" + local.name.lowercase()).toByteArray()).toString()
            memberIds[local.id.toString()] = id
            CloudMemberDto(id, familyId, null, local.name, "member")
        }
        if (memberPayloads.isNotEmpty()) {
            supabase.client.from("family_members").upsert(memberPayloads)
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
        for (remote in remoteHouseholdMembers) {
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
        val mappingPayloads = mutableListOf<CloudTypeCategoryLinkDto>()
        for (mapping in room.allMappings()) {
            val type = syncedTypes.firstOrNull { it.id == mapping.typeId } ?: continue
            val category = syncedCategories.firstOrNull { it.id == mapping.categoryId } ?: continue
            val typeId = remoteTypes.firstOrNull { it.name.equals(type.name, true) }?.id
                ?: UUID.nameUUIDFromBytes((familyId + ":type:" + type.name.lowercase()).toByteArray()).toString()
            val categoryId = remoteCategories.firstOrNull { it.name.equals(category.name, true) }?.id
                ?: UUID.nameUUIDFromBytes((familyId + ":category:" + category.name.lowercase()).toByteArray()).toString()
            mappingPayloads += CloudTypeCategoryLinkDto(familyId, typeId, categoryId)
        }
        if (mappingPayloads.isNotEmpty()) {
            supabase.client.from("type_category_links").upsert(mappingPayloads)
        }
    }

    suspend fun saveToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseClientProvider.isConfigured) error("Cloud non configurato.")
            val currentUserId = supabase.currentUserId() ?: error("Account MoneyFamily non autenticato.")
            val familyId = supabase.familiesForCurrentUser().firstOrNull()?.id
                ?: supabase.createFamily("MoneyFamily Account")

            syncReferenceData(familyId)

            val remoteTypes = supabase.client.from("typologies").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudTypologyDto>()
            val remoteCategories = supabase.client.from("categories").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudCategoryDto>()
            val remoteMembers = supabase.client.from("family_members").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudMemberDto>()

            val local = room.allEntities()
            val usedCloudIds = mutableSetOf<String>()
            val payloads = ArrayList<CloudOperationDto>(local.size)

            for (movement in local) {
                var cloudId = movement.cloudId
                if (cloudId == null || !usedCloudIds.add(cloudId)) {
                    cloudId = UUID.randomUUID().toString()
                    usedCloudIds.add(cloudId)
                }

                val timestamp = if (movement.updatedAt.isAfterTimestamp("2000-01-01T00:00:00Z")) {
                    movement.updatedAt
                } else {
                    java.time.Instant.now().toString()
                }

                if (movement.cloudId != cloudId || movement.updatedAt != timestamp) {
                    room.setCloudId(movement.id, cloudId, timestamp)
                }

                val typologyId = remoteTypes.firstOrNull { it.name.equals(movement.typeName, true) }?.id
                val categoryId = remoteCategories.firstOrNull { it.name.equals(movement.category, true) }?.id
                val memberId = remoteMembers.firstOrNull { it.displayName.equals(movement.member, true) }?.id

                payloads += CloudOperationDto(
                    id = cloudId,
                    familyId = familyId,
                    typologyId = typologyId,
                    categoryId = categoryId,
                    memberId = memberId,
                    amount = movement.amount,
                    description = movement.description,
                    operationDate = movement.date.toSupabaseDate(),
                    paymentMethod = movement.paymentMethod,
                    createdBy = currentUserId,
                    updatedAt = timestamp,
                    deletedAt = null
                )
            }

            val rpcParams = Json.encodeToJsonElement(
                ReplaceOperationsParams.serializer(),
                ReplaceOperationsParams(
                    pFamilyId = familyId,
                    pOperations = payloads
                )
            ).jsonObject
            val result = supabase.client.postgrest.rpc(
                "replace_family_operations",
                rpcParams
            ).decodeSingle<ReplaceOperationsResult>()

            room.clearTombstones()
            result.count
        }
    }

    suspend fun loadFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (!SupabaseClientProvider.isConfigured) error("Cloud non configurato.")
            if (supabase.currentUserId() == null) error("Account MoneyFamily non autenticato.")
            val familyId = supabase.familiesForCurrentUser().firstOrNull()?.id
                ?: error("Nessun workspace Cloud associato all'account.")

            syncReferenceData(familyId)

            val remoteTypes = supabase.client.from("typologies").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudTypologyDto>()
            val remoteCategories = supabase.client.from("categories").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudCategoryDto>()
            val remoteMembers = supabase.client.from("family_members").select {
                filter { eq("family_id", familyId) }
            }.decodeList<CloudMemberDto>()
            // PostgREST limits a single response to 1000 rows by default.
            // Fetch operations in pages so every Cloud operation is restored.
            val remoteOperations = mutableListOf<CloudOperationDto>()
            val pageSize = 500
            var page = 0
            while (true) {
                val from = page * pageSize
                val to = from + pageSize - 1
                val batch = supabase.client.from("operations").select {
                    filter { eq("family_id", familyId) }
                    range(from.toLong(), to.toLong())
                }.decodeList<CloudOperationDto>()
                remoteOperations += batch
                if (batch.size < pageSize) break
                page++
            }

            // "Carica dal Cloud" is an explicit restore: the Cloud snapshot
            // becomes the local source of truth. Fetch everything first; only then
            // replace Room atomically, so a failed download never destroys local data.
            val restored = remoteOperations
                .filter { it.deletedAt == null }
                .mapNotNull { remote ->
                    val typeName = remoteTypes.firstOrNull { it.id == remote.typologyId }?.name.orEmpty()
                    val category = remoteCategories.firstOrNull { it.id == remote.categoryId }?.name.orEmpty()
                    val member = remoteMembers.firstOrNull { it.id == remote.memberId }?.displayName.orEmpty()
                    if (remote.id.isBlank()) null else MovementWithCloudId(
                        movement = Movement(
                            id = -(remote.id.hashCode().toLong() and Long.MAX_VALUE).coerceAtLeast(1L),
                            type = if (remote.amount >= 0) MovementType.INCOME else MovementType.EXPENSE,
                            amount = remote.amount,
                            category = category,
                            description = remote.description,
                            date = remote.operationDate.fromSupabaseDate(),
                            member = member,
                            paymentMethod = remote.paymentMethod,
                            typeName = typeName
                        ),
                        cloudId = remote.id,
                        updatedAt = remote.updatedAt
                    )
                }

            val uniqueRestored = buildList {
                val usedIds = mutableSetOf<Long>()
                for (item in restored) {
                    var id = item.movement.id
                    while (!usedIds.add(id)) id--
                    add(item.copy(movement = item.movement.copy(id = id)))
                }
            }

            room.replaceOperationsFromCloud(uniqueRestored)
            syncBudgets(familyId)            uniqueRestored.size
        }
    }

    suspend fun sync(): Result<Int> = saveToCloud()

    private suspend fun syncBudgets(familyId: String) {
        val store = budgetStore ?: return
        val localBudgets = store.all()
        val currentUserId = supabase.currentUserId()
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
                        createdBy = currentUserId,
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
private data class ReplaceOperationsParams(
    @SerialName("p_family_id") val pFamilyId: String,
    @SerialName("p_operations") val pOperations: List<CloudOperationDto>
)

@Serializable
private data class ReplaceOperationsResult(
    val count: Int
)

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
