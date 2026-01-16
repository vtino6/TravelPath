# Guide UML Complet - TravelPath

## 📋 Fichiers UML Disponibles

### 1. **SCHEMA_BD_UML_COMPLET.puml**
**Description:** Schéma complet de toutes les bases de données utilisées dans TravelPath.

**Contenu:**
- **PostgreSQL (Backend):**
  - Table `users`: Utilisateurs
  - Table `routes`: Parcours sauvegardés avec `steps_json` (JSON)
  - Table `steps`: Étapes (legacy, principalement dans JSON)
  - Table `places`: Cache des lieux

- **Room SQLite (Android):**
  - Table `saved_routes`: Routes sauvegardées localement
  - Table `cached_routes`: Cache des routes générées
  - Table `cached_places`: Cache des lieux

- **Firebase Firestore (Photos):**
  - Collection `photos`: Photos partagées par ville (intégration collègue)

**Relations:**
- `users` → `routes` (1:N)
- `routes` → `steps` (1:N)
- `places` → `steps` (1:N)

---

### 2. **CLASSES_UML_COMPLET.puml**
**Description:** Diagramme de classes complet incluant Android et Backend.

**Contenu:**
- **Android App:**
  - **UI Screens:** HomeScreen, PreferencesScreen, RouteSelectionScreen, RouteDetailScreen, ActiveNavigationScreen, SavedRoutesScreen, BrowsePlacesScreen
  - **ViewModels:** PreferencesViewModel, RouteSelectionViewModel, SavedRoutesViewModel, RouteDetailViewModel, BrowsePlacesViewModel, CitySearchViewModel
  - **Repositories:** RouteRepository, PlaceRepository, SavedRouteRepository, PhotoRepository
  - **Data Models:** Route, RouteStep, Place, RouteRequest, RouteResponse, StepResponse, PlaceResponse, SavedRoute, CachedRoute, CachedPlace

- **Backend:**
  - **Controllers:** RoutesController, PlacesController, UserController, WeatherController
  - **Services:** RouteGeneratorService, RouteService, PlacesService, WeatherService, UserService
  - **External Services:** YelpPlacesService, GooglePlacesService, OpenRouteServiceClient, WeatherApiClient, OverpassClient
  - **Models:** Route, Step, Place, User
  - **DTOs:** RouteRequest, RouteResponse, StepResponse, PlaceResponse, PlaceSearchResponse, UserRequest, UserResponse, LoginRequest
  - **Repositories:** RouteRepository, PlaceRepository, StepRepository, UserRepository

- **Enums:** RouteType, TransportationMode, PlaceCategory, TimeSlot, EffortLevel

**Relations:** Toutes les dépendances entre classes, services, repositories, et APIs externes.

---

### 3. **LOGIQUE_METIER_UML_COMPLET.puml**
**Description:** Logique métier complète avec tous les algorithmes et processus.

**Contenu:**
- **Génération de Routes:**
  - RouteGeneratorService avec toutes les méthodes
  - Processus complet de génération (8 étapes)
  - Budget dégressif détaillé

- **Sélection de Lieux:**
  - PlacesService avec conversion Yelp/Google
  - Multiplicateurs de ville
  - Catégories

- **Calcul des Coûts:**
  - Coûts par mode de transport
  - Coûts de nourriture
  - Budget total

- **Optimisation:**
  - Algorithme Nearest Neighbor
  - Matrice de distances
  - Complexité O(n²)

- **Filtrage:**
  - Filtre météo
  - Filtre budget
  - Filtre distance

- **Sélection par Type:**
  - ECONOMIC, BALANCED, COMFORT
  - Algorithmes de tri

- **Validation:**
  - Critères de validation
  - Tolérance 10%

- **Persistance:**
  - Sérialisation JSON
  - Synchronisation Room ↔ PostgreSQL

---

## 🎯 Comment Utiliser Ces Diagrammes

### Visualisation

1. **Avec PlantUML:**
   - Installer PlantUML: https://plantuml.com/starting
   - Ouvrir les fichiers `.puml` dans VS Code avec extension PlantUML
   - Ou utiliser l'éditeur en ligne: http://www.plantuml.com/plantuml/uml/

2. **Export en images:**
   ```bash
   # Avec PlantUML CLI
   java -jar plantuml.jar SCHEMA_BD_UML_COMPLET.puml
   java -jar plantuml.jar CLASSES_UML_COMPLET.puml
   java -jar plantuml.jar LOGIQUE_METIER_UML_COMPLET.puml
   ```

3. **Avec VS Code:**
   - Installer l'extension "PlantUML"
   - Ouvrir un fichier `.puml`
   - Appuyer sur `Alt+D` pour prévisualiser
   - Exporter en PNG/SVG avec `Ctrl+Shift+P` → "PlantUML: Export Current Diagram"

---

## 📊 Points Clés des Diagrammes

### Schéma Base de Données
- **PostgreSQL:** Source de vérité, stockage persistant
- **Room:** Cache local Android, accès offline
- **Firebase:** Photos partagées (intégration collègue)
- **steps_json:** JSON pour éviter conflits Hibernate

### Classes
- **Architecture MVVM:** Screens → ViewModels → Repositories
- **Offline-first:** Room cache + backend sync
- **Multi-API:** Yelp + Google Places + OpenStreetMap
- **Firebase:** PhotoRepository pour photos de villes

### Logique Métier
- **Budget dégressif:** 90% lieux, 10% transport, diminue après chaque sélection
- **Sélection itérative:** 1500m depuis lieu actuel, budget restant
- **Nearest Neighbor:** O(n²) pour optimisation ordre
- **Validation:** Routes rejetées si > budget × 1.1

---

## 🔍 Détails Techniques

### Budget Dégressif
1. Allocation initiale: 90% pour lieux
2. Premier lieu: soustrait du budget
3. Lieux suivants: filtrés par budget restant
4. Arrêt si budget épuisé

### Sélection de Lieux
1. Récupération: 2km par catégorie
2. Filtre météo: retire lieux extérieurs
3. Ajout lieux requis: toujours inclus
4. Sélection itérative: 1500m + budget restant
5. Tri selon type: ECONOMIC/BALANCED/COMFORT
6. Aléatoire top 3: variété

### Coûts Transport
- **WALKING:** 0€
- **BICYCLE:** 0€
- **PUBLIC_TRANSPORT:** 2.50€/segment (fixe)
- **CAR:** (distance × 0.10€) + 3€ parking
- **MIXED:** Sélection intelligente selon distance

### Validation
- Budget total = lieux + transport
- Route acceptée si: total ≤ maxBudget × 1.1
- Route rejetée si: total > maxBudget × 1.1

---

## 📝 Pour la Présentation

Ces diagrammes peuvent être utilisés pour:
- **Documentation technique:** Expliquer l'architecture aux développeurs
- **Présentation projet:** Montrer la structure et les choix techniques
- **Onboarding:** Aider les nouveaux développeurs à comprendre le système
- **Maintenance:** Identifier les dépendances et points d'amélioration
- **Intégration:** Expliquer comment les composants s'intègrent

---

## ✅ Résumé

- **SCHEMA_BD_UML_COMPLET.puml:** Toutes les bases de données (PostgreSQL, Room, Firebase)
- **CLASSES_UML_COMPLET.puml:** Toutes les classes (Android + Backend)
- **LOGIQUE_METIER_UML_COMPLET.puml:** Toute la logique métier (algorithmes, processus)

Ces diagrammes sont complets et à jour avec toutes les fonctionnalités implémentées.
