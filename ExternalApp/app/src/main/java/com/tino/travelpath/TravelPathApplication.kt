package com.tino.travelpath

import android.app.Application
import com.tino.travelpath.data.database.TravelPathDatabase

class TravelPathApplication : Application() {
    
    val database by lazy { TravelPathDatabase.getDatabase(this) }
    
    override fun onCreate() {
        super.onCreate()
    }
}





