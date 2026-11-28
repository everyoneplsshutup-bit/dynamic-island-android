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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

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
    SETTINGS("Prefs", Icons.Filled.Settings)
}

@Composable
fun ObsidianCrimsonBackground(
    modifier: Modifier = Modifier,
    showHorizonPlanet: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val appBackdrop = rememberLayerBackdrop()
    val accent = MaterialTheme.colorScheme.primary
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

            // Subtle neutral-grey + user-accent ambient atmosphere over #121212
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (showHorizonPlanet) 0.14f else 0.10f),
                        Color(0x0EFFFFFF),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.08f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.85f, h * 0.08f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.18f, h * 0.92f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.18f, h * 0.92f)
            )
        }

        CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
            content()
        }
    }
}

@Composable
fun ObsidianGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    highlighted: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bgBrush = Brush.verticalGradient(
        colors = if (highlighted) {
            listOf(Color(0xFF23252C), Color(0xFF1A1B20))
        } else {
            listOf(Color(0xFF1D1D21), Color(0xFF161619))
        }
    )
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.85f))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.14f),
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
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.24f),
                        Color(0xFF222227),
                        Color(0xFF18181C)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(tint.copy(alpha = 0.52f), Color.White.copy(alpha = 0.12f))
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
    iconSize: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
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
                        tint.copy(alpha = if (isActive) 0.34f else 0.16f),
                        Color(0xFF232328),
                        Color(0xFF18181C)
                    )
                )
            )
            .border(
                width = if (isActive) 1.4.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isActive) tint else Color.White.copy(alpha = 0.18f),
                        tint.copy(alpha = if (isActive) 0.65f else 0.16f)
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
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(IslandColors.ObsidianCard)
                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
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
            CrimsonSquircleIcon(icon = icon, tint = accent)
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = IslandColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = IslandColors.TextSecondary,
                fontSize = 13.sp,
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
    val accent = MaterialTheme.colorScheme.primary
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF28282E),
            disabledContentColor = Color.White.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
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
    val accent = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = IslandColors.ObsidianCard,
            contentColor = IslandColors.TextPrimary
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun FloatingGlassPillDock(
    selectedTab: MainDockTab,
    onSelectTab: (MainDockTab) -> Unit,
    modifier: Modifier = Modifier,
    pillStyle: com.example.system.PillStyle = com.example.system.PillStyle.APPLE_LIQUID,
    glassDensity: Float = 0.65f,
    reduceGlass: Boolean = false
) {
    val accent = MaterialTheme.colorScheme.primary
    val backdrop = LocalAppBackdrop.current
    val dockShape = RoundedCornerShape(28.dp)
    val tabs = MainDockTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    var prevIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(selectedIndex) }
    val movingRight = selectedIndex >= prevIndex
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        prevIndex = selectedIndex
    }

    val leftFracState = androidx.compose.animation.core.animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (movingRight) MotionTokens.dockTrailingSpring() else MotionTokens.dockLeadingSpring(),
        label = "dock_pill_left"
    )
    val rightFracState = androidx.compose.animation.core.animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (movingRight) MotionTokens.dockLeadingSpring() else MotionTokens.dockTrailingSpring(),
        label = "dock_pill_right"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .islandGlassChrome(
                shape = dockShape,
                pillStyle = pillStyle,
                glassDensity = glassDensity,
                reduceGlass = reduceGlass,
                solidFallback = false,
                accentColor = accent,
                isHighlighted = false,
                backdrop = backdrop,
                isRegularCard = false
            )
            .testTag("floating_glass_bottom_dock")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Sliding glowing liquid pill indicator underneath active tab
            Canvas(
                modifier = Modifier
                    .matchParentSize()
            ) {
                val count = tabs.size.coerceAtLeast(1)
                val slotW = size.width / count
                val rawL = kotlin.math.min(leftFracState.value, rightFracState.value)
                val rawR = kotlin.math.max(leftFracState.value, rightFracState.value)
                val leftX = rawL * slotW + 2.dp.toPx()
                val rightX = (rawR + 1f) * slotW - 2.dp.toPx()
                val pillW = (rightX - leftX).coerceAtLeast(slotW * 0.72f)
                val radiusPx = (size.height / 2f).coerceAtMost(20.dp.toPx())

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.24f),
                            accent.copy(alpha = 0.36f),
                            accent.copy(alpha = 0.24f)
                        ),
                        startX = leftX,
                        endX = leftX + pillW
                    ),
                    topLeft = Offset(leftX, 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(pillW, size.height - 2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radiusPx, radiusPx)
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.75f),
                    topLeft = Offset(leftX + pillW * 0.24f, size.height - 3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(pillW * 0.52f, 2.5.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val iconScaleState = androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) MotionTokens.DOCK_ACTIVE_ICON_SCALE else 1f,
                        animationSpec = MotionTokens.dockIconScaleSpring(),
                        label = "dock_icon_scale_${tab.name}"
                    )
                    val itemColor = if (isSelected) Color.White else IslandColors.TextMuted
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 4.dp)
                            .graphicsLayer {
                                alpha = if (isSelected) 1f else MotionTokens.DOCK_INACTIVE_ALPHA
                            }
                            .testTag("dock_tab_${tab.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) accent else itemColor,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = iconScaleState.value
                                    scaleY = iconScaleState.value
                                }
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = itemColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingGlassBottomDock(
    selectedTab: MainDockTab,
    onSelectTab: (MainDockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingGlassPillDock(
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
        modifier = modifier
    )
}
