package com.moneyfamily.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Transitional repository. It keeps existing users' JSON data while the Room schema is introduced. */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("moneyfamily_data", Context.MODE_PRIVATE)

    fun loadMovements(): List<Movement> = try {
        val array = JSONArray(prefs.getString("movements", "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(Movement(o.getLong("id"), MovementType.valueOf(o.optString("type", "EXPENSE")), o.getDouble("amount"), o.optString("category", "Altro"), o.optString("description"), o.optString("date"), o.optString("member", "Famiglia"), o.optString("paymentMethod", "Carta")))
            }
        }
    } catch (_: Exception) { emptyList() }

    fun saveMovements(items: List<Movement>) {
        val array = JSONArray()
        items.forEach { m -> array.put(JSONObject().apply { put("id", m.id); put("type", m.type.name); put("amount", m.amount); put("category", m.category); put("description", m.description); put("date", m.date); put("member", m.member); put("paymentMethod", m.paymentMethod) }) }
        prefs.edit().putString("movements", array.toString()).apply()
    }
}
