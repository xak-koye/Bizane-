package com.bizane.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.material.icons.filled.QrCodeScanner
import com.bizane.app.data.AppSettings
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.data.FoodStorage
import com.bizane.app.data.L
import com.bizane.app.data.OpenFoodFactsLookup
import com.bizane.app.data.SharedProductDB
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
    onClose: () -> Unit,
    onScanBarcode: () -> Unit = {}
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
    var barcode by remember { mutableStateOf(editItem?.barcode ?: "") }
    var notifyEnabled by remember { mutableStateOf(editItem?.notifyEnabled ?: false) }
    var notifyDaysBefore by remember { mutableStateOf(editItem?.notifyDaysBefore ?: 1) }
    var showNotifyDaysPicker by remember { mutableStateOf(false) }
    var lookingUpBarcode by remember { mutableStateOf(false) }
    var pickedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickedBase64 by remember { mutableStateOf(editItem?.imageBase64) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBuyPicker by remember { mutableStateOf(false) }
    var showExpPicker by remember { mutableStateOf(false) }
    var showImageSourceSheet by remember { mutableStateOf(false) }

    // کاتێک لە پەڕەی سکانی بارکۆد دەگەڕێینەوە، ئەم کۆدە دەخوێنێتەوە: سەرەتا لە Open Food Facts
    // دەگەڕێت (داتابەیسێکی گشتی جیهانی)، ئەگەر نەدۆزرایەوە (زۆرجار بەرهەمی ناوخۆیی/کوردستانین)
    // دواتر لە SharedProductDB دەگەڕێت (داتابەیسی هاوبەشی خودی ئەپەکە، هاوبەش لەگەڵ iOS).
    LaunchedEffect(BarcodeResultHolder.value) {
        val code = BarcodeResultHolder.value ?: return@LaunchedEffect
        BarcodeResultHolder.value = null
        barcode = code
        lookingUpBarcode = true
        val info = OpenFoodFactsLookup.lookup(code)
        if (info != null) {
            lookingUpBarcode = false
            info.name?.let { name = it }
            info.imageUrl?.let { url ->
                val bmp = OpenFoodFactsLookup.downloadImage(url)
                if (bmp != null) {
                    pickedBitmap = bmp
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                    pickedBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                }
            }
            return@LaunchedEffect
        }
        val shared = SharedProductDB.lookup(code)
        lookingUpBarcode = false
        if (shared == null || (shared.name == null && shared.image == null)) {
            errorMsg = L("add.barcodeNotFound")
            return@LaunchedEffect
        }
        shared.name?.let { name = it }
        shared.image?.let { bmp ->
            pickedBitmap = bmp
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            pickedBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }
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
        // پێویستە فایلەکە لەناو "images/" ی cacheDir دروست بکرێت، چونکە file_paths.xml
        // تەنیا ڕێگە بەو ژێرپۆشەیە دەدات (<cache-path path="images/" />). پێشتر فایلەکە
        // ڕاستەوخۆ لە ڕەگی cacheDir دروست دەکرا، کە وا دەکرد FileProvider.getUriForFile
        // IllegalArgumentException ـی "Failed to find configured root" بدات و ئەپەکە
        // یەکسەر کراش بکات هەر کاتێک کامێرا بەکاردەهێنرا.
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File.createTempFile("bizane_", ".jpg", imagesDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraImageUri = uri
        cameraLauncher.launch(uri)
    }

    // ڕێگەپێدانی کامێرا لە کاتی runtime دا داوا دەکرێت؛ بێ ئەمە بەرنامەکە کراش دەکات
    // (permission لە Manifest دا ڕاگەیەندراوە، بەڵام هەرگیز داوای runtime نەدەکرا)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else errorMsg = L("add.cameraPermissionMsg")
    }

    fun requestCameraAndLaunch() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        containerColor = com.bizane.app.ui.theme.PageBG,
        topBar = {
            TopAppBar(
                title = { Text(if (editItem != null) L("add.editTitle") else L("add.addTitle"), color = Color.White) },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text(L("common.close"), color = Color.White) }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.trim().isEmpty()) {
                            errorMsg = L("add.errNoName"); return@TextButton
                        }
                        if (expiryDate <= purchaseDate) {
                            errorMsg = L("add.errDateOrder"); return@TextButton
                        }
                        if (editItem != null) {
                            val updated = editItem.copy(
                                name = name.trim(), category = category,
                                purchaseDate = purchaseDate, expiryDate = expiryDate,
                                notes = notes, imageBase64 = pickedBase64 ?: editItem.imageBase64,
                                barcode = barcode.trim().ifEmpty { null },
                                notifyEnabled = notifyEnabled, notifyDaysBefore = notifyDaysBefore
                            )
                            FoodStorage.update(updated); vm.refreshAfterEdit()
                        } else {
                            val newItem = FoodItem(
                                name = name.trim(), category = category,
                                purchaseDate = purchaseDate, expiryDate = expiryDate,
                                imageBase64 = pickedBase64, notes = notes,
                                barcode = barcode.trim().ifEmpty { null },
                                notifyEnabled = notifyEnabled, notifyDaysBefore = notifyDaysBefore
                            )
                            FoodStorage.add(newItem); vm.refreshAfterEdit()
                        }
                        // ئەگەر بارکۆدێکی هەبوو، ناو و وێنەکە بنێرە بۆ داتابەیسی هاوبەش (لە
                        // پاشبنەمادا، بێ چاوەڕوانی وەڵام) — تاوەکو بەکارهێنەرانی تر (Android یان
                        // iOS) کە دواتر هەمان بارکۆد سکان دەکەن پێویستیان بە دووبارە نووسینەوەی
                        // هەمان زانیاری نەبێت.
                        val cleanBarcode = barcode.trim().ifEmpty { null }
                        if (cleanBarcode != null) {
                            SharedProductDB.submit(cleanBarcode, name.trim(), pickedBitmap)
                        }
                        onClose()
                    }) { Text(L("common.save"), color = Color.White, fontWeight = FontWeight.Bold) }
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
                        .clickable { showImageSourceSheet = true },
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
                            Text(L("add.photoLabel"), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                if (pickedBitmap != null) {
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
            SectionLabel(L("add.namePlaceholder"))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel(L("add.barcodeLabel"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = barcode, onValueChange = { barcode = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text(L("add.barcodePlaceholder"), color = Color.Gray) },
                    colors = fieldColors()
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldBG)
                        .clickable(enabled = !lookingUpBarcode) { onScanBarcode() },
                    contentAlignment = Alignment.Center
                ) {
                    if (lookingUpBarcode) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White
                        )
                    } else {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L("add.categoryLabel"))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(FoodCategory.selectable) { cat ->
                    CategoryChip(cat, selected = cat == category) { category = cat }
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L("add.purchaseDateLabel"))
            DateField(purchaseDate, enabled = true) { showBuyPicker = true }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L("add.expiryDateLabel"))
            DateField(expiryDate, enabled = true) { showExpPicker = true }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L("add.notesLabel"))
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = fieldColors()
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L("add.notifyLabel"), color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                androidx.compose.material3.Switch(
                    checked = notifyEnabled,
                    onCheckedChange = { notifyEnabled = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Color(0xFF33D976))
                )
            }
            if (notifyEnabled) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FieldBG)
                        .clickable { showNotifyDaysPicker = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (notifyDaysBefore == 0) L("add.daysToday") else L("add.daysBefore", notifyDaysBefore),
                        color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f)
                    )
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.ChevronRight,
                        contentDescription = null, tint = Color.Gray
                    )
                }
            }

            if (editItem != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FieldBG),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(L("add.deleteBtn"), color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (showImageSourceSheet) {
        AlertDialog(
            onDismissRequest = { showImageSourceSheet = false },
            title = { Text(L("add.photoLabel")) },
            text = { Text(L("add.gallery")) },
            confirmButton = {
                TextButton(onClick = { showImageSourceSheet = false; requestCameraAndLaunch() }) { Text(L("add.camera")) }
            },
            dismissButton = {
                TextButton(onClick = { showImageSourceSheet = false; galleryLauncher.launch("image/*") }) { Text(L("add.gallery")) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(L("common.areYouSure")) },
            text = { Text(L("add.deleteConfirmMsg")) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    editItem?.let { item ->
                        FoodStorage.delete(item.id)
                        vm.refreshAfterEdit()
                    }
                    onClose()
                }) { Text(L("common.yesDelete"), color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(L("common.no")) } }
        )
    }

    if (showNotifyDaysPicker) {
        NotifyDaysPickerDialog(
            initial = notifyDaysBefore,
            onDismiss = { showNotifyDaysPicker = false },
            onPick = { notifyDaysBefore = it; showNotifyDaysPicker = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotifyDaysPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L("add.notifyLabel")) },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(260.dp)) {
                items((0..30).toList()) { d ->
                    val isSelected = d == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = d }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (d == 0) L("add.daysToday") else L("add.daysBefore", d),
                            color = if (isSelected) Color(0xFF0A84FF) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(selected) }) { Text(L("common.ok")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("common.cancel")) } }
    )
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
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it) } ?: onDismiss() }) { Text(L("common.ok")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(L("common.cancel")) } }
    ) {
        DatePicker(state = state)
    }
}

