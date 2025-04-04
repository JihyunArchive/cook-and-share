package com.example.test.model

data class FridgeRequest(
    val ingredientName: String,
    val storageArea: String,
    val fridgeDate: String,
    val dateOption: String,
    val quantity: Double,
    val price: Double,
    val unitCategory: String,
    val unitDetail: String,
    val userId: Long
)
