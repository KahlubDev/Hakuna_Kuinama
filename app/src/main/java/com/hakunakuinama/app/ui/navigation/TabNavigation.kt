package com.hakunakuinama.app.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Switches tabs, with one back stack shape for every entry point.
 *
 * The bar's items and the Home screen's "build a meal" button both want the same thing —
 * land on the Menu Builder as a *tab*, not as a new screen pushed on top of Home. Two
 * separate `navigate` calls doing that job is how a back stack quietly grows to
 * [home, builder, builder]: a plain `navigate` with no `launchSingleTop` pushes a second
 * copy every time, and BACK then has to be pressed twice to leave a screen the user
 * believes they only entered once. So there is one function, and every caller uses it.
 *
 * The three flags are the documented bottom-navigation contract:
 *
 *  - `popUpTo(findStartDestination().id) { saveState = true }` unwinds to the start
 *    destination and parks each tab's scroll position and state under its own key. The
 *    **node id**, not the route string: the start destination is a node in the graph, and
 *    passing a route pattern is only equivalent while the graph is flat. It stops working
 *    the moment a nested graph is added, which is exactly the kind of thing that reads as
 *    "back is broken" on a user's phone and as nothing at all in a test.
 *  - `launchSingleTop = true` makes re-tapping the current tab a no-op instead of a second
 *    copy of the same screen.
 *  - `restoreState = true` puts back the parked scroll position, so returning to a long
 *    list lands where the user left it.
 */
fun NavController.navigateToTab(route: Route) {
    navigate(route.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
