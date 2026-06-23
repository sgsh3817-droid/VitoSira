package com.example.data.firebase

import android.content.Context
import com.example.data.database.*
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirebaseAuth private constructor(private val userDao: UserDao) {
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    suspend fun signUp(name: String, email: String, passwordHash: String, phone: String? = null): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val existing = userDao.getUserByEmail(email)
            if (existing != null) {
                return@withContext Result.failure(Exception("Email already registered"))
            }
            val newUser = UserEntity(
                name = name,
                email = email,
                passwordHash = passwordHash,
                phone = phone,
                profilePicUri = "https://api.dicebear.com/7.x/identicon/svg?seed=$name"
            )
            val id = userDao.insertUser(newUser)
            val createdUser = newUser.copy(id = id.toInt())
            _currentUser.value = createdUser
            Result.success(createdUser)
        }
    }

    suspend fun signIn(email: String, passwordHash: String): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email) ?: return@withContext Result.failure(Exception("User not found"))
            if (user.passwordHash != passwordHash) {
                return@withContext Result.failure(Exception("Incorrect password"))
            }
            if (user.isBlocked) {
                return@withContext Result.failure(Exception("Your account is blocked by administrative settings."))
            }
            _currentUser.value = user
            Result.success(user)
        }
    }

    suspend fun signInWithGoogle(): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val googleEmail = "sgsh3817@gmail.com" // Matches metadata user email
            val existing = userDao.getUserByEmail(googleEmail)
            if (existing != null) {
                if (existing.isBlocked) {
                    return@withContext Result.failure(Exception("Your account is blocked."))
                }
                _currentUser.value = existing
                return@withContext Result.success(existing)
            }
            val newUser = UserEntity(
                name = "PixelCraft Creator",
                email = googleEmail,
                passwordHash = "google_authenticated_login",
                profilePicUri = "https://api.dicebear.com/7.x/pixel-art/svg?seed=creator",
                isPremium = true // Let Google users get premium out of the box!
            )
            val id = userDao.insertUser(newUser)
            val created = newUser.copy(id = id.toInt())
            _currentUser.value = created
            Result.success(created)
        }
    }

    suspend fun updateCurrentUserProfile(name: String, phone: String?, profilePicUri: String?): Result<UserEntity> {
        return withContext(Dispatchers.IO) {
            val current = _currentUser.value ?: return@withContext Result.failure(Exception("No user logged in"))
            val updated = current.copy(name = name, phone = phone, profilePicUri = profilePicUri)
            userDao.updateUser(updated)
            _currentUser.value = updated
            Result.success(updated)
        }
    }

    fun forgotPassword(email: String): Result<String> {
        return Result.success("A password reset link is simulated. Check your local inputs.")
    }

    fun signOut() {
        _currentUser.value = null
    }

    companion object {
        @Volatile private var INSTANCE: FirebaseAuth? = null
        fun getInstance(context: Context): FirebaseAuth {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = FirebaseAuth(db.userDao())
                INSTANCE = instance
                instance
            }
        }
    }
}

