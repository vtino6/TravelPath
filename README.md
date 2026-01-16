# TravelPath - Manuel d'Utilisation et d'Installation

## 📋 Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Prérequis](#prérequis)
3. [Installation](#installation)
   - [Backend (Spring Boot)](#backend-spring-boot)
   - [Application Android](#application-android)
   - [Base de données PostgreSQL](#base-de-données-postgresql)
4. [Configuration](#configuration)
   - [Clés API](#clés-api)
   - [Configuration du backend](#configuration-du-backend)
   - [Configuration de l'application Android](#configuration-de-lapplication-android)
5. [Utilisation](#utilisation)
   - [Démarrage de l'application](#démarrage-de-lapplication)
   - [Création d'un itinéraire](#création-dun-itinéraire)
   - [Navigation active](#navigation-active)
   - [Gestion des itinéraires sauvegardés](#gestion-des-itinéraires-sauvegardés)
6. [Dépannage](#dépannage)
7. [Architecture](#architecture)

---

## Vue d'ensemble

**TravelPath** est une application mobile de planification de voyages qui génère des itinéraires personnalisés basés sur les préférences de l'utilisateur (budget, activités, mode de transport, sensibilité météo).

### Technologies utilisées

- **Frontend** : Android (Kotlin) avec Jetpack Compose
- **Backend** : Spring Boot (Java)
- **Base de données** : PostgreSQL (backend) + Room/SQLite (mobile)
- **APIs externes** : Google Places, Yelp, OpenWeatherMap, OpenRouteService, OpenStreetMap, Firebase Firestore

---

## Prérequis

### Pour le développement

- **Java JDK 11+** (pour le backend)
- **Android Studio** (Hedgehog ou plus récent)
- **PostgreSQL 12+** (pour la base de données backend)
- **Maven 3.6+** (pour le backend)
- **Git** (pour cloner le dépôt)

### Comptes API requis

- **Google Cloud Platform** (pour Google Places API)
- **Yelp Fusion API** (pour les restaurants)
- **OpenWeatherMap** (optionnel, pour le filtrage météo)
- **Firebase** (pour les photos partagées)

---

## Installation

### Backend (Spring Boot)

#### 1. Cloner le dépôt

```bash
git clone https://github.com/vtino6/TravelPath.git
cd TravelPath/workspace/backend
```

#### 2. Installer PostgreSQL

**Sur macOS (Homebrew) :**
```bash
brew install postgresql@12
brew services start postgresql@12
```

**Sur Linux (Ubuntu/Debian) :**
```bash
sudo apt-get update
sudo apt-get install postgresql-12
sudo systemctl start postgresql
```

**Sur Windows :**
Télécharger depuis https://www.postgresql.org/download/windows/

#### 3. Créer la base de données

```bash
# Se connecter à PostgreSQL
psql -U postgres

# Créer la base de données
CREATE DATABASE travelpath_db;

# Créer un utilisateur (optionnel)
CREATE USER valentino WITH PASSWORD 'votre_mot_de_passe';
GRANT ALL PRIVILEGES ON DATABASE travelpath_db TO valentino;

# Quitter
\q
```

#### 4. Configurer la base de données

Éditer `workspace/backend/src/main/resources/application.properties` :

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/travelpath_db
spring.datasource.username=valentino
spring.datasource.password=votre_mot_de_passe
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

#### 5. Ajouter les dépendances Maven (si nécessaire)

Si le projet utilise Maven, vérifier que `pom.xml` contient :

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

#### 6. Lancer le backend

```bash
cd workspace/backend
mvn spring-boot:run
```

Le backend devrait démarrer sur `http://localhost:8080/api`

**Vérification :**
```bash
curl http://localhost:8080/api/routes/test
```

Réponse attendue :
```json
{"status":"ok","message":"Backend is running","timestamp":"2024-..."}
```

---

### Application Android

#### 1. Ouvrir le projet dans Android Studio

```bash
cd TravelPath/ExternalApp
# Ouvrir Android Studio et sélectionner le dossier ExternalApp
```

#### 2. Synchroniser les dépendances Gradle

Android Studio devrait automatiquement synchroniser les dépendances. Sinon :

```bash
cd ExternalApp
./gradlew build
```

#### 3. Configurer l'émulateur ou un appareil physique

- **Émulateur** : Créer un AVD (Android Virtual Device) avec API 24+
- **Appareil physique** : Activer le mode développeur et le débogage USB

#### 4. Compiler et installer

Dans Android Studio :
- Cliquer sur "Run" (▶️) ou `Shift + F10`
- Sélectionner l'appareil cible
- L'application sera compilée et installée automatiquement

---

### Base de données PostgreSQL

La base de données est créée automatiquement au premier démarrage du backend grâce à `spring.jpa.hibernate.ddl-auto=update`.

**Tables créées automatiquement :**
- `users` : Utilisateurs
- `routes` : Itinéraires sauvegardés
- `steps` : Étapes d'un itinéraire
- `places` : Lieux en cache

---

## Configuration

### Clés API

#### 1. Google Places API

**Étapes :**

1. Aller sur https://cloud.google.com/free
2. Créer un compte Google Cloud (gratuit : $300 de crédit + $200/mois)
3. Aller sur https://console.cloud.google.com
4. Naviguer vers "APIs & Services" → "Library"
5. Rechercher "Places API (New)" et cliquer "Enable"
6. Aller dans "APIs & Services" → "Credentials"
7. Cliquer "+ CREATE CREDENTIALS" → "API key"
8. Copier la clé API

**Ajouter au backend :**

Éditer `workspace/backend/src/main/resources/application.properties` :

```properties
google.places.api.key=VOTRE_CLE_API_ICI
google.places.enabled=true
google.places.cache.enabled=true
google.places.cache.duration.hours=24
```

#### 2. Yelp Fusion API

**Étapes :**

1. Aller sur https://www.yelp.com/developers
2. Créer une application
3. Obtenir la clé API

**Ajouter au backend :**

```properties
yelp.api.key=VOTRE_CLE_YELP_ICI
yelp.api.enabled=true
yelp.plan=base
```

#### 3. OpenWeatherMap (optionnel)

**Étapes :**

1. Aller sur https://openweathermap.org/api
2. Créer un compte gratuit
3. Obtenir la clé API

**Ajouter au backend :**

```properties
weather.api.key=VOTRE_CLE_WEATHER_ICI
weather.api.enabled=true
```

#### 4. Firebase (pour les photos)

**Étapes :**

1. Aller sur https://console.firebase.google.com
2. Créer un projet Firebase
3. Ajouter une application Android
4. Télécharger `google-services.json`
5. Placer `google-services.json` dans `ExternalApp/app/`

**Configuration :**

- Le fichier `google-services.json` doit être présent dans `ExternalApp/app/`
- Les dépendances Firebase sont déjà configurées dans `build.gradle.kts`

---

### Configuration du backend

#### Fichier `application.properties`

Configuration complète :

```properties
# Server Configuration
server.port=8080
server.servlet.context-path=/api

# Database Configuration (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/travelpath_db
spring.datasource.username=valentino
spring.datasource.password=
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Yelp API Configuration
yelp.api.key=VOTRE_CLE_YELP
yelp.api.enabled=true
yelp.plan=base

# Google Places API Configuration
google.places.api.key=VOTRE_CLE_GOOGLE
google.places.enabled=true
google.places.cache.enabled=true
google.places.cache.duration.hours=24

# Weather API Configuration (optionnel)
weather.api.key=VOTRE_CLE_WEATHER
weather.api.enabled=true
```

#### Activer le cache

Si le backend a une classe principale, ajouter `@EnableCaching` :

```java
@SpringBootApplication
@EnableCaching
public class TravelPathApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelPathApplication.class, args);
    }
}
```

---

### Configuration de l'application Android

#### 1. URL du backend

**Pour l'émulateur Android :**
- URL : `http://10.0.2.2:8080/api`
- `10.0.2.2` est l'adresse spéciale qui mappe vers `localhost` de votre ordinateur

**Pour un appareil physique :**
- Trouver l'IP de votre ordinateur :
  - **macOS/Linux** : `ifconfig | grep "inet "`
  - **Windows** : `ipconfig`
- URL : `http://VOTRE_IP:8080/api`
- **Important** : L'appareil et l'ordinateur doivent être sur le même réseau WiFi

**Modifier dans le code :**

Fichier : `ExternalApp/app/src/main/java/com/external/app/data/network/NetworkModule.kt`

```kotlin
private const val BASE_URL = "http://10.0.2.2:8080/api" // Émulateur
// ou
private const val BASE_URL = "http://192.168.1.100:8080/api" // Appareil physique
```

#### 2. Clé API Google Maps (optionnel, pour les cartes)

**Étapes :**

1. Obtenir une clé API Google Maps depuis Google Cloud Console
2. Créer `ExternalApp/local.properties` (si n'existe pas) :
   ```properties
   GOOGLE_MAPS_API_KEY=VOTRE_CLE_MAPS_ICI
   ```

**Note :** L'application utilise OpenStreetMap par défaut, donc Google Maps n'est pas strictement nécessaire.

---

## Utilisation

### Démarrage de l'application

#### 1. Démarrer le backend

```bash
cd workspace/backend
mvn spring-boot:run
```

Vérifier que le backend répond :
```bash
curl http://localhost:8080/api/routes/test
```

#### 2. Lancer l'application Android

- Ouvrir Android Studio
- Ouvrir le projet `ExternalApp`
- Cliquer sur "Run" (▶️)
- Sélectionner l'appareil cible

#### 3. Première connexion

- L'application démarre sur l'écran d'accueil
- Cliquer sur "Créer un parcours" pour générer un itinéraire
- Ou "Découvrir des lieux" pour explorer les photos de la communauté

---

### Création d'un itinéraire

#### 1. Écran des préférences

Après avoir cliqué sur "Créer un parcours", vous arrivez sur l'écran des préférences :

**Champs à remplir :**

- **Budget** : Budget total en euros (ex: 50€)
- **Nombre de lieux** : Nombre de lieux souhaités (défaut: 1)
- **Mode de transport** : Sélectionner un ou plusieurs modes
  - 🚶 Marche
  - 🚴 Vélo
  - 🚇 Transport public
  - 🚗 Voiture
- **Activités** : Sélectionner les catégories souhaitées
  - 🍽️ Restaurant
  - 🎨 Culture
  - 🎢 Loisirs
  - 🔍 Découverte
- **Sensibilité météo** : Activer/désactiver le filtrage météo

#### 2. Point de départ

- Saisir une ville ou une adresse dans le champ de recherche
- Sélectionner le point de départ depuis les suggestions

#### 3. Génération de l'itinéraire

- Cliquer sur "Générer l'itinéraire"
- L'application affiche un écran de chargement
- Le backend génère 1 à 3 itinéraires selon les contraintes :
  - **Économique** : Priorité aux lieux moins chers et proches
  - **Équilibré** : Priorité à la distance minimale
  - **Confort** : Priorité aux lieux plus chers, puis à la distance

#### 4. Sélection d'un itinéraire

- L'écran affiche les itinéraires générés avec :
  - Mini-carte avec le tracé
  - Budget total
  - Distance totale
  - Icons des modes de transport utilisés
- Cliquer sur un itinéraire pour voir les détails

---

### Navigation active

#### 1. Détails de l'itinéraire

Sur l'écran de détails, vous pouvez voir :

- **Carte** : Vue complète de l'itinéraire avec tous les points
- **Liste des étapes** : Chaque étape avec :
  - Nom du lieu
  - Catégorie (Restaurant, Culture, etc.)
  - Distance depuis l'étape précédente
  - Coût estimé
- **Métriques** : Budget total, modes de transport

#### 2. Démarrer la navigation

- Cliquer sur "COMMENCER NAVIGATION"
- L'application passe en mode navigation active

#### 3. Navigation étape par étape

**Contrôles disponibles :**

- **Flèche gauche** (◀️) : Étape précédente
- **Flèche droite** (▶️) : Étape suivante
- **Retour** (↩️) : Retour à l'écran précédent
- **Swipe horizontal** : Navigation par glissement

**Affichage :**

- **Carte** : Vue centrée sur l'étape actuelle
- **Indicateur d'étapes** : Points en haut montrant la progression
- **Informations de l'étape** : Nom, catégorie, distance, coût

---

### Gestion des itinéraires sauvegardés

#### 1. Sauvegarder un itinéraire

- Sur l'écran de détails, cliquer sur l'icône "Sauvegarder"
- L'itinéraire est sauvegardé localement (Room) et sur le backend

#### 2. Ajouter aux favoris

- Cliquer sur l'icône "Cœur" (❤️)
- L'itinéraire est ajouté aux favoris

#### 3. Voir les itinéraires sauvegardés

- Depuis l'écran d'accueil, naviguer vers "Mes itinéraires"
- Deux onglets :
  - **Tous** : Tous les itinéraires sauvegardés
  - **Favoris** : Uniquement les itinéraires favoris

#### 4. Supprimer un itinéraire

- Sur un itinéraire sauvegardé, cliquer sur l'icône "Supprimer" (🗑️)
- Confirmer la suppression
- L'itinéraire est supprimé localement et sur le backend

#### 5. Partager un itinéraire

- Cliquer sur l'icône "Partager" (📤)
- L'itinéraire est partagé via les applications installées (SMS, Email, etc.)

---

## Dépannage

### Le backend ne démarre pas

**Problème :** Erreur de connexion à la base de données

**Solution :**
1. Vérifier que PostgreSQL est démarré :
   ```bash
   # macOS
   brew services list
   
   # Linux
   sudo systemctl status postgresql
   ```
2. Vérifier les identifiants dans `application.properties`
3. Vérifier que la base de données existe :
   ```bash
   psql -U valentino -d travelpath_db -c "SELECT 1;"
   ```

---

### L'application Android ne se connecte pas au backend

**Problème :** Erreur réseau ou timeout

**Solutions :**

1. **Vérifier que le backend est démarré :**
   ```bash
   curl http://localhost:8080/api/routes/test
   ```

2. **Vérifier l'URL dans `NetworkModule.kt` :**
   - Émulateur : `http://10.0.2.2:8080/api`
   - Appareil physique : `http://VOTRE_IP:8080/api`

3. **Vérifier le firewall :**
   - Désactiver temporairement le firewall pour tester
   - Ou autoriser le port 8080

4. **Vérifier les logs Android :**
   - Dans Android Studio : View → Tool Windows → Logcat
   - Chercher les erreurs réseau (OkHttp, Retrofit)

---

### Aucun itinéraire généré

**Problème :** Le backend ne trouve pas de lieux

**Solutions :**

1. **Vérifier les clés API :**
   - Vérifier que `google.places.api.key` et `yelp.api.key` sont correctes
   - Vérifier que les APIs sont activées dans les consoles respectives

2. **Vérifier les logs du backend :**
   - Chercher les erreurs d'API dans les logs Maven
   - Vérifier les quotas API (limites gratuites)

3. **Tester manuellement les APIs :**
   ```bash
   # Test Google Places
   curl "http://localhost:8080/api/places/search?latitude=48.8566&longitude=2.3522&categories=CULTURE"
   
   # Test Yelp
   curl "http://localhost:8080/api/places/search?latitude=48.8566&longitude=2.3522&categories=RESTAURANT"
   ```

---

### Erreur de compilation Android

**Problème :** Erreurs Gradle ou dépendances manquantes

**Solutions :**

1. **Nettoyer le projet :**
   ```bash
   cd ExternalApp
   ./gradlew clean
   ```

2. **Synchroniser les dépendances :**
   - Dans Android Studio : File → Sync Project with Gradle Files

3. **Invalider les caches :**
   - File → Invalidate Caches / Restart

---

### La carte ne s'affiche pas

**Problème :** OpenStreetMap ne charge pas

**Solutions :**

1. **Vérifier la connexion Internet** (OpenStreetMap nécessite Internet)

2. **Vérifier les permissions :**
   - L'application doit avoir la permission `INTERNET` dans `AndroidManifest.xml`

3. **Vérifier les logs :**
   - Chercher les erreurs liées à `osmdroid` dans Logcat

---

## Architecture

### Structure du projet

```
TravelPath/
├── ExternalApp/              # Application Android
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/external/app/
│   │   │   │   ├── ui/              # Écrans Compose
│   │   │   │   ├── data/            # Modèles, Repository, API
│   │   │   │   └── navigation/      # Navigation Compose
│   │   │   └── res/                 # Ressources
│   │   └── build.gradle.kts
│   └── build.gradle.kts
│
└── workspace/backend/        # Backend Spring Boot
    ├── src/main/
    │   ├── java/com/travelpath/
    │   │   ├── controller/         # Contrôleurs REST
    │   │   ├── service/            # Logique métier
    │   │   ├── external/           # Clients API externes
    │   │   ├── model/              # Modèles de données
    │   │   └── config/             # Configuration
    │   └── resources/
    │       └── application.properties
    └── pom.xml (ou build.gradle)
```

### Diagrammes UML

Le projet contient trois diagrammes UML complets :

1. **`SCHEMA_BD_UML_COMPLET.puml`** : Schéma de base de données (PostgreSQL + Room + Firebase)
2. **`CLASSES_UML_COMPLET.puml`** : Diagramme de classes (Android + Backend)
3. **`LOGIQUE_METIER_UML_COMPLET.puml`** : Logique métier (algorithme de génération d'itinéraires)

**Pour visualiser :**
- Utiliser un plugin PlantUML dans votre IDE
- Ou utiliser http://www.plantuml.com/plantuml/uml/

---

## Support

Pour toute question ou problème :

1. Consulter les logs (backend et Android)
2. Vérifier la configuration des clés API
3. Vérifier la connectivité réseau
4. Consulter la documentation des APIs externes

---

**Version :** 1.0  
**Dernière mise à jour :** 2024
