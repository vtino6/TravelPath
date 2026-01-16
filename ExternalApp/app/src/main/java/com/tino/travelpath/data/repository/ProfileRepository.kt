package com.tino.travelpath.data.repository

import com.tino.travelpath.data.database.dao.PreferencesDao
import com.tino.travelpath.data.database.dao.ProfilDao
import com.tino.travelpath.data.database.entities.NiveauEffort
import com.tino.travelpath.data.database.entities.Preferences
import com.tino.travelpath.data.database.entities.Profil
import com.tino.travelpath.data.database.entities.TypeProfil
import java.util.UUID

/**
 * Repository for profile management
 */
class ProfileRepository(
    private val profilDao: ProfilDao,
    private val preferencesDao: PreferencesDao
) {
    
    /**
     * Create a default profile for a user (only if no profile exists)
     * Creates both the Profile and default Preferences
     */
    suspend fun createDefaultProfileIfNeeded(
        utilisateurId: String,
        userName: String
    ): Profil? {
        // Check if user already has profiles
        val existingProfiles = profilDao.getByUtilisateurSync(utilisateurId)
        
        // If profiles already exist, return null (don't create)
        if (existingProfiles.isNotEmpty()) {
            return null
        }
        
        // Create default profile
        val profil = Profil(
            id = UUID.randomUUID().toString(),
            utilisateurId = utilisateurId,
            nom = "Profil principal", // Default name
            typeProfil = TypeProfil.ADULTES, // Default to ADULTES (individual)
            dateCreation = System.currentTimeMillis()
        )
        
        profilDao.insert(profil)
        
        // Create default preferences for this profile
        val preferences = Preferences(
            id = UUID.randomUUID().toString(),
            profilId = profil.id,
            budgetMax = 50.0, // Default budget
            duree = 4 * 60, // Default 4 hours in minutes
            niveauEffort = NiveauEffort.FACILE, // Default easy
            sensibiliteFroid = 0,
            sensibiliteChaleur = 0,
            sensibiliteHumidite = 0,
            dateCreation = System.currentTimeMillis()
        )
        
        preferencesDao.insert(preferences)
        
        return profil
    }
    
    /**
     * Get all profiles for a user
     */
    fun getProfilesByUser(utilisateurId: String) = profilDao.getByUtilisateur(utilisateurId)
    
    /**
     * Get profile by ID
     */
    suspend fun getProfileById(id: String) = profilDao.getById(id)
    
    /**
     * Create a new profile
     */
    suspend fun createProfile(
        utilisateurId: String,
        nom: String,
        typeProfil: TypeProfil
    ): Profil {
        val profil = Profil(
            id = UUID.randomUUID().toString(),
            utilisateurId = utilisateurId,
            nom = nom,
            typeProfil = typeProfil,
            dateCreation = System.currentTimeMillis()
        )
        
        profilDao.insert(profil)
        
        // Create default preferences for the new profile
        val preferences = Preferences(
            id = UUID.randomUUID().toString(),
            profilId = profil.id,
            budgetMax = 50.0,
            duree = 4 * 60,
            niveauEffort = NiveauEffort.FACILE,
            sensibiliteFroid = 0,
            sensibiliteChaleur = 0,
            sensibiliteHumidite = 0,
            dateCreation = System.currentTimeMillis()
        )
        
        preferencesDao.insert(preferences)
        
        return profil
    }
}

