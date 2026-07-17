package com.bizane.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.AppSettings
import com.bizane.app.data.AuthManager
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.FoodSyncService
import com.bizane.app.ui.theme.FieldBG
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    vm: FoodViewModel,
    editItem: FoodItem?,
    groupId: String?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(editItem?.name ?: "") }
    var category by remember { mutableStateOf(editItem?.category ?: FoodCategory.FOOD) }
    var purchaseDate by remember { mutableStateOf(editItem?.purchaseDate ?: System.currentTimeMillis()) }
    var expiryDate by remember {
        mutableStateOf(editItem?.expiryDate ?: (System.currentTimeMillis() + 7L * 86_400_000L))
    }
    var notes by remember { mutableStateOf(editItem?.notes ?: "") }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickedBase64 by remember { mutableStateOf(editItem?.imageBase64) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBuyPicker by remember { mutableStateOf(false) }
    var showExpPicker by remember { mutableStateOf(false) }
    var showImageSourceSheet by remember { mutableStateOf(false) }

    // خۆکار: بۆنی ئایتمە هەڵگیراوەکە لادراو e، تەنیا کاتێک هی ئەندامێکی ترە لە گروپدا
    val isReadOnly = remember(editItem) {
        val item = editItem
        item != null && groupId != null && item.ownerId != null && item.ownerId != AuthManager.uid
    }
    val canDelete = remember(editItem, vm.deleteUnlockedState.value) {
        val item = editItem
        if (item == null || groupId == null) true
        else vm.canModify(item)
    }

    // Load existing image bitmap
    LaunchedEffect(editItem) {
        editItem?.imageBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                pickedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) { }
        }
    }

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun onImagePicked(bmp: Bitmap) {
        pickedBitmap = bmp
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
        pickedBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        // OCR: خۆکار ناوی خواردن پڕبکەوە ئەگەر خانەکە بەتاڵ بێت
        if (name.isBlank()) runOcr(bmp) { text -> if (name.isBlank()) name = text }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bmp = uriToBitmap(context, it)
            if (bmp != null) onImagePicked(bmp)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val bmp = uriToBitmap(context, uri)
                if (bmp != null) onImagePicked(bmp)
            }
        }
    }

    fun launchCamera() {
        // پێویستە فایلەکە لەناو بوخچەی "images/" دروست بکرێت، چونکە file_paths.xml
        // تەنیا ئەو ڕێڕەوە دەناسێتەوە (کرش دەکات ئەگەر لە ڕیشەی cacheDir دروست بکرێت)
        val imagesDir = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
        val file = File.createTempFile("bizane_", ".jpg", imagesDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else errorMsg = "ڕێگەی کامێرا پێویستە بۆ گرتنی وێنە"
    }

    fun requestCameraAndLaunch() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) launchCamera() else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    Scaffold(
        containerColor = com.bizane.app.ui.theme.PageBG,
        topBar = {
            TopAppBar(
                title = { Text(if (isReadOnly) "تەنیا بینین" else if (editItem != null) "دەستکاری بکە" else "زیاد بکە", color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("داخستن", color = Color.White) }
                },
                actions = {
                    if (!isReadOnly) {
                        TextButton(onClick = {
                            if (groupId != null && !AppSettings.canEditGroup) {
                                errorMsg = "ئەدمینی گروپ ڕێگەی زیادکردن/دەستکاریکردنی نەداویت (تەنیا بینین)."
                                return@TextButton
                            }
                            if (name.trim().isEmpty()) {
                                errorMsg = "تکایە ناوی خواردنەکە بنووسە"; return@TextButton
                            }
                            if (expiryDate <= purchaseDate) {
                                errorMsg = "بەرواری بەسەرچون دەبێت لەدوای بەرواری کڕین بێت"; return@TextButton
                            }
                            if (editItem != null) {
                                val updated = editItem.copy(
                                    name = name.trim(), category = category,
                                    purchaseDate = purchaseDate, expiryDate = expiryDate,
                                    notes = notes, imageBase64 = pickedBase64 ?: editItem.imageBase64
                                )
                                if (groupId != null) FoodSyncService.save(groupId, updated) { vm.refreshAfterEdit() }
                                else { FoodStorage.update(updated); vm.refreshAfterEdit() }
                            } else {
                                var newItem = FoodItem(
                                    name = name.trim(), category = category,
                                    purchaseDate = purchaseDate, expiryDate = expiryDate,
                                    imageBase64 = pickedBase64, notes = notes
                                )
                                if (groupId != null) {
                                    newItem = newItem.copy(
                                        ownerId = AuthManager.uid,
                                        ownerName = AppSettings.userName.ifEmpty { "ئەندام" }
                                    )
                                    FoodSyncService.save(groupId, newItem) { vm.refreshAfterEdit() }
                                } else {
                                    FoodStorage.add(newItem); vm.refreshAfterEdit()
                                }
                            }
                            onClose()
                        }) { Text("پاشەکەوت", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.bizane.app.ui.theme.PageBG)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isReadOnly) {
                Text(
                    "🔒  ئەم ئایتمە هی ئەندامێکی ترە — تەنیا دەتوانیت بیبینیت",
                    color = Color(0xFFFF9500), fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(14.dp))
            }
            errorMsg?.let {
                Text(it, color = Color(0xFFFF3B30), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
            }

            // Image picker
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(150.dp, 100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FieldBG)
                        .clickable(enabled = !isReadOnly) { showImageSourceSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (pickedBitmap != null) {
                        Image(
                            bitmap = pickedBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Gray)
                            Spacer(Modifier.height(6.dp))
                            Text("وێنەی خواردنەکە", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                if (pickedBitmap != null && !isReadOnly) {
                    Box(
                        modifier = Modifier
                            .padding(start = 110.dp, top = 62.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { showImageSourceSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("ناوی خواردن")
            OutlinedTextField(
                value = name, onValueChange = { name = it }, enabled = !isReadOnly,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel("جۆری خواردن")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(FoodCategory.selectable) { cat ->
                    CategoryChip(cat, selected = cat == category) { if (!isReadOnly) category = cat }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("بەرواری کڕین")
            DateField(purchaseDate, enabled = !isReadOnly) { showBuyPicker = true }

            Spacer(Modifier.height(16.dp))
            SectionLabel("بەرواری بەسەرچون")
            DateField(expiryDate, enabled = !isReadOnly) { showExpPicker = true }

            Spacer(Modifier.height(16.dp))
            SectionLabel("تێبینی")
            OutlinedTextField(
                value = notes, onValueChange = { notes = it }, enabled = !isReadOnly,
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = fieldColors()
            )

            if (groupId != null && editItem?.ownerName != null) {
                Spacer(Modifier.height(10.dp))
                Text("👤 زیادکراوە لەلایەن: ${editItem.ownerName}", color = Color.Gray, fontSize = 12.sp)
            }

            if (editItem != null && canDelete) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldBG),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🗑  سڕینەوە", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showImageSourceSheet) {
        AlertDialog(
            onDismissRequest = { showImageSourceSheet = false },
            title = { Text("وێنەی خواردنەکە") },
            text = { Text("سەرچاوەیەک هەڵبژێرە") },
            confirmButton = {
                TextButton(onClick = { showImageSourceSheet = false; requestCameraAndLaunch() }) { Text("📷 کامێرا") }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceSheet = false; galleryLauncher.launch("image/*") }) { Text("🖼 گەلەری وێنەکان") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("دڵنیایی؟") },
            text = { Text("ئایا دەتەوێت بیسڕیتەوە؟") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    editItem?.let { item ->
                        if (groupId != null && item.firestoreId != null) {
                            val myName = AppSettings.userName.ifEmpty { "ئەندام" }
                            FoodSyncService.delete(groupId, item, myName)
                        } else {
                            FoodStorage.delete(item.id)
                        }
                        vm.refreshAfterEdit()
                    }
                    onClose()
                }) { Text("بەڵێ، بسڕەوە", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("نەخێر") } }
        )
    }

    if (showBuyPicker) {
        DatePickDialog(initial = purchaseDate, onDismiss = { showBuyPicker = false }) {
            purchaseDate = it; showBuyPicker = false
        }
    }
    if (showExpPicker) {
        DatePickDialog(initial = expiryDate, onDismiss = { showExpPicker = false }) {
            expiryDate = it; showExpPicker = false
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DateField(millis: Long, enabled: Boolean, onClick: () -> Unit) {
    val fmt = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FieldBG)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(fmt.format(Date(millis)), color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = FieldBG, unfocusedContainerColor = FieldBG, disabledContainerColor = FieldBG,
    focusedTextColor = Color.White, unfocusedTextColor = Color.White, disabledTextColor = Color.White.copy(alpha = 0.6f),
    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it) } ?: onDismiss() }) { Text("باشە") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("پاشگەزبوونەوە") } }
    ) {
        DatePicker(state = state)
    }
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = try {
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
} catch (e: Exception) { null }

/** دەقی سەر وێنەکە دەخوێنێتەوە (وەکو Vision OCR ـی iOS) و بەخۆکاری دەیخاتە ناو ناوی خواردن */
private fun runOcr(bitmap: Bitmap, onResult: (String) -> Unit) {
    try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val bestLine = visionText.textBlocks
                    .flatMap { it.lines }
                    .maxByOrNull { it.text.length }
                    ?.text
                if (!bestLine.isNullOrBlank()) onResult(bestLine)
            }
    } catch (e: Exception) { /* OCR ئارەزوومەندانەیە، ناکات هیچ کێشەیەک دروست بکات */ }
}
