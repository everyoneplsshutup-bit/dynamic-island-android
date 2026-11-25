package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MainDockTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    NOTES("Notes", Icons.Filled.Description),
    ALERTS("Alerts", Icons.Filled.Notifications),
    SHELF("Shelf", Icons.Filled.Layers),
    TOOLS("Tools", Icons.Filled.Build),
    MUSIC("Music", Icons.Filled.MusicNote),
    TIMER("Timer", Icons.Filled.Timer),
    SEARCH("Search", Icons.Filled.Search),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun ObsidianCrimsonBackground(
    modifier: Modifier = Modifier,
    showHorizonPlanet: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val appBackdrop = rememberLayerBackdrop()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IslandColors.ObsidianBg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(appBackdrop)
        ) {
            val w = size.width
            val h = size.height

            // Top-right & upper ambient crimson nebula glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IslandColors.CrimsonPrimary.copy(alpha = 0.28f),
                        IslandColors.CrimsonDeep.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.86f, h * 0.10f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * 0.86f, h * 0.10f)
            )

            // Bottom ambient crimson wave glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IslandColors.CrimsonPrimary.copy(alpha = 0.18f),
                        IslandColors.CrimsonDeep.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.30f, h * 0.94f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.30f, h * 0.94f)
            )

            if (showHorizonPlanet) {
                val planetRadius = w * 0.72f
                val planetCenter = Offset(w * 0.44f, h * 0.28f + planetRadius * 0.46f)

                // Secondary dark moon upper right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1D090C),
                            Color(0xFF0A0608),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.84f, h * 0.15f),
                        radius = w * 0.085f
                    ),
                    radius = w * 0.075f,
                    center = Offset(w * 0.84f, h * 0.15f)
                )

                // Atmospheric crimson corona behind horizon planet
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            IslandColors.CrimsonBright.copy(alpha = 0.32f),
                            IslandColors.CrimsonPrimary.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(planetCenter.x + w * 0.06f, planetCenter.y - planetRadius * 0.10f),
                        radius = planetRadius * 1.18f
                    ),
                    radius = planetRadius * 1.18f,
                    center = planetCenter
                )

                // Dark planet body
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A0A0E),
                            Color(0xFF090608),
                            Color(0xFF040406)
                        ),
                        center = Offset(planetCenter.x - planetRadius * 0.25f, planetCenter.y - planetRadius * 0.25f),
                        radius = planetRadius
                    ),
                    radius = planetRadius,
                    center = planetCenter
                )

                // Bright crimson lit top horizon arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            IslandColors.CrimsonPrimary.copy(alpha = 0.55f),
                            IslandColors.CrimsonBright.copy(alpha = 0.95f),
                            Color(0xFFFF8890),
                            IslandColors.CrimsonPrimary.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = planetCenter
                    ),
                    startAngle = 205f,
                    sweepAngle = 125f,
                    useCenter = false,
                    topLeft = Offset(planetCenter.x - planetRadius, planetCenter.y - planetRadius),
                    size = androidx.compose.ui.geometry.Size(planetRadius * 2f, planetRadius * 2f),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Upper sweeping silk crimson light-wave
                val topWave = Path().apply {
                    moveTo(-w * 0.08f, h * 0.20f)
                    cubicTo(
                        w * 0.28f, h * 0.12f,
                        w * 0.62f, h * 0.24f,
                        w * 1.08f, h * 0.08f
                    )
                }
                drawPath(
                    path = topWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            IslandColors.CrimsonDeep.copy(alpha = 0.16f),
                            IslandColors.CrimsonBright.copy(alpha = 0.34f),
                            Color(0xFFFF7A85).copy(alpha = 0.42f)
                        )
                    ),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = topWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            IslandColors.CrimsonBright.copy(alpha = 0.82f),
                            Color(0xFFFFA5AC).copy(alpha = 0.92f)
                        )
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Lower sweeping silk crimson light-wave
                val botWave = Path().apply {
                    moveTo(-w * 0.06f, h * 0.90f)
                    cubicTo(
                        w * 0.34f, h * 0.80f,
                        w * 0.70f, h * 0.92f,
                        w * 1.08f, h * 0.72f
                    )
                }
                drawPath(
                    path = botWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            IslandColors.CrimsonDeep.copy(alpha = 0.12f),
                            IslandColors.CrimsonPrimary.copy(alpha = 0.24f),
                            IslandColors.CrimsonBright.copy(alpha = 0.26f)
                        )
                    ),
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = botWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            IslandColors.CrimsonPrimary.copy(alpha = 0.65f),
                            Color(0xFFFF7C87).copy(alpha = 0.70f)
                        )
                    ),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
            content()
        }
    }
}

@Composable
fun ObsidianGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    highlighted: Boolean = false,
    accentColor: Color = IslandColors.CrimsonBright,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bgBrush = Brush.verticalGradient(
        colors = if (highlighted) {
            listOf(Color(0xE6280B10), Color(0xEC120A0D))
        } else {
            listOf(Color(0xD81A1216), Color(0xE40E090B))
        }
    )
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(accentColor, accentColor.copy(alpha = 0.55f), accentColor)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.18f),
                accentColor.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    }

    val clickableMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(if (highlighted) 1.3.dp else 0.9.dp, borderBrush, shape)
            .then(clickableMod)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun CrimsonSquircleIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    tint: Color = IslandColors.CrimsonBright,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.34f),
                        Color(0xFF1F080C),
                        Color(0xFF110609)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(tint.copy(alpha = 0.72f), Color.White.copy(alpha = 0.14f))
                ),
                shape = shape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CrimsonCircularHaloIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    iconSize: Dp = 26.dp,
    tint: Color = IslandColors.CrimsonBright,
    isActive: Boolean = false,
    contentDescription: String? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = if (isActive) 0.42f else 0.24f),
                        Color(0xFF1E0B0F),
                        Color(0xFF0E080B)
                    )
                )
            )
            .border(
                width = if (isActive) 1.4.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isActive) tint else Color.White.copy(alpha = 0.24f),
                        tint.copy(alpha = if (isActive) 0.75f else 0.22f)
                    )
                ),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) Color.White else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun MockupScreenHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backTestTag: String? = null,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA191114))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                    .let { if (backTestTag != null) it.testTag(backTestTag) else it }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
        } else {
            CrimsonSquircleIcon(icon = icon)
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = IslandColors.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingAction != null) {
            Spacer(Modifier.width(8.dp))
            trailingAction()
        }
    }
}

@Composable
fun CrimsonPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = IslandColors.CrimsonPrimary,
            contentColor = Color.White,
            disabledContainerColor = IslandColors.CrimsonDeep.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, IslandColors.CrimsonGlow.copy(alpha = 0.72f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun ObsidianPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xCC181014),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = IslandColors.CrimsonBright)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun FloatingGlassBottomDock(
    selectedTab: MainDockTab,
    onSelectTab: (MainDockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dockShape = RoundedCornerShape(24.dp)
    Surface(
        shape = dockShape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(dockShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xEE1A1014), Color(0xF50C080A))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        IslandColors.CrimsonBorder,
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = dockShape
            )
            .testTag("floating_glass_bottom_dock")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainDockTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val itemColor = if (isSelected) IslandColors.CrimsonBright else Color(0xFFA0A2AC)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(vertical = 4.dp)
                        .testTag("dock_tab_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = itemColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = itemColor,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) IslandColors.CrimsonBright else Color.Transparent)
                    )
                }
            }
        }
    }
}
