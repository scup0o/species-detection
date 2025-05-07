package com.project.speciesdetection.data.model.user.repository

import com.project.speciesdetection.data.model.user.User
import javax.inject.Inject

class RemoteUserRepository @Inject constructor() : UserRepository{
    override fun getCurrentUser(): User? {
        return User("1", "Hai Phung", "haiphung@gmail.com", null)
    }
}