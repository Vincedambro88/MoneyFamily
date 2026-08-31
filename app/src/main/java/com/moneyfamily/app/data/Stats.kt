package com.moneyfamily.app.data

data class MonthStats(val year: Int, val month: Int, val income: Double, val expense: Double) {
    val balance: Double get() = income + expense
}

data class CategoryStats(val category: String, val amount: Double, val percentage: Double)

data class MemberStats(val member: String, val income: Double, val expense: Double) {
    val balance: Double get() = income + expense
}

private fun Movement.yearMonth(): Pair<Int, Int>? {
    val parts = date.split("/")
    if (parts.size != 3) return null
    val year = parts[2].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    return year to month
}

fun List<Movement>.monthStats(): List<MonthStats> {
    val valid = mapNotNull { m -> m.yearMonth()?.let { it to m } }
    if (valid.isEmpty()) return emptyList()
    val years = valid.map { it.first.first }.distinct().sortedDescending()
    return years.flatMap { year ->
        (1..12).map { month ->
            val values = valid.filter { it.first.first == year && it.first.second == month }.map { it.second }
            MonthStats(
                year,
                month,
                values.filter { it.type == MovementType.INCOME }.sumOf { it.amount },
                values.filter { it.type == MovementType.EXPENSE }.sumOf { it.amount }
            )
        }
    }
}

fun List<Movement>.categoryStats(): List<CategoryStats> {
    val expenses = filter { it.type == MovementType.EXPENSE }
    val totalAbs = expenses.sumOf { kotlin.math.abs(it.amount) }
    return expenses.groupBy { it.category }.map { (category, items) ->
        val amount = items.sumOf { it.amount }
        CategoryStats(category, amount, if (totalAbs > 0) kotlin.math.abs(amount) * 100 / totalAbs else 0.0)
    }.sortedBy { it.amount }
}

fun List<Movement>.memberStats(): List<MemberStats> = groupBy { it.member }.map { (member, items) ->
    MemberStats(
        member,
        items.filter { it.type == MovementType.INCOME }.sumOf { it.amount },
        items.filter { it.type == MovementType.EXPENSE }.sumOf { it.amount }
    )
}.sortedBy { it.expense }
