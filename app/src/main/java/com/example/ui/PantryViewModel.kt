package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MealRecipe
import com.example.data.model.PantryItem
import com.example.data.model.StorageLocation
import com.example.data.repository.FoodApiRepository
import com.example.data.repository.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PantryViewModel(application: Application) : AndroidViewModel(application) {

    private val pantryRepository: PantryRepository
    private val foodApiRepository = FoodApiRepository()

    init {
        val db = AppDatabase.getDatabase(application)
        pantryRepository = PantryRepository(db.pantryDao())
        viewModelScope.launch {
            pantryRepository.seedSampleDataIfEmpty()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLocation = MutableStateFlow<StorageLocation?>(null)
    val selectedLocation: StateFlow<StorageLocation?> = _selectedLocation.asStateFlow()

    val rawActiveItems: StateFlow<List<PantryItem>> = pantryRepository.activeItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringSoonItems: StateFlow<List<PantryItem>> = pantryRepository.getExpiringSoonItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consumedItems: StateFlow<List<PantryItem>> = pantryRepository.consumedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wastedItems: StateFlow<List<PantryItem>> = pantryRepository.wastedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined filtered pantry items
    val filteredItems: StateFlow<List<PantryItem>> = combine(
        rawActiveItems,
        _searchQuery,
        _selectedLocation
    ) { items, query, location ->
        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.brand.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.barcode.contains(query)
            val matchesLocation = location == null || item.storageLocation == location.name
            matchesQuery && matchesLocation
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Scanner state
    private val _isSearchingBarcode = MutableStateFlow(false)
    val isSearchingBarcode: StateFlow<Boolean> = _isSearchingBarcode.asStateFlow()

    private val _scannedItem = MutableStateFlow<PantryItem?>(null)
    val scannedItem: StateFlow<PantryItem?> = _scannedItem.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Recipes state
    private val _matchedRecipes = MutableStateFlow<List<MealRecipe>>(emptyList())
    val matchedRecipes: StateFlow<List<MealRecipe>> = _matchedRecipes.asStateFlow()

    private val _isLoadingRecipes = MutableStateFlow(false)
    val isLoadingRecipes: StateFlow<Boolean> = _isLoadingRecipes.asStateFlow()

    private val _selectedRecipeForDetail = MutableStateFlow<MealRecipe?>(null)
    val selectedRecipeForDetail: StateFlow<MealRecipe?> = _selectedRecipeForDetail.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<PantryItem?>(null)
    val selectedItemForDetail: StateFlow<PantryItem?> = _selectedItemForDetail.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLocation(location: StorageLocation?) {
        _selectedLocation.value = location
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun clearScannedItem() {
        _scannedItem.value = null
        _scanError.value = null
    }

    fun selectRecipeForDetail(recipe: MealRecipe?) {
        _selectedRecipeForDetail.value = recipe
    }

    fun selectItemForDetail(item: PantryItem?) {
        _selectedItemForDetail.value = item
    }

    fun searchBarcode(barcode: String, onResult: ((PantryItem?) -> Unit)? = null) {
        val trimmed = barcode.trim()
        if (trimmed.isBlank()) return

        _isSearchingBarcode.value = true
        _scanError.value = null

        viewModelScope.launch {
            val result = foodApiRepository.fetchProductByBarcode(trimmed)
            _isSearchingBarcode.value = false
            if (result.isSuccess) {
                val item = result.getOrNull()
                _scannedItem.value = item
                onResult?.invoke(item)
            } else {
                _scanError.value = result.exceptionOrNull()?.localizedMessage ?: "Product not found"
                onResult?.invoke(null)
            }
        }
    }

    fun savePantryItem(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.insertItem(item)
            _scannedItem.value = null
            _userMessage.value = "Added ${item.name} to pantry!"
        }
    }

    fun updatePantryItem(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.updateItem(item)
            _selectedItemForDetail.value = null
            _userMessage.value = "Updated ${item.name}!"
        }
    }

    fun markConsumed(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.markAsConsumed(item.id)
            if (_selectedItemForDetail.value?.id == item.id) {
                _selectedItemForDetail.value = null
            }
            _userMessage.value = "Awesome! Saved ${item.name} from food waste."
        }
    }

    fun markWasted(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.markAsWasted(item.id)
            if (_selectedItemForDetail.value?.id == item.id) {
                _selectedItemForDetail.value = null
            }
            _userMessage.value = "${item.name} marked as expired."
        }
    }

    fun deleteItem(item: PantryItem) {
        viewModelScope.launch {
            pantryRepository.deleteItem(item.id)
            if (_selectedItemForDetail.value?.id == item.id) {
                _selectedItemForDetail.value = null
            }
            _userMessage.value = "Removed ${item.name}."
        }
    }

    fun loadRescueRecipes() {
        val expiring = expiringSoonItems.value
        val itemsToUse = if (expiring.isNotEmpty()) expiring else rawActiveItems.value.take(3)
        if (itemsToUse.isEmpty()) {
            _matchedRecipes.value = emptyList()
            return
        }

        _isLoadingRecipes.value = true
        viewModelScope.launch {
            val recipes = foodApiRepository.fetchRecipesForExpiringItems(itemsToUse)
            _matchedRecipes.value = recipes
            _isLoadingRecipes.value = false
        }
    }
}
