package com.tasteindia.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * A recipe image that cannot collapse the layout it sits in.
 *
 * Loading, failure and "no URL at all" all render at the same size as a success,
 * so a broken image never changes row height or shifts the list.
 *
 * [contentDescription] is null wherever the meal's name is already visible
 * beside the image — announcing "Chicken Handi" twice is worse for a screen
 * reader user than announcing it once. The details hero passes a real
 * description because there the image carries information the title does not.
 */
@Composable
fun MealImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Int = 28,
) {
    val placeholderModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surfaceVariant)

    if (url == null) {
        ImageFallback(placeholderModifier, iconSize)
        return
    }

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(placeholderModifier)
        },
        error = {
            ImageFallback(placeholderModifier, iconSize)
        },
    )
}

@Composable
private fun ImageFallback(modifier: Modifier, iconSize: Int) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Outlined.RestaurantMenu,
            // Decorative: the enclosing MealImage already carries whatever
            // description is appropriate for its context, so labelling the
            // fallback icon too would announce the image twice.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}
