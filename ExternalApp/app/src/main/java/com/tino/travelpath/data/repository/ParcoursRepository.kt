package com.tino.travelpath.data.repository

import com.tino.travelpath.data.database.dao.EtapeDao
import com.tino.travelpath.data.database.dao.ParcoursDao
import com.tino.travelpath.data.database.entities.Etape
import com.tino.travelpath.data.database.entities.Parcours
import com.tino.travelpath.data.database.entities.Preferences
import com.tino.travelpath.service.RouteGeneratorService
import kotlinx.coroutines.flow.Flow

class ParcoursRepository(
    private val parcoursDao: ParcoursDao,
    private val etapeDao: EtapeDao,
    private val routeGeneratorService: RouteGeneratorService
) {
    
    fun getAllParcours(utilisateurId: String): Flow<List<Parcours>> {
        return parcoursDao.getByUtilisateur(utilisateurId)
    }
    
    fun getSauvegardes(utilisateurId: String): Flow<List<Parcours>> {
        return parcoursDao.getSauvegardes(utilisateurId)
    }
    
    suspend fun getParcoursById(id: String): Parcours? {
        return parcoursDao.getById(id)
    }
    
    fun getEtapes(parcoursId: String): Flow<List<Etape>> {
        return etapeDao.getByParcours(parcoursId)
    }
    
    suspend fun generateRoutes(preferences: Preferences): List<Parcours> {
        return routeGeneratorService.generateRoutes(preferences)
    }
    
    suspend fun saveParcours(parcours: Parcours) {
        parcoursDao.insert(parcours.copy(estSauvegarde = true))
    }
    
    suspend fun updateSauvegarde(id: String, sauvegarde: Boolean) {
        parcoursDao.updateSauvegarde(id, sauvegarde)
    }
    
    suspend fun updateFavori(id: String, favori: Boolean) {
        parcoursDao.updateFavori(id, favori)
    }
    
    suspend fun deleteParcours(parcours: Parcours) {
        parcoursDao.delete(parcours)
    }
}
