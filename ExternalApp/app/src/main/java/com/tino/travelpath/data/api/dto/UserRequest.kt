package com.tino.travelpath.data.api.dto

data class UserRequest(
    val name: String,
    val email: String,
    val password: String? = null // Required for registration, optional for updates
)