class FirebaseFirestore private constructor(private val db: AppDatabase) {
    val projectDao = db.projectDao()
    val templateDao = db.templateDao()
    val presetDao = db.presetDao()
    val userDao = db.userDao()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (templateDao.getAllTemplates().isEmpty()) {
                prepopulateTemplates()
            }
        }
    }

    private suspend fun prepopulateTemplates() {
        val defaultTemplates = listOf(
            // Logo Templates
            TemplateEntity(
                name = "Business Logo",
                type = "logo",
                category = "business",
                width = 1000,
                height = 1000,
                unit = "Pixels",
                layers = listOf(
                    DesignLayer(
                        id = "logo_circle",
                        name = "Corporate Border",
                        type = LayerType.SHAPE,
                        positionX = 500f,
                        positionY = 500f,
                        scaleX = 1f,
                        scaleY = 1f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.CIRCLE,
                            fillColor = ColorData.Transparent,
                            borderColor = ColorData(33, 150, 243, 255), // Teal/Blue Accent
                            borderWidth = 12f
                        )
                    ),
                    DesignLayer(
                        id = "logo_text",
                        name = "Main Heading",
                        type = LayerType.TEXT,
                        positionX = 500f,
                        positionY = 500f,
                        scaleX = 1f,
                        scaleY = 1f,
                        textProperties = TextProperties(
                            text = "NEXUS CORP",
                            fontSize = 42f,
                            color = ColorData(33, 150, 243, 255),
                            isBold = true,
                            fontFamilyName = "Monospace"
                        )
                    ),
                    DesignLayer(
                        id = "logo_subtext",
                        name = "Tagline",
                        type = LayerType.TEXT,
                        positionX = 500f,
                        positionY = 600f,
                        scaleX = 1f,
                        scaleY = 1f,
                        textProperties = TextProperties(
                            text = "EST. 2026",
                            fontSize = 20f,
                            color = ColorData(120, 120, 120, 255),
                            fontFamilyName = "Sans-Serif"
                        )
                    )
                )
            ),
            TemplateEntity(
                name = "Gaming Logo",
                type = "logo",
                category = "gaming",
                width = 1000,
                height = 1000,
                layers = listOf(
                    DesignLayer(
                        id = "gam_bg",
                        name = "Apex Shield",
                        type = LayerType.SHAPE,
                        positionX = 500f,
                        positionY = 500f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.RECTANGLE,
                            fillColor = ColorData(25, 25, 35, 255),
                            borderColor = ColorData(233, 30, 99, 255), // Pink Neon
                            borderWidth = 8f
                        )
                    ),
                    DesignLayer(
                        id = "gam_icon",
                        name = "Star Emblem",
                        type = LayerType.SHAPE,
                        positionX = 500f,
                        positionY = 400f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.STAR,
                            fillColor = ColorData(233, 30, 99, 255),
                            borderColor = ColorData(255, 255, 255, 255),
                            borderWidth = 3f
                        )
                    ),
                    DesignLayer(
                        id = "gam_text",
                        name = "Gamer Nickname",
                        type = LayerType.TEXT,
                        positionX = 500f,
                        positionY = 650f,
                        textProperties = TextProperties(
                            text = "CYBER RX",
                            fontSize = 54f,
                            color = ColorData(255, 255, 255, 255),
                            isBold = true,
                            strokeWidth = 6f,
                            strokeColor = ColorData(233, 30, 99, 255),
                            textEffect = "Neon"
                        )
                    )
                )
            ),
            // Banner Templates
            TemplateEntity(
                name = "YouTube Banner",
                type = "banner",
                category = "gaming",
                width = 2560,
                height = 1440,
                layers = listOf(
                    DesignLayer(
                        id = "ban_bg",
                        name = "Banner Background",
                        type = LayerType.SHAPE,
                        positionX = 1280f,
                        positionY = 720f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.RECTANGLE,
                            fillColor = ColorData(10, 10, 15, 255)
                        )
                    ),
                    DesignLayer(
                        id = "ban_stripe",
                        name = "Aesthetic Base Stripe",
                        type = LayerType.SHAPE,
                        positionX = 1280f,
                        positionY = 720f,
                        scaleX = 2.5f,
                        scaleY = 0.25f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.RECTANGLE,
                            fillColor = ColorData(40, 40, 55, 255)
                        )
                    ),
                    DesignLayer(
                        id = "ban_title",
                        name = "Main Channel Text",
                        type = LayerType.TEXT,
                        positionX = 1280f,
                        positionY = 680f,
                        textProperties = TextProperties(
                            text = "MAXIMUM VLOGS",
                            fontSize = 80f,
                            color = ColorData(255, 235, 59, 255), // Yellow Glow
                            isBold = true,
                            fontFamilyName = "Monospace",
                            textEffect = "Glow"
                        )
                    ),
                    DesignLayer(
                        id = "ban_subtitle",
                        name = "Sub text info",
                        type = LayerType.TEXT,
                        positionX = 1280f,
                        positionY = 800f,
                        textProperties = TextProperties(
                            text = "NEW VIDEO EVERY TUESDAY & FRIDAY",
                            fontSize = 28f,
                            color = ColorData.White,
                            fontFamilyName = "Sans-Serif"
                        )
                    )
                )
            ),
            // Thumbnail templates
            TemplateEntity(
                name = "Vlog Thumbnail",
                type = "thumbnail",
                category = "vlog",
                width = 1280,
                height = 720,
                layers = listOf(
                    DesignLayer(
                        id = "thumb_bg",
                        name = "Vibrant Backdrop",
                        type = LayerType.SHAPE,
                        positionX = 640f,
                        positionY = 360f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.RECTANGLE,
                            fillColor = ColorData(255, 110, 64, 255) // Bright Orange
                        )
                    ),
                    DesignLayer(
                        id = "thumb_sh",
                        name = "Speech Box Accent",
                        type = LayerType.SHAPE,
                        positionX = 640f,
                        positionY = 360f,
                        scaleX = 0.8f,
                        scaleY = 0.5f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.RECTANGLE,
                            fillColor = ColorData(255, 255, 255, 255),
                            borderColor = ColorData.Black,
                            borderWidth = 6f
                        )
                    ),
                    DesignLayer(
                        id = "thumb_title",
                        name = "Stellar Title Text",
                        type = LayerType.TEXT,
                        positionX = 640f,
                        positionY = 320f,
                        textProperties = TextProperties(
                            text = "MY DAILY TRIP!",
                            fontSize = 62f,
                            color = ColorData.Black,
                            isBold = true,
                            fontFamilyName = "Cursive"
                        )
                    ),
                    DesignLayer(
                        id = "thumb_sub",
                        name = "Sub title text",
                        type = LayerType.TEXT,
                        positionX = 640f,
                        positionY = 430f,
                        textProperties = TextProperties(
                            text = "unbelievable moments inside...",
                            fontSize = 32f,
                            color = ColorData(255, 110, 64, 255),
                            isBold = true,
                            fontFamilyName = "Sans-Serif"
                        )
                    )
                )
            ),
            // Profile DP Template
            TemplateEntity(
                name = "Professional DP",
                type = "dp",
                category = "professional",
                width = 1080,
                height = 1080,
                layers = listOf(
                    DesignLayer(
                        id = "dp_shape_bg",
                        name = "Profile Ring",
                        type = LayerType.SHAPE,
                        positionX = 540f,
                        positionY = 540f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.CIRCLE,
                            fillColor = ColorData(220, 230, 242, 255),
                            borderColor = ColorData(26, 115, 232, 255),
                            borderWidth = 10f
                        )
                    ),
                    DesignLayer(
                        id = "dp_star",
                        name = "Top Quality badge",
                        type = LayerType.SHAPE,
                        positionX = 540f,
                        positionY = 250f,
                        scaleX = 0.25f,
                        scaleY = 0.25f,
                        shapeProperties = ShapeProperties(
                            shapeType = ShapeType.STAR,
                            fillColor = ColorData(255, 193, 7, 255) // Amber Star
                        )
                    ),
                    DesignLayer(
                        id = "dp_name",
                        name = "Initials",
                        type = LayerType.TEXT,
                        positionX = 540f,
                        positionY = 500f,
                        textProperties = TextProperties(
                            text = "CEO",
                            fontSize = 80f,
                            color = ColorData(26, 115, 232, 255),
                            isBold = true,
                            fontFamilyName = "Serif"
                        )
                    )
                )
            )
        )
        templateDao.insertTemplates(defaultTemplates)
    }

    companion object {
        @Volatile private var INSTANCE: FirebaseFirestore? = null
        fun getInstance(context: Context): FirebaseFirestore {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = FirebaseFirestore(db)
                INSTANCE = instance
                instance
            }
        }
    }
}

class FirebaseStorage private constructor() {
    fun uploadImageBytes(bytes: ByteArray): String {
        // Safe conversion of image bytes to simulated storage URL (Base64 data url)
        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
        return "data:image/png;base64,$base64"
    }

    fun uploadSamplePlaceholder(type: String): String {
        return "https://picsum.photos/seed/$type/1280/720"
    }

    companion object {
        private var INSTANCE: FirebaseStorage? = null
        fun getInstance(): FirebaseStorage {
            if (INSTANCE == null) {
                INSTANCE = FirebaseStorage()
            }
            return INSTANCE!!
        }
    }
}
