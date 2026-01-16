package com.tino.travelpath.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tino.travelpath.data.database.dao.*
import com.tino.travelpath.data.database.entities.*

@Database(
    entities = [
        Utilisateur::class,
        Profil::class,
        Preferences::class,
        PreferencesActivite::class,
        Lieu::class,
        HorairesOuverture::class,
        PreferencesLieuObligatoire::class,
        Parcours::class,
        Etape::class,
        Media::class,
        LieuMedia::class,
        EtapeMedia::class,
        HistoriqueNavigation::class,
        CacheHorsLigne::class,
        EvaluationParcours::class,
        PartageParcours::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TravelPathDatabase : RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao
    abstract fun profilDao(): ProfilDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun lieuDao(): LieuDao
    abstract fun parcoursDao(): ParcoursDao
    abstract fun etapeDao(): EtapeDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: TravelPathDatabase? = null

        fun getDatabase(context: Context): TravelPathDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelPathDatabase::class.java,
                    "travelpath_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
