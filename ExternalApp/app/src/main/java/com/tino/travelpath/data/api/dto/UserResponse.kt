package com.tino.travelpath.data.api.dto

import com.google.gson.annotations.SerializedName

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("createdAt")
    val createdAt: String,
    @SerializedName("updatedAt")
    val updatedAt: String
)





