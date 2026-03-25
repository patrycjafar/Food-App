package com.example.foodapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- 1. MODELE DANYCH ---
data class MealResponse(val meals: List<Meal>?)
data class Meal(
    val idMeal: String,
    val strMeal: String,
    val strMealThumb: String
)

// --- 2. POŁĄCZENIE API ---
interface TheMealDbApi {
    @GET("filter.php")
    fun getMealsByIngredient(@Query("i") ingredient: String): Call<MealResponse>
}

// --- 3. ADAPTER DO LISTY POLUBIONYCH ---
class LikedMealsAdapter(private val meals: List<Meal>) :
    RecyclerView.Adapter<LikedMealsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val ivThumb: ImageView = view.findViewById(R.id.ivThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = meals[position]
        holder.tvName.text = meal.strMeal
        Glide.with(holder.itemView.context).load(meal.strMealThumb).into(holder.ivThumb)
    }

    override fun getItemCount() = meals.size
}

// --- 4. GŁÓWNA AKTYWNOŚĆ ---
class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var layoutHome: LinearLayout
    private lateinit var layoutLiked: LinearLayout
    private lateinit var ivDishImage: ImageView
    private lateinit var tvDishName: TextView
    private lateinit var spinner: Spinner
    private lateinit var adapterLiked: LikedMealsAdapter

    // Logic Variables
    private lateinit var viewModel: MainViewModel
    private val ingredients = listOf("Chicken", "Beef", "Pork", "Potato", "Cheese", "Salmon")

    override fun onCreate(savedInstanceState: Bundle?) {
        // Sprawdzanie czy jest zapisany czarny motyw
        val isDarkSaved = loadThemePreference()
        if (isDarkSaved) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // Wszystkie findViewById
        drawerLayout = findViewById(R.id.drawerLayout)
        layoutHome = findViewById(R.id.layoutHome)
        layoutLiked = findViewById(R.id.layoutLiked)
        ivDishImage = findViewById(R.id.ivDishImage)
        tvDishName = findViewById(R.id.tvDishName)
        spinner = findViewById(R.id.ingredientSpinner)

        val btnReject: View = findViewById(R.id.btnReject)
        val btnLike: View = findViewById(R.id.btnLike)
        val btnMenu: View = findViewById(R.id.btnMenu)
        val navView: NavigationView = findViewById(R.id.navigationView)
        val recyclerLiked: RecyclerView = findViewById(R.id.recyclerViewLiked)

        // Logika przełącznika
        val darkModeItem = navView.menu.findItem(R.id.nav_dark_mode)
        val themeSwitch = darkModeItem.actionView as androidx.appcompat.widget.SwitchCompat
        themeSwitch.isChecked = isDarkSaved

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveThemePreference(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Konfiguracja API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.themealdb.com/api/json/v1/1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(TheMealDbApi::class.java)

        // Konfiguracja Spinnera (Wybór składnika)
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ingredients)
        spinner.adapter = spinnerAdapter

        // MIEJSCE NAPRAWY 1: Poprawna implementacja OnItemSelectedListener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedIngredient = ingredients[position]

                if (viewModel.lastSelectedIngredient != selectedIngredient || viewModel.currentMeals.isEmpty()) {
                    viewModel.lastSelectedIngredient = selectedIngredient
                    fetchMeals(api, selectedIngredient)
                } else {
                    loadDishUI()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        // MIEJSCE NAPRAWY 2: Kod wyciągnięty POZA spinner
        // Konfiguracja Listy Polubionych
        adapterLiked = LikedMealsAdapter(viewModel.likedMeals)
        recyclerLiked.layoutManager = LinearLayoutManager(this)
        recyclerLiked.adapter = adapterLiked

        // Obsługa Menu Bocznego (Drawer)
        btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    layoutHome.visibility = View.VISIBLE
                    layoutLiked.visibility = View.GONE
                    viewModel.isHomeVisible = true
                }
                R.id.nav_liked -> {
                    layoutHome.visibility = View.GONE
                    layoutLiked.visibility = View.VISIBLE
                    viewModel.isHomeVisible = false
                    adapterLiked.notifyDataSetChanged()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Logika Przycisków (Tinder)
        btnReject.setOnClickListener {
            loadNextDish()
        }

        btnLike.setOnClickListener {
            if (viewModel.currentIndex < viewModel.currentMeals.size) {
                viewModel.likedMeals.add(viewModel.currentMeals[viewModel.currentIndex])
                Toast.makeText(this, "Polubiono!", Toast.LENGTH_SHORT).show()
                loadNextDish()
            }
        }

        // Przywracanie stanu po obrocie / zmianie motywu
        if (viewModel.isHomeVisible) {
            layoutHome.visibility = View.VISIBLE
            layoutLiked.visibility = View.GONE
        } else {
            layoutHome.visibility = View.GONE
            layoutLiked.visibility = View.VISIBLE
        }
    } // <-- MIEJSCE NAPRAWY 3: Tutaj zamykamy funkcję onCreate()

    // MIEJSCE NAPRAWY 4: Funkcje pomocnicze wrzucone do klasy MainActivity, POZA onCreate()

    private fun fetchMeals(api: TheMealDbApi, ingredient: String) {
        viewModel.currentIndex = 0
        tvDishName.text = "Ładowanie..."
        ivDishImage.setImageResource(android.R.drawable.ic_menu_gallery)

        api.getMealsByIngredient(ingredient).enqueue(object : Callback<MealResponse> {
            override fun onResponse(call: Call<MealResponse>, response: Response<MealResponse>) {
                if (response.isSuccessful && response.body()?.meals != null) {
                    viewModel.currentMeals = response.body()!!.meals!!
                    loadDishUI()
                } else {
                    tvDishName.text = "Brak dań z tym składnikiem"
                }
            }

            override fun onFailure(call: Call<MealResponse>, t: Throwable) {
                tvDishName.text = "Błąd połączenia"
            }
        })
    }

    private fun loadNextDish() {
        viewModel.currentIndex++
        loadDishUI()
    }

    private fun loadDishUI() {
        if (viewModel.currentIndex < viewModel.currentMeals.size) {
            val meal = viewModel.currentMeals[viewModel.currentIndex]
            tvDishName.text = meal.strMeal
            Glide.with(this).load(meal.strMealThumb).into(ivDishImage)
        } else {
            tvDishName.text = "To już wszystko w tej kategorii!"
            ivDishImage.setImageResource(android.R.drawable.ic_menu_info_details)
        }
    }

    private fun saveThemePreference(isDarkMode: Boolean) {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean("dark_mode", isDarkMode)
            apply()
        }
    }

    private fun loadThemePreference(): Boolean {
        val sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE)
        return sharedPreferences.getBoolean("dark_mode", false)
    }
}