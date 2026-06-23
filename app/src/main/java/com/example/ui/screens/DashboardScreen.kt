package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.PresetEntity
import com.example.data.database.ProjectEntity
import com.example.data.database.TemplateEntity
import com.example.data.database.UserEntity
import com.example.data.firebase.FirebaseAuth
import com.example.data.firebase.FirebaseFirestore
import com.example.model.ColorData
import com.example.model.DesignLayer
import com.example.model.ImageProperties
import com.example.model.LayerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToEditor: (projectId: Int) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance(context) }
    val firestore = remember { FirebaseFirestore.getInstance(context) }
    val scope = rememberCoroutineScope()

    val currentUser by auth.currentUser.collectAsState()
    val rawRecentProjects by firestore.projectDao.getAllProjectsFlow().collectAsState(initial = emptyList())
    val rawPresets by firestore.presetDao.getAllPresetsFlow().collectAsState(initial = emptyList())
    val rawTemplates by firestore.templateDao.getAllTemplatesFlow().collectAsState(initial = emptyList())

    // State modals
    var showCreateProjectModal by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("New Project") }
    var projectWidth by remember { mutableStateOf("1080") }
    var projectHeight by remember { mutableStateOf("1080") }
    var projectUnit by remember { mutableStateOf("Pixels") } // Pixels, Inches, CM, MM
    var saveAsPreset by remember { mutableStateOf(false) }

    var showProfileModal by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var profilePhone by remember { mutableStateOf("") }

    var activeTab by remember { mutableStateOf("all") } // all, logo, banner, thumbnail, dp

    LaunchedEffect(currentUser) {
        currentUser?.let {
            profileName = it.name
            profilePhone = it.phone ?: ""
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "App Logo Icon",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("PixelCraft Studio", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Admin View button
                    IconButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.testTag("admin_panel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Area",
                            tint = Color(0xFFFFB74D)
                        )
                    }

                    // Profile avatar button
                    IconButton(
                        onClick = { showProfileModal = true },
                        modifier = Modifier.testTag("profile_button")
                    ) {
                        if (currentUser?.profilePicUri != null) {
                            AsyncImage(
                                model = currentUser?.profilePicUri,
                                contentDescription = "Profile Pic",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, Color(0xFFD0BCFF), CircleShape)
                            )
                        } else {
                            Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile Settings", tint = Color.LightGray)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateProjectModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_project_fab")
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "New Layout")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Hero section with welcoming header and premium badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hello, ${currentUser?.name ?: "Designer"}!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Let's edit mock-free masterworks manually today.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (currentUser?.isPremium == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB74D).copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PRO ACCOUNT",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Image(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Decorative Palette",
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                            .padding(12.dp)
                    )
                }
            }

            // Quick Actions Panel
            Text(
                text = "Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionItem(
                    icon = Icons.Default.AddPhotoAlternate,
                    label = "Quick Edit",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // Quick Edit simulates adding an image project immediately
                        scope.launch {
                            val defaultImageUri = "https://picsum.photos/1280/720"
                            val quickProj = ProjectEntity(
                                name = "Quick Custom Photo",
                                width = 1280,
                                height = 720,
                                layers = listOf(
                                    DesignLayer(
                                        id = "bg_photo",
                                        name = "Base Image Layer",
                                        type = LayerType.IMAGE,
                                        positionX = 640f,
                                        positionY = 360f,
                                        imageProperties = ImageProperties(
                                            imageSrc = defaultImageUri,
                                            isLocalAsset = false
                                        )
                                    )
                                )
                            )
                            val id = firestore.projectDao.insertProject(quickProj)
                            onNavigateToEditor(id.toInt())
                        }
                    }
                )
                QuickActionItem(
                    icon = Icons.Default.QrCode,
                    label = "Custom Size",
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f),
                    onClick = { showCreateProjectModal = true }
                )
                QuickActionItem(
                    icon = Icons.Default.Stars,
                    label = "Logo Creator",
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        activeTab = "logo"
                    }
                )
            }

            // My Projects list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Projects (${rawRecentProjects.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (rawRecentProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "No project icon", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Create a new custom canvas, or select a template to launch the draft editor.", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rawRecentProjects) { project ->
                        ProjectRowItem(
                            project = project,
                            onOpen = { onNavigateToEditor(project.id) },
                            onDelete = {
                                scope.launch {
                                    firestore.projectDao.deleteProjectById(project.id)
                                }
                            }
                        )
                    }
                }
            }

            // Design Preset Templates Panel
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Templates Gallery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Scrollable tabs filter
            ScrollableTabRow(
                selectedTabIndex = when(activeTab) {
                    "all" -> 0
                    "logo" -> 1
                    "banner" -> 2
                    "thumbnail" -> 3
                    "dp" -> 4
                    else -> 0
                },
                containerColor = Color.Transparent,
                contentColor = Color(0xFFD0BCFF),
                edgePadding = 0.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Tab(selected = activeTab == "all", onClick = { activeTab = "all" }) {
                    Text("All Items", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
                Tab(selected = activeTab == "logo", onClick = { activeTab = "logo" }) {
                    Text("Logos", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
                Tab(selected = activeTab == "banner", onClick = { activeTab = "banner" }) {
                    Text("Banners", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
                Tab(selected = activeTab == "thumbnail", onClick = { activeTab = "thumbnail" }) {
                    Text("Thumbnails", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
                Tab(selected = activeTab == "dp", onClick = { activeTab = "dp" }) {
                    Text("Profile DPs", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            // Template Items Grid
            val filteredTemplates = remember(rawTemplates, activeTab) {
                if (activeTab == "all") rawTemplates else rawTemplates.filter { it.type == activeTab }
            }

            if (filteredTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD0BCFF))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredTemplates.chunked(2).forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowList.forEach { temp ->
                                TemplateGridCard(
                                    template = temp,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        // Start a project based on template schema
                                        scope.launch {
                                            val newProjectFromTemplate = ProjectEntity(
                                                name = "My ${temp.name}",
                                                width = temp.width,
                                                height = temp.height,
                                                unit = temp.unit,
                                                layers = temp.layers
                                            )
                                            val createdId = firestore.projectDao.insertProject(newProjectFromTemplate)
                                            onNavigateToEditor(createdId.toInt())
                                        }
                                    }
                                )
                            }
                            if (rowList.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // New Project Custom Options Modal Form
    if (showCreateProjectModal) {
        AlertDialog(
            onDismissRequest = { showCreateProjectModal = false },
            title = { Text("New Design Options", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth().testTag("proj_name_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = projectWidth,
                            onValueChange = { projectWidth = it },
                            label = { Text("Width") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("proj_width_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        OutlinedTextField(
                            value = projectHeight,
                            onValueChange = { projectHeight = it },
                            label = { Text("Height") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("proj_height_input"),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Measurement Unit", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Pixels", "Inches", "CM", "MM").forEach { unit ->
                            val isSelected = projectUnit == unit
                            FilterChip(
                                selected = isSelected,
                                onClick = { projectUnit = unit },
                                label = { Text(unit, fontSize = 11.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = saveAsPreset,
                            onCheckedChange = { saveAsPreset = it }
                        )
                        Text("Save as custom size preset", color = Color.White, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = projectWidth.toIntOrNull() ?: 1080
                        val h = projectHeight.toIntOrNull() ?: 1080
                        scope.launch {
                            if (saveAsPreset) {
                                firestore.presetDao.insertPreset(
                                    PresetEntity(name = "$projectName (${w}x${h})", width = w, height = h, unit = projectUnit)
                                )
                            }
                            val newProject = ProjectEntity(
                                name = projectName,
                                width = w,
                                height = h,
                                unit = projectUnit
                            )
                            val id = firestore.projectDao.insertProject(newProject)
                            showCreateProjectModal = false
                            onNavigateToEditor(id.toInt())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Create Canvas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectModal = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Profile Settings Dialog
    if (showProfileModal) {
        AlertDialog(
            onDismissRequest = { showProfileModal = false },
            title = { Text("Profile & Settings", color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Email: ${currentUser?.email ?: ""}", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profilePhone,
                        onValueChange = { profilePhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = {
                            auth.signOut()
                            showProfileModal = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Log out icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Out")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                auth.updateCurrentUserProfile(profileName, profilePhone.ifEmpty { null }, currentUser?.profilePicUri)
                                showProfileModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Changes")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick)
            .testTag("quick_action_${label.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
fun ProjectRowItem(
    project: ProjectEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(130.dp)
            .clickable(onClick = onOpen)
            .testTag("project_card_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.secondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers representation", tint = Color(0xFFD0BCFF), modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Draft Project",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = project.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.layers.size} Layers (${project.width}x${project.height} ${project.unit.take(2)})",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TemplateGridCard(
    template: TemplateEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick)
            .testTag("template_card_${template.name.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = template.type.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF)
                    )
                }

                Column {
                    Text(
                        text = template.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Preset size: ${template.width}x${template.height}",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
