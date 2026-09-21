package com.moneyfamily.app.data

import io.github.jan.supabase.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CreateFamilyParams(
    @SerialName("family_name") val familyName: String
)

class SupabaseRepository(
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseClientProvider.client
) {
    suspend fun currentUserId(): String? = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull()?.id
    }

    suspend fun currentUserEmail(): String? = withContext(Dispatchers.IO) {
        client.auth.currentUserOrNull()?.email
    }

    suspend fun signUp(email: String, password: String): String? = withContext(Dispatchers.IO) {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        client.auth.currentUserOrNull()?.id
    }

    suspend fun signIn(email: String, password: String) = withContext(Dispatchers.IO) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOutCurrentDevice() = withContext(Dispatchers.IO) {
        client.auth.signOut()
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
        client.postgrest.rpc(
            "create_family",
            CreateFamilyParams(name.trim())
        ).decodeSingle<String>()
    }
}
