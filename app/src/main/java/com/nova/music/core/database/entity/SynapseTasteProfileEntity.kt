package com.nova.music.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "synapse_taste_profile")
data class SynapseTasteProfileEntity(
    @PrimaryKey
    val profileKey: String = "default_user_profile",
    val profileJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
