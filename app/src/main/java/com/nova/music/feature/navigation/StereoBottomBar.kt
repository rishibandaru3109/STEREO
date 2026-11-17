package com.nova.music.feature.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.music.core.designsystem.theme.StereoTheme

@Composable
fun StereoBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StereoTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.surfaceElevated)
            .border(
                width = 0.5.dp,
                color = colors.surfaceHighlight,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.values().forEach { tab ->
                val isSelected = tab == currentTab

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primaryAccent else colors.textSecondary,
                    animationSpec = tween(durationMillis = 200),
                    label = "iconColor"
                )

                val pillBackground by animateColorAsState(
                    targetValue = if (isSelected) colors.primaryAccent.copy(alpha = 0.12f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "pillBackground"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(pillBackground)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.title,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = iconColor
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
