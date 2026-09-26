package com.hakunakuinama.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hakunakuinama.app.domain.model.BudgetSummary
import com.hakunakuinama.app.domain.model.GroceryItem
import com.hakunakuinama.app.domain.model.GroceryPlan
import com.hakunakuinama.app.domain.model.Weeks
import com.hakunakuinama.app.domain.usecase.GenerateGroceryListUseCase
import com.hakunakuinama.app.domain.usecase.GetBudgetSummaryUseCase
import com.hakunakuinama.app.domain.usecase.GetWeeklyGroceryListUseCase
import com.hakunakuinama.app.domain.usecase.SetGroceryItemCheckedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the shopping-list bottom sheet on the recipe screen.
 *
 * [isSheetVisible] lives in the ViewModel rather than in the composable so the sheet
 * survives rotation: a list the user is halfway through working through should not
 * vanish because they turned the phone.
 */
data class GroceryListUiState(
    val isLoading: Boolean = true,
    val items: List<GroceryItem> = emptyList(),
    val summary: BudgetSummary = BudgetSummary(),
    val isSheetVisible: Boolean = false,
    val errorCause: Throwable? = null,
) {
    val isEmpty: Boolean get() = !isLoading && errorCause == null && items.isEmpty()
}

sealed interface GroceryListEvent {
    data class ListGenerated(val lineCount: Int) : GroceryListEvent
    data object GenerateFailed : GroceryListEvent
}

/**
 * Owns the weekly shopping list.
 *
 * The week is resolved once, from the injected [Clock], at construction. Recomputing it
 * per emission would mean a list that silently rolls over to a new week mid-session if
 * the app were left open across Sunday night — and a user would find their ticked-off
 * shopping gone.
 */
@HiltViewModel
class GroceryListViewModel @Inject constructor(
    getWeeklyGroceryList: GetWeeklyGroceryListUseCase,
    getBudgetSummary: GetBudgetSummaryUseCase,
    private val generateGroceryList: GenerateGroceryListUseCase,
    private val setGroceryItemChecked: SetGroceryItemCheckedUseCase,
    clock: Clock,
) : ViewModel() {

    private val weekStart: LocalDate = Weeks.startOf(LocalDate.now(clock))

    private val isSheetVisible = MutableStateFlow(false)

    private val _events = Channel<GroceryListEvent>(Channel.BUFFERED)
    val events: Flow<GroceryListEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<GroceryListUiState> = combine(
        getWeeklyGroceryList(weekStart),
        getBudgetSummary(weekStart),
        isSheetVisible,
    ) { items, summary, sheetVisible ->
        GroceryListUiState(
            isLoading = false,
            items = items,
            summary = summary,
            isSheetVisible = sheetVisible,
        )
    }
        .catch { cause -> emit(GroceryListUiState(isLoading = false, errorCause = cause)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = GroceryListUiState(),
        )

    /** Replaces the week with a list built from this recipe, then reveals the sheet. */
    fun onGenerateForRecipe(mealId: Long, servings: Int = 1) {
        viewModelScope.launch {
            generateGroceryList(
                GroceryPlan(
                    mealIds = listOf(mealId),
                    weekStart = weekStart,
                    servingsPerMeal = servings,
                ),
            )
                .onSuccess { lineCount ->
                    isSheetVisible.value = true
                    _events.send(GroceryListEvent.ListGenerated(lineCount))
                }
                .onFailure { _events.send(GroceryListEvent.GenerateFailed) }
        }
    }

    fun onSheetRequested() {
        isSheetVisible.value = true
    }

    fun onSheetDismissed() {
        isSheetVisible.value = false
    }

    fun onItemChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch { setGroceryItemChecked(itemId, checked) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
