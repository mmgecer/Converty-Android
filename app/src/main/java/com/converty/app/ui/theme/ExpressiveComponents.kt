@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.converty.app.ui.theme

import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Keeps the alpha Expressive navigation API at the design-system boundary.
 * Screen code should call this wrapper rather than Material 3 alpha APIs directly.
 */
@Composable
fun ExpressiveNavigationBar(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    content: @Composable () -> Unit,
) {
    ShortNavigationBar(
        modifier = modifier,
        arrangement = if (centered) {
            ShortNavigationBarArrangement.Centered
        } else {
            ShortNavigationBarArrangement.EqualWeight
        },
        content = content,
    )
}

@Composable
fun ExpressiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    horizontal: Boolean = false,
) {
    ShortNavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label,
        modifier = modifier,
        enabled = enabled,
        iconPosition = if (horizontal) {
            NavigationItemIconPosition.Start
        } else {
            NavigationItemIconPosition.Top
        },
    )
}

/** Null progress means indeterminate. Values outside 0..1 are safely clamped. */
@Composable
fun ExpressiveLinearProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
) {
    if (progress == null) {
        LinearWavyProgressIndicator(
            modifier = modifier,
            amplitude = if (motionEnabled) 1f else 0f,
        )
    } else {
        LinearWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier,
            amplitude = { if (motionEnabled) 1f else 0f },
        )
    }
}

/** Null progress means indeterminate. Values outside 0..1 are safely clamped. */
@Composable
fun ExpressiveCircularProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
) {
    if (progress == null) {
        CircularWavyProgressIndicator(
            modifier = modifier,
            amplitude = if (motionEnabled) 1f else 0f,
        )
    } else {
        CircularWavyProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier,
            amplitude = { if (motionEnabled) 1f else 0f },
        )
    }
}
