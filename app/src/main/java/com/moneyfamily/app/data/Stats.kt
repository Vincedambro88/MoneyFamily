package com.moneyfamily.app.data

data class MonthStats(val year: Int, val month: Int, val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

data class CategoryStats(val category: String, val amount: Double, val percentage: Double)

data class MemberStats(val member: String, val income: Double, val expense: Double) {
    val balance: Double get() = income - expense
}

fun List<Movement>.monthStats(): List<MonthStats> = groupBy {
    val parts = it.date.split("/")
    if (parts.size == 3) parts[2].toIntOrNull() to parts[1].toIntOrNull() else null to null
}.mapNotNull { (key, values) ->
    val year = key.first ?: return@mapNotNull null
    val month = key.second ?: return@mapNotNull null
    MonthStats(year, month, values.filter { it.type == MovementType.INCOME }.sumOf { it.amount }, values.filter { it.type == MovementType.EXPENSE }.sumOf { it.amount })
}.sortedWith(compareByDescending<MonthStats> { it.year }.thenByDescending { it.month })

fun List<Movement>.categoryStats(): List<CategoryStats> {
    val total = filter { it.type == MovementType.EXPENSE }.sumOf { it.amount }
    return filter { it.type == MovementType.EXPENSE }.groupBy { it.category }.map { (category, items) ->
        val amount = items.sumOf { it.amount }
        CategoryStats(category, amount, if (total > 0) amount * 100 / total else 0.0)
    }.sortedByDescending { it.amount }
}

fun List<Movement>.memberStats(): List<MemberStats> = groupBy { it.member }.map { (member, items) ->
    MemberStats(member, items.filter { it.type == MovementType.INCOME }.sumOf { it.amount }, items.filter { it.type == MovementType.EXPENSE }.sumOf { it.amount })
}.sortedByDescending { it.expense }
