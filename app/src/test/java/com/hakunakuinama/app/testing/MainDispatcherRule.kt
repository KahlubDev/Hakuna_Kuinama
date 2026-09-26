package com.hakunakuinama.app.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Installs a test dispatcher as `Dispatchers.Main` for the duration of a test.
 *
 * Required because `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`. Without
 * this, constructing any ViewModel in a unit test throws
 * "Module with the Main dispatcher is missing" — which is a confusing way to learn that
 * `viewModelScope` exists.
 *
 * Exposes [testDispatcher] so a test can use the *same* scheduler for virtual time; a
 * second independent scheduler would make `advanceTimeBy` a no-op from the ViewModel's
 * point of view.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
