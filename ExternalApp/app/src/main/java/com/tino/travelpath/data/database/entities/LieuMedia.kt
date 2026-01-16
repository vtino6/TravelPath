package com.tino.travelpath.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lieux_medias",
    primaryKeys = ["lieuId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = Lieu::class,
            parentColumns = ["id"],
            childColumns = ["lieuId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Media::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lieuId", "ordre"]),
        Index(value = ["mediaId"])
    ]
)
data class LieuMedia(
    val lieuId: String,
    val mediaId: String,
    val ordre: Int = 0
)
