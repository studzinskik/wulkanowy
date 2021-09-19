package io.github.wulkanowy.utils

import android.view.Menu
import android.view.MenuItem
import androidx.core.view.forEach
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.github.wulkanowy.R
import timber.log.Timber

fun MenuItem.navigateToConnectedAction(navController: NavController): Boolean {
    return try {
        navController.navigate(
            itemId, null, NavOptions.Builder()
                .setEnterAnim(android.R.anim.fade_in)
                .setExitAnim(android.R.anim.fade_out)
                .setPopUpTo(R.id.main_nav_graph, false)
                .build()
        )
        true
    } catch (e: Exception) {
        Timber.e(e, "Error in bottom navigation view")
        false
    }
}

fun Menu.uncheckAllItems() {
    setGroupCheckable(0, true, false)
    forEach { it.isChecked = false }
    setGroupCheckable(0, true, true)
}
