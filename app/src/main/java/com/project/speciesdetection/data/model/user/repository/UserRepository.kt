package com.project.speciesdetection.data.model.user.repository

import com.project.speciesdetection.data.model.user.User

interface UserRepository {
    fun getCurrentUser() : User?
}