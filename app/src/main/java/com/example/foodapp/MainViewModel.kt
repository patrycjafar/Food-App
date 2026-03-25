package com.example.foodapp // Upewnij się, że masz tu swój pakiet!

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    // Przenosimy tutaj zmienne, które mają przetrwać zmianę motywu
    var currentMeals: List<Meal> = emptyList()
    var currentIndex: Int = 0
    val likedMeals = mutableListOf<Meal>()

    // Do zapamiętania, czy otwarty jest ekran Home czy Liked
    var isHomeVisible: Boolean = true

    // Zabezpieczenie, żeby Spinner nie pobierał danych od nowa przy obrocie ekranu/zmianie motywu
    var lastSelectedIngredient: String = ""
}