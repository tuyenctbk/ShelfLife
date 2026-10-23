# ShelfLife: Smart Pantry & Expiry Radar

ShelfLife is an intelligent pantry management and food-waste prevention Android app (sister app to ClaimVault and OpenKitchen). It tracks grocery expiration dates via barcode scanning, displays an active Expiry Radar for foods nearing expiration (within 3 days), and automatically queries TheMealDB to recommend recipes that consume those exact ingredients before they spoil.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed based on your preferences:

- **Confirmed Decision 1 (Storage Engine)**: Local on-device persistence powered by Android **Room Database** (with Kotlin Coroutines & Flow), guaranteeing instant responsiveness, offline accessibility, and zero account friction.
- **Confirmed Decision 2 (Barcode & Entry Mode)**: Full-featured **CameraX live scanner** accompanied by quick sample barcode chips, instant barcode manual input, and product search via OpenFoodFacts API.
- **Confirmed Decision 3 (Expiry Radar & Recipes)**: Prominent **Expiry Radar** dashboard highlighting foods expiring within 3 days (or expired), integrated directly with **TheMealDB API** to automatically search recipes matching those ingredients, plus USDA FoodData Central nutrition insights.

---

### 1. Overview & Core Concept

- **What It Does**:
  - Scans or looks up grocery products using **OpenFoodFacts API** (fetching product titles, brands, packaging, ingredient lists, Nutri-Score, and product photos).
  - Categorizes inventory by storage zones: **Fridge**, **Pantry**, and **Freezer**.
  - Tracks shelf-life countdowns with color-coded badges (Green = Fresh, Amber = Expiring in 3 Days, Red = Expiring Today / Expired).
  - Provides the **Expiry Radar**: an automated intelligence banner that aggregates items expiring within 3 days and queries **TheMealDB** to suggest appetizing recipes using those specific ingredients.
  - Features USDA FoodData Central nutrient profiles for common pantry staples.
  - Allows marking items as consumed or wasted to track monthly waste reduction stats.

- **Target Persona**:
  - Home cooks, grocery shoppers, and environmentally conscious households looking to cut grocery bills and eliminate spoiled food waste.

- **Key Value**:
  - Eliminates "forgotten food" in the back of the pantry and turns imminent waste into dinner ideas within seconds.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Pantry Hub & Expiry Radar**:
   - Header with quick summary metrics: Total Items, Expiring in 3 Days, Freshness Score.
   - **Active Expiry Radar Banner**: If any item is within 3 days of expiring, a pulsating radar alert card appears with one-tap "Find Rescue Recipes" powered by TheMealDB.
   - Filter chips: All, Fridge, Pantry, Freezer, Expiring Soon.
   - Search bar for quick filtering across all items.
2. **Barcode Scanner & Item Intake**:
   - Camera view with animated scanner target box.
   - Quick preset barcodes for popular items (Milk, Eggs, Pasta, Canned Tomatoes, Bread, Greek Yogurt, Apples, Spinach) for instant testing in cloud emulator.
   - Manual barcode input / text search dialog.
   - Upon detection, OpenFoodFacts details are pulled into a pre-filled item card (Name, Brand, Category, Image, Default Expiry suggestion based on storage type).
3. **Recipe Rescue Screen (TheMealDB Integration)**:
   - Triggered from the Expiry Radar or recipe tab.
   - Fetches meals using ingredients expiring soon (e.g., `chicken_breast`, `tomato`, `egg`, `cheese`, `milk`, `potato`).
   - Detailed recipe modal: meal preview image, category/cuisine tags, full ingredient & measurement checklist, step-by-step instructions, and external video link.
4. **Item Details & Nutrition (USDA / OpenFoodFacts)**:
   - Full nutritional breakdown (Calories, Protein, Carbs, Fats, Nutri-Score).
   - "Mark as Consumed" or "Trash" with confirmation, updating waste prevention metrics.

#### Visual Identity & Theme
- **Theme Direction**: Fresh culinary modernism—inviting, clean, and legible.
- **Color Palette**:
  - Primary / Fresh Accent: Emerald Forest (`#15803D`, `#16A34A`, `#22C55E`)
  - Warning / Expiry Amber: Saffron Gold (`#D97706`, `#F59E0B`, `#FEF3C7`)
  - Urgent Alert: Crimson Coral (`#DC2626`, `#F87171`)
  - Surface & Background: Crisp organic cream/slate surfaces with soft rounded cards (`16.dp` to `24.dp` corners).
- **Typography & Layout**:
  - Clean M3 typography hierarchy with bold headers and legible data metrics.
  - Responsive layout supporting handheld phones, foldables, and tablets with `WindowInsets` safe padding.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Retrofit Integration with OpenFoodFacts & TheMealDB**
  - *Chosen Approach*: Official public REST endpoints using Retrofit and Moshi JSON parsing, with OkHttp caching and fallback parsing.
  - *Why*: Provides real-world product lookups for hundreds of thousands of grocery items and rich culinary recipes without requiring mock data or simulated responses.
  - *Alternatives Considered*: Hardcoded mock catalog; rejected because real APIs provide authentic user value.
