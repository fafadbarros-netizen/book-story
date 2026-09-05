package ua.acclorite.book_story.ui.settings.appearance.colors.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.acclorite.book_story.R
import ua.acclorite.book_story.domain.model.reader.ColorPreset
import ua.acclorite.book_story.presentation.settings.SettingsEvent
import ua.acclorite.book_story.presentation.settings.SettingsModel
import ua.acclorite.book_story.ui.common.components.common.StyledText
import ua.acclorite.book_story.ui.settings.components.SettingsSubcategoryTitle

@Immutable
data class CuratedTheme(
    val titleRes: Int,
    val backgroundColor: Color,
    val fontColor: Color
)

val CURATED_THEMES = listOf(
    CuratedTheme(
        titleRes = R.string.theme_classic_white,
        backgroundColor = Color(0xFFFFFFFF),
        fontColor = Color(0xFF1E1E1E)
    ),
    CuratedTheme(
        titleRes = R.string.theme_warm_sepia,
        backgroundColor = Color(0xFFFBF0D9),
        fontColor = Color(0xFF382C1E)
    ),
    CuratedTheme(
        titleRes = R.string.theme_soft_mint,
        backgroundColor = Color(0xFFE4EEE6),
        fontColor = Color(0xFF1B2E24)
    ),
    CuratedTheme(
        titleRes = R.string.theme_slate_dark,
        backgroundColor = Color(0xFF232528),
        fontColor = Color(0xFFE1E4E8)
    ),
    CuratedTheme(
        titleRes = R.string.theme_pure_black,
        backgroundColor = Color(0xFF000000),
        fontColor = Color(0xFFECECEC)
    ),
    CuratedTheme(
        titleRes = R.string.theme_midnight_blue,
        backgroundColor = Color(0xFF0F172A),
        fontColor = Color(0xFFCBD5E1)
    )
)

@Composable
fun ColorPresetOption(backgroundColor: Color) {
    val settingsModel = hiltViewModel<SettingsModel>()
    val state = settingsModel.state.collectAsStateWithLifecycle()
    val selectedPreset = state.value.selectedColorPreset

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SettingsSubcategoryTitle(
            title = stringResource(id = R.string.theme_presets),
            padding = 18.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Curated Theme Swatch Cards Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(CURATED_THEMES) { theme ->
                val isSelected = remember(
                    selectedPreset.backgroundColor,
                    selectedPreset.fontColor,
                    theme.backgroundColor,
                    theme.fontColor
                ) {
                    selectedPreset.backgroundColor.value == theme.backgroundColor.value &&
                            selectedPreset.fontColor.value == theme.fontColor.value
                }

                ThemeCard(
                    theme = theme,
                    isSelected = isSelected,
                    onClick = {
                        val currentPreset = state.value.selectedColorPreset
                        settingsModel.onEvent(
                            SettingsEvent.OnUpdateColorPresetColor(
                                id = currentPreset.id,
                                backgroundColor = theme.backgroundColor,
                                fontColor = theme.fontColor
                            )
                        )
                        settingsModel.onEvent(
                            SettingsEvent.OnSelectColorPreset(currentPreset.id)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: CuratedTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "theme_card_border"
    )

    Surface(
        modifier = Modifier
            .width(105.dp)
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = theme.backgroundColor,
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StyledText(
                    text = "Aa",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = theme.fontColor,
                        fontSize = 20.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                StyledText(
                    text = stringResource(id = theme.titleRes),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = theme.fontColor.copy(alpha = 0.85f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}