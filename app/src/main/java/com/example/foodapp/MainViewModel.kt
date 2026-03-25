package com.example.foodapp

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    var currentMeals: List<Meal> = emptyList()
    var currentIndex: Int = 0
    val likedMeals = mutableListOf<Meal>()

    var isHomeVisible: Boolean = true

    var lastSelectedIngredient: String = ""
}