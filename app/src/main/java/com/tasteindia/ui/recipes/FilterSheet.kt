package com.tasteindia.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tasteindia.R
import com.tasteindia.domain.model.FilterState
import com.tasteindia.domain.model.SortOrder
import com.tasteindia.ui.theme.TasteIndiaTheme

/**
 * All filter controls in one sheet.
 *
 * Changes apply immediately rather than behind an Apply button — with the data
 * already local, filtering is instantaneous, so making the user confirm would
 * add a step without adding certainty. "Clear all" and "Done" are both present;
 * the applied state stays visible as chips on the list behind the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    state: FilterState,
    categoryOptions: List<String>,
    ingredientOptions: List<String>,
    onCategoryChange: (String?) -> Unit,
    onIngredientChange: (String?) -> Unit,
    onFavouritesOnlyChange: (Boolean) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.filters_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                TextButton(
                    onClick = onClearAll,
                    enabled = state.hasActiveFilters,
                ) {
                    Text(stringResource(R.string.filters_clear_all))
                }
            }

            FilterSection(title = stringResource(R.string.filter_sort)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SortOrder.entries.forEachIndexed { index, order ->
                        SegmentedButton(
                            selected = state.sortOrder == order,
                            onClick = { onSortOrderChange(order) },
                            shape = SegmentedButtonDefaults.itemShape(index, SortOrder.entries.size),
                        ) {
                            Text(
                                when (order) {
                                    SortOrder.NAME_ASC -> stringResource(R.string.sort_name_asc)
                                    SortOrder.NAME_DESC -> stringResource(R.string.sort_name_desc)
                                },
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            FilterSection(title = stringResource(R.string.filter_favourites_only)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.filter_favourites_only_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.favouritesOnly,
                        onCheckedChange = onFavouritesOnlyChange,
                        modifier = Modifier.semantics {
                            role = Role.Switch
                        },
                    )
                }
            }

            HorizontalDivider()

            if (categoryOptions.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.filter_category)) {
                    SelectableChipGroup(
                        options = categoryOptions,
                        selected = state.category,
                        onSelect = onCategoryChange,
                    )
                }
                HorizontalDivider()
            }

            if (ingredientOptions.isNotEmpty()) {
                FilterSection(title = stringResource(R.string.filter_ingredient)) {
                    SelectableChipGroup(
                        options = ingredientOptions,
                        selected = state.ingredient,
                        onSelect = onIngredientChange,
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.filters_done))
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        content()
    }
}

/**
 * A single-select chip group with an explicit "Any" option.
 *
 * Selection is carried by a leading checkmark as well as the container colour —
 * the brief rules out communicating filter state by colour alone, and a
 * checkmark is also what a screen reader picks up as the selected state.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SelectableChipGroup(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.filter_any)) },
            leadingIcon = if (selected == null) { { SelectedCheck() } } else null,
        )
        options.forEach { option ->
            val isSelected = option.equals(selected, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else option) },
                label = { Text(option) },
                leadingIcon = if (isSelected) { { SelectedCheck() } } else null,
            )
        }
    }
}

@Composable
private fun SelectedCheck() {
    Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Preview
@Composable
private fun FilterSheetContentPreview() {
    TasteIndiaTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            SelectableChipGroup(
                options = listOf("Chicken", "Dessert", "Vegetarian", "Lamb"),
                selected = "Dessert",
                onSelect = {},
            )
        }
    }
}
