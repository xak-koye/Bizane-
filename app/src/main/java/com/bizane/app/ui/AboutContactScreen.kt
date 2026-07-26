package com.bizane.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bizane.app.R
import com.bizane.app.data.L
import com.bizane.app.ui.theme.CardBG
import com.bizane.app.ui.theme.PageBG

/** پەڕەی "دەربارە و پەیوەندی" — لە ڕێکخستنەکانەوە دەکرێتەوە، هاوشێوەی AboutContactViewController ـی iOS */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutContactScreen(onClose: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = PageBG,
        topBar = {
            TopAppBar(
                title = { Text(L("settings.aboutContactSection"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = L("common.close"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBG)
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
            SectionHeaderAC(L("settings.aboutSection"))
            AboutCard()

            Spacer(Modifier.height(20.dp))
            SectionHeaderAC(L("settings.contactSection"))
            ContactCard(
                title = L("settings.facebook"),
                subtitle = L("settings.facebookSub"),
                bg = Color(0xFF1877F2),
                iconText = "f"
            ) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/khoshawe1/")))
            }

            Spacer(Modifier.height(12.dp))
            ContactCard(
                title = L("settings.github"),
                subtitle = L("settings.githubSub"),
                bg = Color(0xFF141414),
                iconSymbol = Icons.Filled.Code
            ) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/xak-koye")))
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SectionHeaderAC(text: String) {
    Text(text, color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AboutCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBG)
            .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.developer_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(84.dp).clip(CircleShape).background(Color(0xFF333333))
            )
            Spacer(Modifier.height(12.dp))
            Text(L("settings.appName"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(L("settings.version"), color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(1.dp).background(Color(0xFF383838)))
            Spacer(Modifier.height(14.dp))
            Text(
                L("settings.madeByTeam"), color = Color.Gray,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                L("settings.purpose"), color = Color.Gray,
                fontSize = 12.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(L("settings.developer"), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ContactCard(
    title: String,
    subtitle: String,
    bg: Color,
    iconText: String? = null,
    iconSymbol: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBG)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(bg),
                contentAlignment = Alignment.Center
            ) {
                if (iconText != null) {
                    Text(iconText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                } else if (iconSymbol != null) {
                    Icon(iconSymbol, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}
