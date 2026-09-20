package com.moneyfamily.app.data

import android.content.Context
import org.json.JSONObject

data class BudgetEntry(val monthKey: String, val typeName: String, val amount: Double)

class BudgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("moneyfamily_budget", Context.MODE_PRIVATE)

    fun get(monthKey: String): Map<String, Double> {
        val root = runCatching { JSONObject(prefs.getString("budgets", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val month = root.optJSONObject(monthKey) ?: return emptyMap()
        return month.keys().asSequence().associateWith { month.optDouble(it, 0.0) }
    }

    fun set(monthKey: String, typeName: String, amount: Double) {
        val root = runCatching { JSONObject(prefs.getString("budgets", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val month = root.optJSONObject(monthKey) ?: JSONObject().also { root.put(monthKey, it) }
        month.put(typeName, amount.coerceAtLeast(0.0))
        prefs.edit().putString("budgets", root.toString()).apply()
    }

    fun copy(fromMonthKey: String, toMonthKey: String) {
        val source = get(fromMonthKey)
        val root = runCatching { JSONObject(prefs.getString("budgets", "{}") ?: "{}") }.getOrDefault(JSONObject())
        val target = JSONObject()
        source.forEach { (type, amount) -> target.put(type, amount) }
        root.put(toMonthKey, target)
        prefs.edit().putString("budgets", root.toString()).apply()
    }
}

fun monthKey(calendar: java.util.Calendar): String =
    "%04d-%02d".format(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1)