/**
 * وێنە لە URI دەخوێنێتەوە بۆ Bitmap — بە downsampling، چونکە کامێرای مۆبایلە نوێکان وێنەی
 * زۆر گەورە دەگرن (١٢-٥٠+ مێگاپیکسڵ) کە ئەگەر ڕاستەوخۆ بە قەبارەی تەواو بخوێنرێتەوە دەبێتە
 * هۆی OutOfMemoryError و کراشی ئەپەکە. لێرە یەکەم جار تەنیا قەبارەکەی دەزانین (inJustDecodeBounds)،
 * پاشان بە inSampleSize گونجاو وێنەکە بچووک دەکەینەوە پێش خوێندنەوەی ڕاستەقینە.
 */
private fun uriToBitmap(context: Context, uri: Uri, maxDimension: Int = 1600): Bitmap? {
    return try {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        var sampleSize = 1
        val (w, h) = boundsOptions.outWidth to boundsOptions.outHeight
        if (w > 0 && h > 0) {
            while (w / sampleSize > maxDimension || h / sampleSize > maxDimension) sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    } catch (e: Exception) {
        null
    } catch (e: OutOfMemoryError) {
        // وێنەکە زۆر گەورەیە تەنانەت لەگەڵ sampling — لەبری کراشکردن، هیچ وێنەیەک نیشان نادەین
        null
    }
}

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
