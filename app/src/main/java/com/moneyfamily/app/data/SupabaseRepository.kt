package com.moneyfamily.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
        val parameters = buildJsonObject {
            put("family_name", name.trim())
        }
        client.postgrest.rpc(
            "create_family",
            parameters
        ).decodeSingle<String>()
    }

suspend fun isPremiumForCurrentFamily(): Boolean = withContext(Dispatchers.IO) {
        val family = familiesForCurrentUser().firstOrNull() ?: return@withContext false
        client.postgrest.rpc(
            "family_has_active_premium",
            buildJsonObject { put("p_family_id", family.id) }
        ).decodeSingle<Boolean>()
    }

    suspend fun verifyPremiumPurchase(purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        client.functions.invoke(
            function = "verify-premium-purchase",
            body = buildJsonObject {
                put("purchaseToken", purchaseToken)
                put("productId", "moneyfamily_premium")
            }
        )
        true
    }
}
