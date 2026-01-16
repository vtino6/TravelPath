package com.tino.travelpath.data.database

import androidx.room.TypeConverter
import com.tino.travelpath.data.database.entities.*

class Converters {
    @TypeConverter
    fun fromTypeProfil(value: TypeProfil): String = value.name

    @TypeConverter
    fun toTypeProfil(value: String): TypeProfil = TypeProfil.valueOf(value)

    @TypeConverter
    fun fromNiveauEffort(value: NiveauEffort): String = value.name

    @TypeConverter
    fun toNiveauEffort(value: String): NiveauEffort = NiveauEffort.valueOf(value)

    @TypeConverter
    fun fromTransportationMode(value: TransportationMode): String = value.name

    @TypeConverter
    fun toTransportationMode(value: String): TransportationMode = TransportationMode.valueOf(value)

    @TypeConverter
    fun fromActivite(value: Activite): String = value.name

    @TypeConverter
    fun toActivite(value: String): Activite = Activite.valueOf(value)

    @TypeConverter
    fun fromTypeParcours(value: TypeParcours): String = value.name

    @TypeConverter
    fun toTypeParcours(value: String): TypeParcours = TypeParcours.valueOf(value)

    @TypeConverter
    fun fromCreneau(value: Creneau): String = value.name

    @TypeConverter
    fun toCreneau(value: String): Creneau = Creneau.valueOf(value)

    @TypeConverter
    fun fromTypeMedia(value: TypeMedia): String = value.name

    @TypeConverter
    fun toTypeMedia(value: String): TypeMedia = TypeMedia.valueOf(value)

    @TypeConverter
    fun fromTypeContenu(value: TypeContenu): String = value.name

    @TypeConverter
    fun toTypeContenu(value: String): TypeContenu = TypeContenu.valueOf(value)

    @TypeConverter
    fun fromEvaluation(value: Evaluation): String = value.name

    @TypeConverter
    fun toEvaluation(value: String): Evaluation = Evaluation.valueOf(value)

    @TypeConverter
    fun fromTypePartage(value: TypePartage): String = value.name

    @TypeConverter
    fun toTypePartage(value: String): TypePartage = TypePartage.valueOf(value)
}
