package com.tino.travelpath.service

import com.tino.travelpath.data.database.dao.EtapeDao
import com.tino.travelpath.data.database.dao.LieuDao
import com.tino.travelpath.data.database.dao.ParcoursDao
import com.tino.travelpath.data.database.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class RouteGeneratorService(
    private val lieuDao: LieuDao,
    private val parcoursDao: ParcoursDao,
    private val etapeDao: EtapeDao,
    private val weatherService: WeatherService,
    private val navigationService: NavigationService
) {
    
    suspend fun generateRoutes(preferences: Preferences): List<Parcours> = withContext(Dispatchers.Default) {
        val routes = mutableListOf<Parcours>()
        
        val activites = getActivitesFromPreferences(preferences)
        val lieux = mutableListOf<Lieu>()
        
        activites.forEach { activite ->
            val lieuxByCategorie = lieuDao.getByCategorie(activite).first()
            lieux.addAll(lieuxByCategorie)
        }
        
        val lieuxFiltres = filterByWeather(lieux)
        
        routes.add(generateRoute(TypeParcours.ECONOMIQUE, lieuxFiltres, preferences))
        routes.add(generateRoute(TypeParcours.EQUILIBRE, lieuxFiltres, preferences))
        routes.add(generateRoute(TypeParcours.CONFORT, lieuxFiltres, preferences))
        
        routes
    }
    
    private suspend fun generateRoute(
        type: TypeParcours,
        lieux: List<Lieu>,
        preferences: Preferences
    ): Parcours {
        val lieuxSelectionnes = when (type) {
            TypeParcours.ECONOMIQUE -> lieux.filter { it.coutMoyen ?: 0.0 <= 10.0 }.take(5)
            TypeParcours.EQUILIBRE -> lieux.take(7)
            TypeParcours.CONFORT -> lieux.take(8)
        }
        
        val budgetTotal = lieuxSelectionnes.sumOf { it.coutMoyen ?: 0.0 }
        val dureeTotale = lieuxSelectionnes.size * 60
        
        val parcours = Parcours(
            id = UUID.randomUUID().toString(),
            utilisateurId = null,
            nom = "Parcours ${type.name}",
            typeParcours = type,
            budgetTotal = budgetTotal,
            dureeTotale = dureeTotale,
            transportationMode = TransportationMode.MIXED, // Default mode (legacy code - route generation happens on backend)
            ville = null,
            estSauvegarde = false,
            estFavori = false
        )
        
        val etapes = lieuxSelectionnes.mapIndexed { index, lieu ->
            Etape(
                id = UUID.randomUUID().toString(),
                parcoursId = parcours.id,
                lieuId = lieu.id,
                ordre = index + 1,
                creneau = when (index) {
                    0, 1 -> Creneau.MATIN
                    in 2..4 -> Creneau.APRES_MIDI
                    else -> Creneau.SOIR
                },
                dureeEstimee = 60,
                distancePrecedente = if (index > 0) 1.5 else null,
                cout = lieu.coutMoyen ?: 0.0,
                notes = null
            )
        }
        
        parcoursDao.insert(parcours)
        etapeDao.insertAll(etapes)
        
        return parcours
    }
    
    private suspend fun filterByWeather(lieux: List<Lieu>): List<Lieu> {
        return lieux
    }
    
    private fun getActivitesFromPreferences(preferences: Preferences): List<Activite> {
        return listOf(Activite.CULTURE, Activite.DECOUVERTE)
    }
    
    suspend fun optimizeRoute(parcours: Parcours): Parcours {
        return parcours
    }
    
    suspend fun regenerateRoute(
        parcours: Parcours,
        adjustments: Preferences
    ): Parcours {
        val routes = generateRoutes(adjustments)
        return routes.firstOrNull() ?: parcours
    }
}
