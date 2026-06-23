package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY registeredAt DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY registeredAt DESC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY id DESC")
    fun getAllTemplatesFlow(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates")
    suspend fun getAllTemplates(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE type = :type")
    suspend fun getTemplatesByType(type: String): List<TemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templateList: List<TemplateEntity>)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplateById(id: Int)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY id DESC")
    fun getAllPresetsFlow(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)
}
