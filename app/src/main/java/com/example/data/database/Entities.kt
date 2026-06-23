package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.DesignLayer
import com.example.model.FilterProperties

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val phone: String? = null,
    val profilePicUri: String? = null,
    val isBlocked: Boolean = false,
    val isPremium: Boolean = false,
    val registeredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val width: Int,
    val height: Int,
    val unit: String = "Pixels", // Pixels, Inches, CM, MM
    val layers: List<DesignLayer> = emptyList(),
    val globalFilter: FilterProperties = FilterProperties(),
    val thumbnailBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // logo, banner, thumbnail, dp
    val category: String, // business, gaming, tech, fashion, vlog, educational, professional
    val width: Int,
    val height: Int,
    val unit: String = "Pixels",
    val layers: List<DesignLayer> = emptyList(),
    val thumbnailBase64: String? = null
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val width: Int,
    val height: Int,
    val unit: String = "Pixels"
)
