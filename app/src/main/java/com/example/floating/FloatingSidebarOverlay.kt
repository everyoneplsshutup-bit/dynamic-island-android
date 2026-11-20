package com.example.floating

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.DynamicIslandApp
import com.example.search.LaunchableApp
import com.example.system.IslandSettings
import com.example.ui.theme.IslandColors

@Composable
fun FloatingSidebarOverlay(
    app: DynamicIslandApp,
    settings: IslandSettings,
    accentColor: Color,
    onClose: () -> Unit
) {
    var allApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    val lastResult by app.floatingWindowManager.lastResult.collectAsState()

    LaunchedEffect(Unit) {
        allApps = app.searchEngine.getInstalledApps()
    }

    val selectedApps = remember(allApps, settings.floatingAppPackages) {
        if (settings.floatingAppPackages.isEmpty()) {
            allApps.take(8)
        } else {
            val map = allApps.associateBy { it.packageName }
            settings.floatingAppPackages.mapNotNull { map[it] }
        }
    }

    Column(
        modifier = Modifier
            .width(236.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xF00B0B10))
            .border(1.dp, IslandColors.GlassSpecularBorder, RoundedCornerShape(26.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Floating Apps",
                color = IslandColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close sidebar", tint = IslandColors.TextSecondary)
            }
        }

        Text(
            text = if (lastResult != null) {
                lastResult!!.pathUsed.title
            } else {
                "Pick floating apps in Settings → Features"
            },
            color = accentColor,
            fontSize = 10.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.height(220.dp)) {
            items(selectedApps, key = { it.packageName }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            app.floatingWindowManager.launchAppInFloatingWindow(item.packageName)
                            onClose()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bmp = remember(item.packageName) {
                        item.icon?.toBitmap(64, 64)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(bitmap = bmp, contentDescription = item.label, modifier = Modifier.size(28.dp).clip(CircleShape))
                    } else {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.label,
                        color = IslandColors.TextPrimary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = IslandColors.TextSecondary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
