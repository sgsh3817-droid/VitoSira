package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserEntity
import com.example.data.firebase.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance(context) }
    val scope = rememberCoroutineScope()

    var usersList by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var totalProjectsCount by remember { mutableStateOf(0) }
    var totalTemplatesCount by remember { mutableStateOf(0) }

    // Analytics figures
    val analyticsDownloads = 1485
    val analyticsActiveUsers = 42

    LaunchedEffect(Unit) {
        scope.launch {
            usersList = firestore.userDao.getAllUsers()
            totalProjectsCount = firestore.projectDao.getAllProjectsFlow().first().size
            totalTemplatesCount = firestore.templateDao.getAllTemplates().size
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Panel", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("admin_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Stats Overview Row
            Text(
                text = "Performance Metrics & Analytics Reports",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total Sign Ups",
                    value = usersList.size.toString(),
                    icon = Icons.Default.Group,
                    color = Color(0xFFD0BCFF),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Daily Active Users",
                    value = analyticsActiveUsers.toString(),
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Projects Created",
                    value = totalProjectsCount.toString(),
                    icon = Icons.Default.Brush,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Saved Gallery Exports",
                    value = analyticsDownloads.toString(),
                    icon = Icons.Default.Download,
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f)
                )
            }

            // User Accounts Listings
            Text(
                text = "Manage User Profiles (${usersList.size})",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (usersList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = "No user profiles registered in database.",
                        color = Color.LightGray,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                usersList.forEach { u ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = u.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = u.email, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                if (u.phone != null) {
                                    Text(text = "Phone: ${u.phone}", color = Color.Gray, fontSize = 10.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Toggle status Block/Unlock
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val upUser = u.copy(isBlocked = !u.isBlocked)
                                            firestore.userDao.updateUser(upUser)
                                            usersList = firestore.userDao.getAllUsers()
                                            Toast.makeText(context, if (upUser.isBlocked) "Blocked ${u.name}" else "Unlocked ${u.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (u.isBlocked) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("block_user_btn_${u.id}")
                                ) {
                                    Text(text = if (u.isBlocked) "Unlock" else "Block", fontSize = 11.sp, color = Color.White)
                                }

                                // Delete user account
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            firestore.userDao.deleteUserById(u.id)
                                            usersList = firestore.userDao.getAllUsers()
                                            Toast.makeText(context, "Deleted User profile", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.testTag("delete_user_btn_${u.id}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Profile", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Templates metadata analytics status
            Text(
                text = "Preset Library Overview",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Default Templates: $totalTemplatesCount Starter Packs", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sticker Elements Configured: 30 Canva-style assets", color = Color.LightGray, fontSize = 11.sp)
                    Text("Typography Fonts Integrated: 4 Native pairings", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Standard Admin credentials matches: any logged in administrator session.", color = Color.Gray, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color.LightGray, fontSize = 11.sp)
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
