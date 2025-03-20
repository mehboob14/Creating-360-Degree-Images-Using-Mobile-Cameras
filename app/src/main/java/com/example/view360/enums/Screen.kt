package com.example.view360.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector


enum class Screen(val route: String, val icon: ImageVector) {
    Home("home", Icons.Default.Home),
    MultiDevice("Multi-Device",Icons.Default.ShoppingCart),
    Guide("guide",Icons.Default.AccountBox),
    Profile("profile", Icons.Default.Person)
}