- **Decision 2: Hybrid Scanner (CameraX + Preloaded Barcode Presets + Manual Code Entry)**
  - *Chosen Approach*: Implement a full CameraX live preview with camera permissions flow, accompanied by instant quick-select barcodes (popular groceries) and manual digit entry.
  - *Why*: Emulator environments lack real physical grocery barcodes in front of a webcam; presets and manual input ensure 100% testability while preserving live camera capabilities on physical devices.
- **Decision 3: Room Database with Reactive Flow Architecture**
  - *Chosen Approach*: Room `@Entity` with DAO returning `Flow<List<PantryItem>>`, exposed through `PantryViewModel` with `StateFlow`.
  - *Why*: Immediate UI reactivity, zero latency, and persistent local storage that survives app restarts.

---

### 4. Technical Architecture & Data Strategy

#### System & Component Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                       Jetpack Compose UI                     │
│  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────┐  │
│  │ PantryHomeScreen│  │ ScannerScreen    │  │RecipeScreen │  │
│  │ (Expiry Radar)  │  │ (CameraX/Manual) │  │ (TheMealDB) │  │
│  └────────┬────────┘  └────────┬─────────┘  └──────┬──────┘  │
└───────────┼────────────────────┼───────────────────┼─────────┘
            │                    │                   │
            ▼                    ▼                   ▼
┌──────────────────────────────────────────────────────────────┐
│                       PantryViewModel                        │
│   • UI State Flow (pantry items, expiring items, filters)    │
│   • Recipe Suggestions State (TheMealDB matched recipes)     │
│   • Barcode Resolution State (OpenFoodFacts lookup)          │
└───────────────────────────────┬──────────────────────────────┘
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼                                           ▼
┌───────────────────────┐                 ┌─────────────────────┐
│    PantryRepository   │                 │   FoodApiRepository │
│ (Room Database + DAO) │                 │  (OpenFoodFacts &   │
│                       │                 │   TheMealDB APIs)   │
└──────────┬────────────┘                 └──────────┬──────────┘
           ▼                                         ▼
┌───────────────────────┐                 ┌─────────────────────┐
│    SQLite (Room DB)   │                 │  Retrofit Services  │
│  • pantry_items table │                 │  • OpenFoodFacts    │
│  • saved_recipes table│                 │  • TheMealDB API    │
└───────────────────────┘                 └─────────────────────┘
```

#### Data Entities

- **`PantryItem`**:
  - `id: Long` (Auto-increment Primary Key)
  - `barcode: String`
  - `name: String`
  - `brand: String`
  - `category: String` (Dairy, Produce, Meat, Pantry, Bakery, Beverage, Frozen, Snacks)
  - `quantity: Double`
  - `unit: String` (items, g, ml, oz, lbs)
  - `storageLocation: StorageLocation` (FRIDGE, PANTRY, FREEZER)
  - `purchaseDateMillis: Long`
  - `expiryDateMillis: Long`
  - `imageUrl: String?`
  - `ingredientsText: String?`
  - `nutriScore: String?`
  - `caloriesPer100g: Double?`
  - `isConsumed: Boolean = false`
  - `isWasted: Boolean = false`
  - `notes: String?`

- **`MealRecipe`**:
  - `idMeal: String`
  - `strMeal: String`
  - `strMealThumb: String?`
  - `strCategory: String?`
  - `strArea: String?`
  - `strInstructions: String?`
  - `ingredientsWithMeasure: List<Pair<String, String>>`
  - `matchedPantryIngredients: List<String>`

#### Interactive Component & State Mapping

1. **`ExpiryRadarCard`**:
   - Computes items where `daysUntilExpiry <= 3 && !isConsumed && !isWasted`.
   - Triggers `loadRecipesForExpiringItems(ingredients)` when tapped.
   - Shows badge with number of items at risk.
2. **`CameraScannerDialog / ScannerView`**:
   - Manages `CAMERA` permission request via `rememberLauncherForActivityResult`.
   - CameraX `PreviewView` preview.
   - Bottom sheet with manual barcode entry text field + quick test barcode chips (e.g. "Milk 3017620422003", "Pasta 8076809513753", "Tomato Soup 0051000012517").
3. **`PantryItemCard`**:
   - Swipe or button actions: "Consume" (celebratory toast / animation), "Edit", "Delete".
   - Days remaining countdown chip with intuitive color ramp.
4. **`RecipeDetailBottomSheet`**:
   - Full recipe breakdown, ingredient checkboxes, and YouTube tutorial link.
