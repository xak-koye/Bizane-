package com.bizane.app.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.data.FoodCategory
import com.bizane.app.data.FoodItem
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.ChipBG
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("M/d/yy", Locale.US)

@Composable
fun rememberItemBitmap(base64: String?) = remember(base64) {
    if (base64.isNullOrEmpty()) null
    else try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) { null }
}

@Composable
fun CategoryChip(cat: FoodCategory, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color.White else ChipBG)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            "${cat.emoji}  ${cat.raw}",
            color = if (selected) Color(0xFF12121A) else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun FoodListRow(item: FoodItem, isPending: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bmp = rememberItemBitmap(item.imageBase64)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBG)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF333333)),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(item.category.emoji, fontSize = 30.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
                if (isPending) {
                    Spacer(Modifier.width(6.dp))
                    PendingSyncBadge()
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.statusText, color = Color(item.statusColor), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                var dateText = "${dateFmt.format(Date(item.purchaseDate))} ~ ${dateFmt.format(Date(item.expiryDate))}"
                item.ownerName?.let { dateText += "  ·  👤 $it" }
                Text(dateText, color = Color.Gray, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(Color(0xFF383838))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color(item.statusColor))
                )
            }
        }
    }
}

/** نیشانەیەکی بچووک بۆ ئایتمێک کە هێشتا نەگەیشتووەتە گروپ (چاوەڕوانی ناردنە) */
@Composable
fun PendingSyncBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A2E00))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("⏳ چاوەڕوانە", color = Color(0xFFFFCC00), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FoodCard(item: FoodItem, isPending: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bmp = rememberItemBitmap(item.imageBase64)
    Box(
        modifier = modifier
            .aspectRatio(1f / 1.35f)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBG)
            .clickable(onClick = onClick)
    ) {
        if (bmp != null) {
            androidx.compose.foundation.Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(
                item.category.emoji,
                fontSize = 52.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (isPending) {
            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                PendingSyncBadge()
            }
        }
        // dark gradient overlay at bottom for text legibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).fillMaxWidth()) {
            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                var statusText = item.statusText
                item.ownerName?.let { statusText += " · 👤$it" }
                Text(statusText, color = Color(item.statusColor), fontSize = 12.sp, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(item.statusColor))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        " ${maxOf(item.daysLeft, 0)}رۆژ ",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF595959))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(item.statusColor))
                )
            }
        }
    }
}

@Composable
fun IconCircleButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(com.bizane.app.ui.theme.IconBtnBG)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}
