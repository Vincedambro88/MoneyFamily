package com.moneyfamily.app.data

enum class MovementType { INCOME, EXPENSE }

data class Movement(
    val id: Long = 0L,
    val type: MovementType,
    val amount: Double,
    val category: String,
    val description: String,
    val date: String,
    val member: String,
    val paymentMethod: String
)

data class Category(val name: String, val type: MovementType)
data class FamilyMember(val name: String)
data class PaymentMethod(val name: String)
