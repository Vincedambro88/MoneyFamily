package com.moneyfamily.app.data

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {
    suspend fun currentUserId(): String? = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull()?.id
    }

    suspend fun currentProfile(): SupabaseProfileDto? = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
        client.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<SupabaseProfileDto>()
    }

    suspend fun familiesForCurrentUser(): List<SupabaseFamilyDto> = withContext(Dispatchers.IO) {
        client.from("families")
            .select()
            .decodeList<SupabaseFamilyDto>()
    }

    suspend fun familyMembers(familyId: String): List<SupabaseFamilyMemberDto> =
        withContext(Dispatchers.IO) {
            client.from("family_members")
                .select { filter { eq("family_id", familyId) } }
                .decodeList<SupabaseFamilyMemberDto>()
        }

    suspend fun createFamily(name: String): String = withContext(Dispatchers.IO) {
        client.from("create_family")
            .select()
            .decodeSingle<String>()
    }
}
