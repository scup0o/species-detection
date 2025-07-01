package com.project.speciesdetection.data.model.observation

import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp
import java.util.Date

data class Save(
    @ServerTimestamp
    val savedAt: Timestamp? = null
)
