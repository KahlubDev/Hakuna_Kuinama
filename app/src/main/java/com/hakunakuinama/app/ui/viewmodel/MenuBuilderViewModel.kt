package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.domain.model.Ingredient
import com.hakunakuinama.app.domain.model.MealMatch
import com.hakunakuinama.app.domain.usecase.GetIngredientsBySelectionUseCase
import com.hakunakuinama.app.domain.usecase.GetIngredientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Menu Builder state — one snapshot of the three things the screen renders, so the chips
 * and the ranked list can never be drawn from different generations of the data.
 *
 * **Empty selection means "show a prompt", not "show every recipe".** The whole point of
 * this screen is answering "what can I cook with what I already have?". Listing all five
 * meals on open makes it look like a plain menu list and teaches the user nothing; the
 * prompt ("tap what you have") is what teaches the feature. The UI can derive the branch
 * from [selectedIngredientIds] being empty, so no extra field is needed.
 */
data class MenuBuilderUiState(
    val isLoading: Boolean = true,
    val ingredients: List<Ingredient> = emptyList(),
    val selectedIngredientIds: Set<Long> = emptySet(),
    val matches: List<MealMatch> = emptyList(),
    val errorCause: Throwable? = null,
) {
    val showSelectionPrompt: Boolean
        get() = !isLoading && errorCause == null && selectedIngredientIds.isEmpty()

    val showNoResults: Boolean
        get() = !isLoading && errorCause == null && selectedIngredientIds.isNotEmpty() && matches.isEmpty()
}

@HiltViewModel
class MenuBuilderViewModel @Inject constructor(
    getIngredients: GetIngredientsUseCase,
    private val getIngredientsBySelection: GetIngredientsBySelectionUseCase,
) : ViewModel() {

    /**
     * The selection lives here, not in the composable, so it survives tab switches,
     * configuration changes and process death — the user should not lose their ticks
     * because they peeked at a recipe and came back.
     */
    private val _selectedIngredientIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIngredientIds: StateFlow<Set<Long>> = _selectedIngredientIds.asStateFlow()

    val uiState: StateFlow<MenuBuilderUiState> = combine(
        getIngredients(),
        _selectedIngredientIds,
        getIngredientsBySelection.observe(_selectedIngredientIds.map { it.toList() }),
    ) { ingredients, selected, matches ->
        MenuBuilderUiState(
            isLoading = false,
            ingredients = ingredients,
            selectedIngredientIds = selected,
            matches = matches,
        )
    }
        .catch { cause -> emit(MenuBuilderUiState(isLoading = false, errorCause = cause)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MenuBuilderUiState(),
        )

    fun onIngredientToggled(ingredientId: Long) {
        _selectedIngredientIds.update { selected ->
            if (ingredientId in selected) selected - ingredientId else selected + ingredientId
        }
    }

    fun onSelectionCleared() {
        _selectedIngredientIds.value = emptySet()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
