package com.moneyfamily.app.data

import com.moneyfamily.app.BuildConfig

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import io.ktor.client.statement.bodyAsText

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
        client.auth.signUpWith(Email, redirectUrl = SupabaseClientProvider.AUTH_REDIRECT_URL) {
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

    /**
     * Account-centric Cloud workspace.
     *
     * The database keeps the existing family_id relationships internally for
     * backwards compatibility, but the user does not create or manage a family.
     * One authenticated MoneyFamily account owns one hidden workspace shared by
     * all devices logged into that account.
     */
    suspend fun ensureAccountWorkspace(): String = withContext(Dispatchers.IO) {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Devi effettuare l'accesso all'account MoneyFamily.")
        familiesForCurrentUser().firstOrNull()?.id
            ?: createFamily("MoneyFamily Account")
    }

    suspend fun isPremiumForCurrentAccount(): Boolean = withContext(Dispatchers.IO) {
        if (BuildConfig.INTERNAL_PREMIUM_TEST && client.auth.currentUserOrNull()?.id != null) return@withContext true
        if (client.auth.currentUserOrNull()?.id == null) return@withContext false
        val familyId = ensureAccountWorkspace()
        client.postgrest.rpc(
            "family_has_active_premium",
            buildJsonObject { put("p_family_id", familyId) }
        ).decodeSingle<Boolean>()
    }

suspend fun isPremiumForCurrentFamily(): Boolean = runCatching {
        isPremiumForCurrentAccount()
    }.getOrDefault(false)

    suspend fun verifyPremiumPurchase(purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        // Purchase verification is intentionally account-gated.
        if (client.auth.currentUserOrNull()?.id == null) return@withContext false
        ensureAccountWorkspace()
        runCatching {
            val response = client.functions.invoke(
                function = "verify-premium-purchase",
                body = buildJsonObject {
                    put("purchaseToken", purchaseToken)
                    put("productId", "moneyfamily_premium")
                }
            )
            val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            payload["premium"]?.jsonPrimitive?.boolean == true
        }.getOrDefault(false)
    }
}
