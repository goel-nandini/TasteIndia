package com.tasteindia.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasteindia.R
import com.tasteindia.domain.model.Meal
import com.tasteindia.ui.theme.TasteIndiaTheme

/**
 * One recipe in a list. Used unchanged by both Recipes and Favourites.
 *
 * Semantics are merged at the row so a screen reader announces the meal once and
 * treats the row as a single target; the favourite button opts back out with its
 * own label, because it is a separate action with its own state.
 */
@Composable
fun MealRow(
    meal: Meal,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MealImage(
            url = meal.thumbnailUrl,
            // The name is announced by the text beside it; repeating it here
            // would make the row read its title twice.
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // Enrichment only: shown once the lookup response has actually
            // arrived, never guessed from the list endpoint.
            val subtitle = listOfNotNull(meal.category, meal.area).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        FavouriteButton(
            isFavourite = meal.isFavourite,
            mealName = meal.name,
            onToggle = onToggleFavourite,
        )
    }
}

/**
 * Favourite toggle.
 *
 * State is carried by both the icon shape (filled vs outlined) and the
 * accessibility label, so it is never communicated by colour alone. The 48dp
 * minimum keeps it a comfortable touch target inside a dense row.
 */
@Composable
fun FavouriteButton(
    isFavourite: Boolean,
    mealName: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isFavourite) {
        stringResource(R.string.favourite_remove, mealName)
    } else {
        stringResource(R.string.favourite_add, mealName)
    }

    IconButton(
        onClick = onToggle,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Icon(
            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = label,
            tint = if (isFavourite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

private val previewMeal = Meal(
    id = "52795",
    name = "Chicken Handi",
    thumbnailUrl = null,
    category = "Chicken",
    area = "Indian",
    instructions = "Heat oil in a pan…",
    ingredients = emptyList(),
    tags = emptyList(),
    sourceUrl = null,
    youtubeUrl = null,
    detailsLoaded = true,
    isFavourite = false,
)

@Preview(showBackground = true)
@Composable
private fun MealRowPreview() {
    TasteIndiaTheme {
        MealRow(meal = previewMeal, onClick = {}, onToggleFavourite = {})
    }
}

@Preview(name = "Favourited, no image, long name", showBackground = true)
@Composable
private fun MealRowFavouritePreview() {
    TasteIndiaTheme {
        MealRow(
            meal = previewMeal.copy(
                name = "Chicken Handi with Fresh Coriander and Roasted Spices",
                isFavourite = true,
            ),
            onClick = {},
            onToggleFavourite = {},
        )
    }
}

@Preview(name = "Summary only — details not yet loaded", showBackground = true)
@Composable
private fun MealRowSummaryPreview() {
    TasteIndiaTheme {
        MealRow(
            meal = previewMeal.copy(category = null, area = null, detailsLoaded = false),
            onClick = {},
            onToggleFavourite = {},
        )
    }
}
