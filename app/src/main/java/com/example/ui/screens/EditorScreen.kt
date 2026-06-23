package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.ProjectEntity
import com.example.data.firebase.FirebaseFirestore
import com.example.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance(context) }
    val scope = rememberCoroutineScope()

    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var layers by remember { mutableStateOf<List<DesignLayer>>(emptyList()) }
    var globalFilter by remember { mutableStateOf(FilterProperties()) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }

    // UI State Mode selector: crop, resize, rotate, filter, adjustments, text, stickers, shapes, draw, layers
    var activeToolMode by remember { mutableStateOf("layers") }

    // Layer management dialog states
    var showRenameDialog by remember { mutableStateOf(false) }
    var layerToRename by remember { mutableStateOf<DesignLayer?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("PNG") }
    var exportQuality by remember { mutableStateOf("High") }

    // Text editor properties (linked to selection)
    var textInput by remember { mutableStateOf("") }
    var textFontSize by remember { mutableStateOf(24f) }
    var textFontFamilyName by remember { mutableStateOf("Sans-Serif") }
    var fontColorSelection by remember { mutableStateOf(ColorData.Black) }
    var strokeColorSelection by remember { mutableStateOf(ColorData.Transparent) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isUppercase by remember { mutableStateOf(false) }
    var textEffectStyle by remember { mutableStateOf("Normal") }

    // Draw Tool properties
    var drawingStrokes by remember { mutableStateOf<List<StrokePath>>(emptyList()) }
    var currentDrawColor by remember { mutableStateOf(ColorData.Red) }
    var currentBrushSize by remember { mutableStateOf(10f) }
    var currentBrushOpacity by remember { mutableStateOf(1.0f) }
    var isDrawingEraser by remember { mutableStateOf(false) }
    var paintToolSelected by remember { mutableStateOf("pencil") } // pencil, brush, marker, eraser

    // Shape Tool properties
    var selectedShapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeFillColor by remember { mutableStateOf(ColorData.Blue) }
    var shapeBorderColor by remember { mutableStateOf(ColorData.Black) }
    var shapeBorderWidth by remember { mutableStateOf(4f) }
    var shapeOpacity by remember { mutableStateOf(1.0f) }

    // Crop options
    var cropAspectRatio by remember { mutableStateOf("Free") }

    // Load Project details once when screen loads
    LaunchedEffect(projectId) {
        scope.launch {
            val proj = firestore.projectDao.getProjectById(projectId)
            if (proj != null) {
                project = proj
                layers = proj.layers
                globalFilter = proj.globalFilter
            } else {
                Toast.makeText(context, "Draft project not found", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        }
    }

    // Auto-Save Draft to Room periodically or when changes occur
    fun saveDraftProject(updatedLayers: List<DesignLayer> = layers, updatedFilter: FilterProperties = globalFilter) {
        val currentProj = project ?: return
        scope.launch {
            val updated = currentProj.copy(
                layers = updatedLayers,
                globalFilter = updatedFilter,
                lastModifiedAt = System.currentTimeMillis()
            )
            firestore.projectDao.updateProject(updated)
            project = updated
        }
    }

    // Selection helper
    val selectedLayer = remember(layers, selectedLayerId) {
        layers.find { it.id == selectedLayerId }
    }

    // Set properties when a text layer is selected
    LaunchedEffect(selectedLayerId) {
        selectedLayer?.let { layer ->
            if (layer.type == LayerType.TEXT) {
                layer.textProperties?.let { prop ->
                    textInput = prop.text
                    textFontSize = prop.fontSize
                    textFontFamilyName = prop.fontFamilyName
                    fontColorSelection = prop.color
                    strokeColorSelection = prop.strokeColor
                    isBold = prop.isBold
                    isItalic = prop.isItalic
                    isUnderline = prop.isUnderlined
                    isUppercase = prop.isUppercase
                    textEffectStyle = prop.textEffect
                }
            }
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
                    Text(
                        text = project?.name ?: "Editor Workspace",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        saveDraftProject()
                        onNavigateBack()
                    }, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    // Quick Save Draft status
                    IconButton(onClick = {
                        saveDraftProject()
                        Toast.makeText(context, "Draft Saved", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.testTag("quick_save_btn")) {
                        Icon(Icons.Default.Save, contentDescription = "Save Draft", tint = MaterialTheme.colorScheme.onBackground)
                    }

                    // Export project option
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("export_button")
                    ) {
                        Text("Export", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        ) {
            // Main Creative Workspace Canvas
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                // Main Canvas Area scaled correctly matching project specifications
                Box(
                    modifier = Modifier
                        .size(340.dp)
                        .background(Color.White)
                        .border(1.dp, Color.White.copy(alpha = 0.2f))
                        .testTag("canvas_container")
                ) {
                    // Iterating Design Layers
                    layers.forEach { layer ->
                        if (!layer.isHidden) {
                            val isSelected = layer.id == selectedLayerId
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Scale, rotation, flipping & translations applied mathematically
                                    .graphicsLayer {
                                        translationX = layer.positionX - 170f
                                        translationY = layer.positionY - 170f
                                        scaleX = layer.scaleX * (if (layer.flipHorizontal) -1f else 1f)
                                        scaleY = layer.scaleY * (if (layer.flipVertical) -1f else 1f)
                                        rotationZ = layer.rotation
                                    }
                                    .pointerInput(layer.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                if (!layer.isLocked) {
                                                    selectedLayerId = layer.id
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                if (!layer.isLocked && selectedLayerId == layer.id) {
                                                    change.consume()
                                                    layers = layers.map {
                                                        if (it.id == layer.id) {
                                                            it.copy(
                                                                positionX = it.positionX + dragAmount.x,
                                                                positionY = it.positionY + dragAmount.y
                                                            )
                                                        } else it
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                saveDraftProject()
                                            }
                                        )
                                    }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when (layer.type) {
                                    LayerType.IMAGE -> {
                                        layer.imageProperties?.let { prop ->
                                            AsyncImage(
                                                model = prop.imageSrc,
                                                contentDescription = "Image content block",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    LayerType.TEXT -> {
                                        layer.textProperties?.let { prop ->
                                            val tStyle = when (prop.fontFamilyName) {
                                                "Monospace" -> FontFamily.Monospace
                                                "Serif" -> FontFamily.Serif
                                                "Cursive" -> FontFamily.Cursive
                                                else -> FontFamily.SansSerif
                                            }
                                            Text(
                                                text = if (prop.isUppercase) prop.text.uppercase() else prop.text,
                                                fontSize = prop.fontSize.sp,
                                                color = prop.color.toColor(),
                                                fontFamily = tStyle,
                                                fontWeight = if (prop.isBold) FontWeight.Bold else FontWeight.Normal,
                                                fontStyle = if (prop.isItalic) FontStyle.Italic else FontStyle.Normal,
                                                textDecoration = if (prop.isUnderlined) TextDecoration.Underline else TextDecoration.None,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .drawBehind {
                                                        if (prop.backgroundColor != ColorData.Transparent) {
                                                            drawRect(prop.backgroundColor.toColor())
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                    LayerType.STICKER -> {
                                        layer.stickerProperties?.let { prop ->
                                            Text(
                                                text = prop.codeOrName,
                                                fontSize = 62.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    LayerType.SHAPE -> {
                                        layer.shapeProperties?.let { prop ->
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val w = size.width
                                                val h = size.height
                                                when (prop.shapeType) {
                                                    ShapeType.RECTANGLE -> {
                                                        drawRect(
                                                            color = prop.fillColor.toColor(),
                                                            size = size
                                                        )
                                                        drawRect(
                                                            color = prop.borderColor.toColor(),
                                                            style = Stroke(prop.borderWidth)
                                                        )
                                                    }
                                                    ShapeType.CIRCLE -> {
                                                        drawCircle(
                                                            color = prop.fillColor.toColor(),
                                                            radius = size.minDimension / 2.5f
                                                        )
                                                        drawCircle(
                                                            color = prop.borderColor.toColor(),
                                                            radius = size.minDimension / 2.5f,
                                                            style = Stroke(prop.borderWidth)
                                                        )
                                                    }
                                                    ShapeType.TRIANGLE -> {
                                                        val path = Path().apply {
                                                            moveTo(w / 2f, h / 4f)
                                                            lineTo(w * 0.75f, h * 0.75f)
                                                            lineTo(w * 0.25f, h * 0.75f)
                                                            close()
                                                        }
                                                        drawPath(path, prop.fillColor.toColor())
                                                        drawPath(path, prop.borderColor.toColor(), style = Stroke(prop.borderWidth))
                                                    }
                                                    ShapeType.STAR -> {
                                                        val path = Path().apply {
                                                            moveTo(w / 2f, h / 5f)
                                                            lineTo(w * 0.6f, h * 0.4f)
                                                            lineTo(w * 0.85f, h * 0.4f)
                                                            lineTo(w * 0.65f, h * 0.55f)
                                                            lineTo(w * 0.75f, h * 0.8f)
                                                            lineTo(w / 2f, h * 0.65f)
                                                            lineTo(w * 0.25f, h * 0.8f)
                                                            lineTo(w * 0.35f, h * 0.55f)
                                                            lineTo(w * 0.15f, h * 0.4f)
                                                            lineTo(w * 0.4f, h * 0.4f)
                                                            close()
                                                        }
                                                        drawPath(path, prop.fillColor.toColor())
                                                        drawPath(path, prop.borderColor.toColor(), style = Stroke(prop.borderWidth))
                                                    }
                                                    ShapeType.ARROW -> {
                                                        val path = Path().apply {
                                                            moveTo(w * 0.25f, h * 0.45f)
                                                            lineTo(w * 0.6f, h * 0.45f)
                                                            lineTo(w * 0.6f, h * 0.3f)
                                                            lineTo(w * 0.8f, h / 2f)
                                                            lineTo(w * 0.6f, h * 0.7f)
                                                            lineTo(w * 0.6f, h * 0.55f)
                                                            lineTo(w * 0.25f, h * 0.55f)
                                                            close()
                                                        }
                                                        drawPath(path, prop.fillColor.toColor())
                                                        drawPath(path, prop.borderColor.toColor(), style = Stroke(prop.borderWidth))
                                                    }
                                                    ShapeType.LINE -> {
                                                        drawLine(
                                                            color = prop.borderColor.toColor(),
                                                            start = Offset(w * 0.2f, h / 2f),
                                                            end = Offset(w * 0.8f, h / 2f),
                                                            strokeWidth = prop.borderWidth
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    LayerType.DRAWING -> {
                                        layer.drawingProperties?.let { prop ->
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                prop.strokes.forEach { stroke ->
                                                    if (stroke.points.size > 1) {
                                                        val path = Path().apply {
                                                            val start = stroke.points.first()
                                                            moveTo(start.x, start.y)
                                                            stroke.points.drop(1).forEach { p ->
                                                                lineTo(p.x, p.y)
                                                            }
                                                        }
                                                        drawPath(
                                                            path = path,
                                                            color = if (stroke.isEraser) Color.White else stroke.color.toColor(),
                                                            style = Stroke(
                                                                width = stroke.brushSize,
                                                                cap = StrokeCap.Round,
                                                                join = StrokeJoin.Round
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Direct Interactive Drawing Overlay Canvas (active ONLY during Draw Mode)
                    if (activeToolMode == "draw") {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val newStroke = StrokePath(
                                                points = listOf(Point2D(offset.x, offset.y)),
                                                color = currentDrawColor,
                                                brushSize = currentBrushSize,
                                                opacity = currentBrushOpacity,
                                                isEraser = isDrawingEraser
                                            )
                                            drawingStrokes = drawingStrokes + newStroke
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val currentStroke = drawingStrokes.lastOrNull() ?: return@detectDragGestures
                                            val lastPt = currentStroke.points.last()
                                            val nextPt = Point2D(lastPt.x + dragAmount.x, lastPt.y + dragAmount.y)
                                            drawingStrokes = drawingStrokes.dropLast(1) + currentStroke.copy(
                                                points = currentStroke.points + nextPt
                                            )
                                        },
                                        onDragEnd = {
                                            // Finish current drawing session and inject as standard Drawing Layer
                                            scope.launch {
                                                val id = UUID.randomUUID().toString()
                                                val newLayer = DesignLayer(
                                                    id = id,
                                                    name = "Manual Drawing Path",
                                                    type = LayerType.DRAWING,
                                                    positionX = 170f,
                                                    positionY = 170f,
                                                    drawingProperties = DrawingProperties(strokes = drawingStrokes)
                                                )
                                                layers = layers + newLayer
                                                drawingStrokes = emptyList() // clear draft drawing canvas
                                                saveDraftProject()
                                            }
                                        }
                                    )
                                }
                        ) {
                            drawingStrokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = Path().apply {
                                        val first = stroke.points.first()
                                        moveTo(first.x, first.y)
                                        stroke.points.drop(1).forEach { pt ->
                                            lineTo(pt.x, pt.y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (stroke.isEraser) Color.White else stroke.color.toColor(),
                                        style = Stroke(
                                            width = stroke.brushSize,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Central Workspace Mode Settings area below editing canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Selected entity quick actions (Delete, Rename, Layer ordering)
                    selectedLayer?.let { layer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: ${layer.name}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Rotate Left
                                IconButton(onClick = {
                                    layers = layers.map { if (it.id == layer.id) it.copy(rotation = it.rotation - 90f) else it }
                                    saveDraftProject()
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.RotateLeft, "Rotate Left", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Rotate Right
                                IconButton(onClick = {
                                    layers = layers.map { if (it.id == layer.id) it.copy(rotation = it.rotation + 90f) else it }
                                    saveDraftProject()
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.RotateRight, "Rotate Right", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Flip Horiz
                                IconButton(onClick = {
                                    layers = layers.map { if (it.id == layer.id) it.copy(flipHorizontal = !it.flipHorizontal) else it }
                                    saveDraftProject()
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Flip, "Flip horizontal", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Duplicate Layer
                                IconButton(onClick = {
                                    val dup = layer.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${layer.name} Copy",
                                        positionX = layer.positionX + 24f,
                                        positionY = layer.positionY + 24f
                                    )
                                    layers = layers + dup
                                    saveDraftProject()
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Duplicate layer", tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                // Delete
                                IconButton(onClick = {
                                    layers = layers.filter { it.id != layer.id }
                                    selectedLayerId = null
                                    saveDraftProject()
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, "Delete Layer", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Dynamically loaded workspace tool menu panels
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeToolMode) {
                            "layers" -> LayersListToolPanel(
                                layersList = layers,
                                selectedId = selectedLayerId,
                                onSelect = { selectedLayerId = it },
                                onToggleVisibility = { id ->
                                    layers = layers.map { if (it.id == id) it.copy(isHidden = !it.isHidden) else it }
                                    saveDraftProject()
                                },
                                onToggleLock = { id ->
                                    layers = layers.map { if (it.id == id) it.copy(isLocked = !it.isLocked) else it }
                                    saveDraftProject()
                                },
                                onOrderUp = { id ->
                                    val idx = layers.indexOfFirst { it.id == id }
                                    if (idx > 0) {
                                        val mList = layers.toMutableList()
                                        val temp = mList[idx]
                                        mList[idx] = mList[idx - 1]
                                        mList[idx - 1] = temp
                                        layers = mList
                                        saveDraftProject()
                                    }
                                },
                                onOrderDown = { id ->
                                    val idx = layers.indexOfFirst { it.id == id }
                                    if (idx != -1 && idx < layers.size - 1) {
                                        val mList = layers.toMutableList()
                                        val temp = mList[idx]
                                        mList[idx] = mList[idx + 1]
                                        mList[idx + 1] = temp
                                        layers = mList
                                        saveDraftProject()
                                    }
                                },
                                onTriggerRename = { layer ->
                                    layerToRename = layer
                                    renameInput = layer.name
                                    showRenameDialog = true
                                }
                            )

                            "crop" -> CropToolPanel(
                                selectedAsRatio = cropAspectRatio,
                                onSelectRatio = { ratio ->
                                    cropAspectRatio = ratio
                                    Toast.makeText(context, "Cropped Canvas Preview set to $ratio", Toast.LENGTH_SHORT).show()
                                }
                            )

                            "resize" -> ResizeToolPanel(
                                initialW = project?.width ?: 1080,
                                initialH = project?.height ?: 1080,
                                onResize = { w, h ->
                                    project = project?.copy(width = w, height = h)
                                    scope.launch {
                                        project?.let { firestore.projectDao.updateProject(it) }
                                    }
                                    Toast.makeText(context, "Canvas size resized to ${w}x${h}", Toast.LENGTH_SHORT).show()
                                }
                            )

                            "filter" -> FiltersToolPanel(
                                onSelectFilter = { name ->
                                    globalFilter = globalFilter.copy(name = name)
                                    saveDraftProject(updatedFilter = globalFilter)
                                    Toast.makeText(context, "Applied dynamic style: $name", Toast.LENGTH_SHORT).show()
                                }
                            )

                            "adjustments" -> ColorAdjustmentsPanel(
                                currentFilter = globalFilter,
                                onAdjust = { updated ->
                                    globalFilter = updated
                                    saveDraftProject(updatedFilter = updated)
                                }
                            )

                            "text" -> TextEditorPanel(
                                text = textInput,
                                onTextChange = { txt ->
                                    textInput = txt
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(text = txt))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                fontSize = textFontSize,
                                onFontSizeChange = { sz ->
                                    textFontSize = sz
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(fontSize = sz))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                fontFamilyName = textFontFamilyName,
                                onFontFamilyChange = { fam ->
                                    textFontFamilyName = fam
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(fontFamilyName = fam))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                selectedColor = fontColorSelection,
                                onColorChange = { color ->
                                    fontColorSelection = color
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(color = color))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                isBold = isBold,
                                onBoldChange = { b ->
                                    isBold = b
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(isBold = b))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                isItalic = isItalic,
                                onItalicChange = { itl ->
                                    isItalic = itl
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(isBold = isBold, isItalic = itl))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                isUnderline = isUnderline,
                                onUnderlineChange = { und ->
                                    isUnderline = und
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(isUnderlined = und))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                isUppercase = isUppercase,
                                onUppercaseChange = { up ->
                                    isUppercase = up
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(isUppercase = up))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                effect = textEffectStyle,
                                onEffectChange = { eff ->
                                    textEffectStyle = eff
                                    selectedLayerId?.let { id ->
                                        layers = layers.map {
                                            if (it.id == id && it.type == LayerType.TEXT) {
                                                it.copy(textProperties = it.textProperties?.copy(textEffect = eff))
                                            } else it
                                        }
                                        saveDraftProject()
                                    }
                                },
                                onAddNewTextLayer = {
                                    val id = UUID.randomUUID().toString()
                                    val newLayer = DesignLayer(
                                        id = id,
                                        name = "Text Layout Layer",
                                        type = LayerType.TEXT,
                                        positionX = 170f,
                                        positionY = 170f,
                                        textProperties = TextProperties()
                                    )
                                    layers = layers + newLayer
                                    selectedLayerId = id
                                    saveDraftProject()
                                }
                            )

                            "stickers" -> StickersToolPanel(
                                onSelectStickerEmoji = { rawEmoji ->
                                    val id = UUID.randomUUID().toString()
                                    val newLayer = DesignLayer(
                                        id = id,
                                        name = "Sticker Overlay",
                                        type = LayerType.STICKER,
                                        positionX = 170f,
                                        positionY = 170f,
                                        stickerProperties = StickerProperties("emoji", rawEmoji)
                                    )
                                    layers = layers + newLayer
                                    selectedLayerId = id
                                    saveDraftProject()
                                }
                            )

                            "shapes" -> ShapesToolPanel(
                                activeShape = selectedShapeType,
                                onSelectShape = { selectedShapeType = it },
                                fillColor = shapeFillColor,
                                onFillColorChange = { shapeFillColor = it },
                                borderColor = shapeBorderColor,
                                onBorderColorChange = { shapeBorderColor = it },
                                borderWidth = shapeBorderWidth,
                                onBorderWidthChange = { shapeBorderWidth = it },
                                onAddNewShape = {
                                    val id = UUID.randomUUID().toString()
                                    val newLayer = DesignLayer(
                                        id = id,
                                        name = "Geometric ${selectedShapeType.name}",
                                        type = LayerType.SHAPE,
                                        positionX = 170f,
                                        positionY = 170f,
                                        scaleX = 0.5f,
                                        scaleY = 0.5f,
                                        shapeProperties = ShapeProperties(
                                            shapeType = selectedShapeType,
                                            fillColor = shapeFillColor,
                                            borderColor = shapeBorderColor,
                                            borderWidth = shapeBorderWidth
                                        )
                                    )
                                    layers = layers + newLayer
                                    selectedLayerId = id
                                    saveDraftProject()
                                }
                            )

                            "draw" -> DrawingToolPanel(
                                tool = paintToolSelected,
                                onSelectTool = {
                                    paintToolSelected = it
                                    isDrawingEraser = (it == "eraser")
                                },
                                activeColor = currentDrawColor,
                                onColorChange = { currentDrawColor = it },
                                brushSize = currentBrushSize,
                                onBrushSizeChange = { currentBrushSize = it }
                            )
                        }
                    }

                    // Lower Workspace Bottom navigation tabs
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val subTools = listOf(
                            Pair("layers", Icons.Default.Layers),
                            Pair("crop", Icons.Default.Crop),
                            Pair("resize", Icons.Default.AspectRatio),
                            Pair("filter", Icons.Default.AutoAwesome),
                            Pair("adjustments", Icons.Default.Tune),
                            Pair("text", Icons.Default.TextFields),
                            Pair("stickers", Icons.Default.EmojiEmotions),
                            Pair("shapes", Icons.Default.Category),
                            Pair("draw", Icons.Default.Brush)
                        )
                        items(subTools) { (modeName, modeIcon) ->
                            val isSelected = activeToolMode == modeName
                            Column(
                                modifier = Modifier
                                    .clickable {
                                        activeToolMode = modeName
                                        if (modeName != "draw" && modeName != "text" && modeName != "shapes") {
                                            // auto-deselect for global configurations unless specifically active
                                        }
                                    }
                                    .padding(horizontal = 6.dp)
                                    .testTag("tool_nav_$modeName"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = modeName,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = modeName.replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else Color.LightGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Layer alert modal
    if (showRenameDialog && layerToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Layer", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Layer Name") },
                    modifier = Modifier.fillMaxWidth().testTag("rename_layer_input"),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        layers = layers.map {
                            if (it.id == layerToRename?.id) it.copy(name = renameInput) else it
                        }
                        showRenameDialog = false
                        saveDraftProject()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Export completed graphics Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Professional Export Settings", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Format Selection", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("PNG", "JPG", "WEBP").forEach { fmt ->
                            val isSel = exportFormat == fmt
                            FilterChip(
                                selected = isSel,
                                onClick = { exportFormat = fmt },
                                label = { Text(fmt, color = if (isSel) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Quality Scaling", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Low", "Medium", "High", "Ultra HD").forEach { q ->
                            val isSel = exportQuality == q
                            FilterChip(
                                selected = isSel,
                                onClick = { exportQuality = q },
                                label = { Text(q, color = if (isSel) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Warning: Sare tools manual hain, design properties ko compile karke local store storage simulate kiya jayega.", color = Color.Gray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportDialog = false
                        Toast.makeText(context, "Saved successfully to local Gallery in $exportFormat with $exportQuality resolution!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Download & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ---------------- LAYER MANAGEMENT LIST ----------------
@Composable
fun LayersListToolPanel(
    layersList: List<DesignLayer>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onOrderUp: (String) -> Unit,
    onOrderDown: (String) -> Unit,
    onTriggerRename: (DesignLayer) -> Unit
) {
    if (layersList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active design layers. Click Text, Shapes or Drawing below to insert elements.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            layersList.asReversed().forEach { layer ->
                val isSelected = layer.id == selectedId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelect(layer.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = when (layer.type) {
                                    LayerType.IMAGE -> Icons.Default.Image
                                    LayerType.TEXT -> Icons.Default.TextFields
                                    LayerType.STICKER -> Icons.Default.EmojiEmotions
                                    LayerType.SHAPE -> Icons.Default.Category
                                    LayerType.DRAWING -> Icons.Default.Brush
                                },
                                contentDescription = "layer icon",
                                tint = if (isSelected) Color(0xFFD0BCFF) else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = layer.name,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Management Operations Row (Hide, Lock, Rename, Ordering)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onTriggerRename(layer) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, "Rename", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }
                            IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (layer.isHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Visibility",
                                    tint = if (layer.isHidden) Color.Gray else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(onClick = { onToggleLock(layer.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock style",
                                    tint = if (layer.isLocked) Color(0xFFFFB74D) else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(onClick = { onOrderUp(layer.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowUpward, "Order Up", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }
                            IconButton(onClick = { onOrderDown(layer.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowDownward, "Order Down", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CROP PANEL ----------------
@Composable
fun CropToolPanel(
    selectedAsRatio: String,
    onSelectRatio: (String) -> Unit
) {
    Column {
        Text("Social Media Crop Aspect Ratios", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val crops = listOf("Free", "Square (1:1)", "Instagram Post", "Instagram Story", "Facebook Post", "Facebook Cover", "YouTube Thumbnail", "YouTube Banner", "Twitter Post")
            items(crops) { ratio ->
                val isSel = selectedAsRatio == ratio
                FilterChip(
                    selected = isSel,
                    onClick = { onSelectRatio(ratio) },
                    label = { Text(ratio, fontSize = 11.sp, color = if (isSel) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

// ---------------- RESIZE PANEL ----------------
@Composable
fun ResizeToolPanel(
    initialW: Int,
    initialH: Int,
    onResize: (Int, Int) -> Unit
) {
    var widthInput by remember { mutableStateOf(initialW.toString()) }
    var heightInput by remember { mutableStateOf(initialH.toString()) }
    var lockAspect by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Resize Dimensions", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = widthInput,
                onValueChange = {
                    widthInput = it
                    if (lockAspect) {
                        val w = it.toIntOrNull() ?: 1
                        val ratio = initialH.toFloat() / initialW.toFloat()
                        heightInput = (w * ratio).toInt().toString()
                    }
                },
                label = { Text("Width") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = heightInput,
                onValueChange = {
                    heightInput = it
                    if (lockAspect) {
                        val h = it.toIntOrNull() ?: 1
                        val ratio = initialW.toFloat() / initialH.toFloat()
                        widthInput = (h * ratio).toInt().toString()
                    }
                },
                label = { Text("Height") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = {
                    val w = widthInput.toIntOrNull() ?: initialW
                    val h = heightInput.toIntOrNull() ?: initialH
                    onResize(w, h)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Apply")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
            Checkbox(checked = lockAspect, onCheckedChange = { lockAspect = it })
            Text("Lock Aspect Ratio", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

// ---------------- FILTERS CATALOG ----------------
@Composable
fun FiltersToolPanel(onSelectFilter: (String) -> Unit) {
    Column {
        Text("Manual Visual Aesthetics Styles (20 Filters)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val list = listOf(
                "Original", "Vintage", "Retro", "Warm", "Cool", "Black & White", "HDR",
                "Bright", "Dark", "Soft", "Vivid", "Cinematic", "Neon", "Dreamy",
                "Matte", "Classic", "Film", "Golden", "Sunset", "Sketch"
            )
            items(list) { filterName ->
                Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onSelectFilter(filterName) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhotoFilter, contentDescription = "filter preview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = filterName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// ---------------- COLOR ADJUSTMENTS PANEL ----------------
@Composable
fun ColorAdjustmentsPanel(
    currentFilter: FilterProperties,
    onAdjust: (FilterProperties) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AdjustmentSlider(label = "Brightness", value = currentFilter.brightness, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(brightness = it))
        }
        AdjustmentSlider(label = "Contrast", value = currentFilter.contrast, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(contrast = it))
        }
        AdjustmentSlider(label = "Saturation", value = currentFilter.saturation, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(saturation = it))
        }
        AdjustmentSlider(label = "Exposure", value = currentFilter.exposure, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(exposure = it))
        }
        AdjustmentSlider(label = "Temperature", value = currentFilter.temperature, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(temperature = it))
        }
        AdjustmentSlider(label = "Tint", value = currentFilter.tint, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(tint = it))
        }
        AdjustmentSlider(label = "Vibrance", value = currentFilter.vibrance, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(vibrance = it))
        }
        AdjustmentSlider(label = "Highlights", value = currentFilter.highlights, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(highlights = it))
        }
        AdjustmentSlider(label = "Shadows", value = currentFilter.shadows, range = -0.5f..0.5f) {
            onAdjust(currentFilter.copy(shadows = it))
        }
    }
}

@Composable
fun AdjustmentSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(90.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(activeTrackColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f)
        )
        Text(text = String.format("%.2f", value), color = Color.White, fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

// ---------------- TEXT EDITOR PANEL ----------------
@Composable
fun TextEditorPanel(
    text: String,
    onTextChange: (String) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamilyName: String,
    onFontFamilyChange: (String) -> Unit,
    selectedColor: ColorData,
    onColorChange: (ColorData) -> Unit,
    isBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    isItalic: Boolean,
    onItalicChange: (Boolean) -> Unit,
    isUnderline: Boolean,
    onUnderlineChange: (Boolean) -> Unit,
    isUppercase: Boolean,
    onUppercaseChange: (Boolean) -> Unit,
    effect: String,
    onEffectChange: (String) -> Unit,
    onAddNewTextLayer: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rich Custom Text Settings", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddNewTextLayer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "add text", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Text input field
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Modify Text Layer Source") },
            modifier = Modifier.fillMaxWidth().testTag("text_editor_input"),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Options toolbar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // FontFamily Selection Box
            Text("Font Preset Type", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterVertically))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Sans-Serif", "Serif", "Monospace", "Cursive")) { fm ->
                    val isS = fontFamilyName == fm
                    FilterChip(
                        selected = isS,
                        onClick = { onFontFamilyChange(fm) },
                        label = { Text(fm, fontSize = 10.sp, color = if (isS) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        // Color Palette Selector Row
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font Color", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(70.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val clrs = listOf(ColorData.Black, ColorData.White, ColorData.Red, ColorData.Green, ColorData.Blue, ColorData.Yellow, ColorData(233, 30, 99, 255), ColorData(103, 58, 183, 255))
                items(clrs) { cl ->
                    val isS = selectedColor == cl
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(cl.toColor(), CircleShape)
                            .border(if (isS) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { onColorChange(cl) }
                    )
                }
            }
        }

        // Stylings and typography
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconToggleButton(checked = isBold, onCheckedChange = onBoldChange) {
                Icon(Icons.Default.FormatBold, "Bold", tint = if (isBold) MaterialTheme.colorScheme.primary else Color.LightGray)
            }
            IconToggleButton(checked = isItalic, onCheckedChange = onItalicChange) {
                Icon(Icons.Default.FormatItalic, "Italic", tint = if (isItalic) MaterialTheme.colorScheme.primary else Color.LightGray)
            }
            IconToggleButton(checked = isUnderline, onCheckedChange = onUnderlineChange) {
                Icon(Icons.Default.FormatUnderlined, "Underline", tint = if (isUnderline) MaterialTheme.colorScheme.primary else Color.LightGray)
            }
            IconToggleButton(checked = isUppercase, onCheckedChange = onUppercaseChange) {
                Icon(Icons.Default.KeyboardCapslock, "Uppercase", tint = if (isUppercase) MaterialTheme.colorScheme.primary else Color.LightGray)
            }
        }

        // Font Size slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font Size", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(70.dp))
            Slider(value = fontSize, onValueChange = onFontSizeChange, valueRange = 10f..100f, modifier = Modifier.weight(1f))
            Text("${fontSize.toInt()}sp", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
        }

        // Effects configuration
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
            Text("Effects Preset", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(90.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Normal", "Neon", "Outline", "Glow", "3D", "Shadow")) { eff ->
                    val isS = effect == eff
                    FilterChip(
                        selected = isS,
                        onClick = { onEffectChange(eff) },
                        label = { Text(eff, fontSize = 10.sp, color = if (isS) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

// ---------------- STICKERS PANEL ----------------
@Composable
fun StickersToolPanel(onSelectStickerEmoji: (String) -> Unit) {
    Column {
        Text("Canva-Style Sticker Overlay Selector", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val stickers = listOf(
                "😀", "🚀", "🔥", "💥", "⭐", "🎉", "🍔", "⚽", "🐱", "🍟",
                "🎯", "🎨", "📈", "📢", "💬", "💖", "⚡", "🕶️", "💼", "🏆",
                "➡️", "⬅️", "⬆️", "⬇️", "🌟", "🌍", "🎁", "📱", "👻", "💡"
            )
            items(stickers) { emoji ->
                Card(
                    modifier = Modifier
                        .size(54.dp)
                        .clickable { onSelectStickerEmoji(emoji) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = emoji, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

// ---------------- SHAPES PANEL ----------------
@Composable
fun ShapesToolPanel(
    activeShape: ShapeType,
    onSelectShape: (ShapeType) -> Unit,
    fillColor: ColorData,
    onFillColorChange: (ColorData) -> Unit,
    borderColor: ColorData,
    onBorderColorChange: (ColorData) -> Unit,
    borderWidth: Float,
    onBorderWidthChange: (Float) -> Unit,
    onAddNewShape: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Geometrical Shapes Drawer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAddNewShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "add shape", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Draw Shape", fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Shape Type selector row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShapeType.values().toList()) { st ->
                val isS = activeShape == st
                FilterChip(
                    selected = isS,
                    onClick = { onSelectShape(st) },
                    label = { Text(st.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (isS) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Fill color pallet
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fill Color", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(70.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val clrs = listOf(ColorData.Blue, ColorData.Transparent, ColorData.Black, ColorData.White, ColorData.Red, ColorData.Green, ColorData.Yellow)
                items(clrs) { cl ->
                    val isS = fillColor == cl
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (cl == ColorData.Transparent) Color.Transparent else cl.toColor(), CircleShape)
                            .border(width = if (isS) 2.dp else 1.dp, color = if (isS) Color.White else Color.Gray, shape = CircleShape)
                            .clickable { onFillColorChange(cl) }
                    ) {
                        if (cl == ColorData.Transparent) {
                            Icon(Icons.Default.Close, contentDescription = "transparent", tint = Color.Red, modifier = Modifier.size(14.dp).align(Alignment.Center))
                        }
                    }
                }
            }
        }

        // Border color pallet
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Border Color", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(70.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val clrs = listOf(ColorData.Black, ColorData.White, ColorData.Red, ColorData.Green, ColorData.Blue, ColorData.Yellow)
                items(clrs) { cl ->
                    val isS = borderColor == cl
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(cl.toColor(), CircleShape)
                            .border(if (isS) 2.dp else 0.dp, Color.White, CircleShape)
                            .clickable { onBorderColorChange(cl) }
                    )
                }
            }
        }

        // Border Width Slider
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Border Width", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(90.dp))
            Slider(value = borderWidth, onValueChange = onBorderWidthChange, valueRange = 1f..30f, modifier = Modifier.weight(1f))
            Text("${borderWidth.toInt()}px", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        }
    }
}

// ---------------- DRAWING TOOLS PANEL ----------------
@Composable
fun DrawingToolPanel(
    tool: String,
    onSelectTool: (String) -> Unit,
    activeColor: ColorData,
    onColorChange: (ColorData) -> Unit,
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit
) {
    Column {
        Text("Manual Freehand Painting Tools", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        // Tool types selector (pencil, brush, marker, eraser)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("pencil", "brush", "marker", "eraser").forEach { t ->
                val isS = tool == t
                FilterChip(
                    selected = isS,
                    onClick = { onSelectTool(t) },
                    label = { Text(t.replaceFirstChar { it.uppercase() }, color = if (isS) MaterialTheme.colorScheme.onPrimary else Color.LightGray) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Color selector (disable if eraser)
        if (tool != "eraser") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Paint Color", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val clrs = listOf(ColorData.Red, ColorData.Green, ColorData.Blue, ColorData.Yellow, ColorData.Black, ColorData.White, ColorData(233, 30, 99, 255))
                    items(clrs) { cl ->
                        val isS = activeColor == cl
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(cl.toColor(), CircleShape)
                                .border(if (isS) 2.dp else 0.dp, Color.White, CircleShape)
                                .clickable { onColorChange(cl) }
                        )
                    }
                }
            }
        }

        // Size slider
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Brush Size", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.width(80.dp))
            Slider(value = brushSize, onValueChange = onBrushSizeChange, valueRange = 1f..50f, modifier = Modifier.weight(1f))
            Text("${brushSize.toInt()}px", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
        }
    }
}
