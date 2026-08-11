package com.tripnext.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripnext.app.ui.AppViewModel
import com.tripnext.app.ui.TripNextApp
import com.tripnext.app.ui.theme.TripNextTheme
import com.tripnext.app.ui.theme.TripVisualTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val quickExpense = intent?.data?.host == "quick-expense"
        setContent {
            val preferences = remember { getSharedPreferences("tripnext_appearance", MODE_PRIVATE) }
            var visualTheme by remember { mutableStateOf(TripVisualTheme.fromKey(preferences.getString("theme", null))) }
            TripNextTheme(visualTheme) {
                val vm: AppViewModel = viewModel(factory = AppViewModel.Factory((application as TripNextApplication).repository))
                TripNextApp(vm, startWithQuickExpense = quickExpense, visualTheme = visualTheme) { selected ->
                    visualTheme = selected
                    preferences.edit().putString("theme", selected.storageKey).apply()
                }
            }
        }
    }
}
