package com.moneyfamily.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private const val FORMAT_VERSION = 1

    suspend fun writeBackup(context: Context, uri: Uri, repository: RoomRepository) {
        val snapshot = repository.exportSnapshot()
        val root = JSONObject().apply {
            put("format", "MoneyFamily Backup")
            put("formatVersion", FORMAT_VERSION)
            put("createdAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date()))
            put("movements", JSONArray().also { array -> snapshot.movements.forEach { array.put(it.toJson()) } })
            put("types", JSONArray().also { array -> snapshot.types.forEach { array.put(it.toJson()) } })
            put("categories", JSONArray().also { array -> snapshot.categories.forEach { array.put(it.toJson()) } })
            put("members", JSONArray().also { array -> snapshot.members.forEach { array.put(it.toJson()) } })
            put("mappings", JSONArray().also { array -> snapshot.mappings.forEach { array.put(it.toJson()) } })
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
            ?: error("Impossibile creare il file di backup")
    }

    suspend fun restoreBackup(context: Context, uri: Uri, repository: RoomRepository) {
        val text = context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)?.toString(Charsets.UTF_8)
            ?: error("Impossibile leggere il file di backup")
        val root = JSONObject(text)
        require(root.optString("format") == "MoneyFamily Backup") { "File non riconosciuto" }
        require(root.optInt("formatVersion", -1) == FORMAT_VERSION) { "Versione backup non supportata" }

        val snapshot = MoneyFamilySnapshot(
            movements = root.getJSONArray("movements").toEntityList { it.toMovementEntity() },
            types = root.getJSONArray("types").toEntityList { it.toTypeEntity() },
            categories = root.getJSONArray("categories").toEntityList { it.toCategoryEntity() },
            members = root.getJSONArray("members").toEntityList { it.toMemberEntity() },
            mappings = root.getJSONArray("mappings").toEntityList { it.toMappingEntity() }
        )
        require(snapshot.types.map { it.id }.distinct().size == snapshot.types.size) { "Backup tipologie non valido" }
        require(snapshot.categories.map { it.id }.distinct().size == snapshot.categories.size) { "Backup categorie non valido" }
        require(snapshot.members.map { it.id }.distinct().size == snapshot.members.size) { "Backup membri non valido" }
        repository.restoreSnapshot(snapshot)
    }

    private fun MovementEntity.toJson() = JSONObject().apply {
        put("id", id); put("type", type); put("typeName", typeName); put("amount", amount)
        put("category", category); put("description", description); put("date", date); put("member", member); put("paymentMethod", paymentMethod)
    }
    private fun TypeEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("active", active) }
    private fun CategoryEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("active", active) }
    private fun FamilyMemberEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("active", active) }
    private fun TypeCategoryEntity.toJson() = JSONObject().apply { put("id", id); put("typeId", typeId); put("categoryId", categoryId) }

    private fun JSONObject.toMovementEntity() = MovementEntity(
        id = getLong("id"), type = getString("type"), typeName = optString("typeName"), amount = getDouble("amount"),
        category = optString("category"), description = optString("description"), date = optString("date"),
        member = optString("member"), paymentMethod = optString("paymentMethod")
    )
    private fun JSONObject.toTypeEntity() = TypeEntity(getLong("id"), getString("name"), optBoolean("active", true))
    private fun JSONObject.toCategoryEntity() = CategoryEntity(getLong("id"), getString("name"), optBoolean("active", true))
    private fun JSONObject.toMemberEntity() = FamilyMemberEntity(getLong("id"), getString("name"), optBoolean("active", true))
    private fun JSONObject.toMappingEntity() = TypeCategoryEntity(getLong("id"), getLong("typeId"), getLong("categoryId"))

    private inline fun <T> JSONArray.toEntityList(transform: (JSONObject) -> T): List<T> = buildList(length()) {
        for (i in 0 until length()) add(transform(getJSONObject(i)))
    }
}
