package com.zenvairo.sportssuitability.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SportType { SURF, WINDSURF, SKI }

/**
 * A saved place the user cares about.
 *
 * [coastFacingDeg] is the compass bearing the coast looks towards (0 = the beach faces
 * north, so you look north at the sea). It is optional and only meaningful for water
 * sports; when set, the app can tell offshore from onshore wind, which is the single
 * biggest quality factor for surf.
 *
 * [baseAltitudeM] is only meaningful for ski resorts: forecasts are requested at that
 * elevation instead of the geocoder's village-level altitude, which otherwise reports
 * rain where the slopes get snow.
 */
@Entity(tableName = "spots")
data class Spot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val region: String?,
    val latitude: Double,
    val longitude: Double,
    val sportType: SportType,
    /** Opted into the morning go/no-go alert. On by default; the card switch opts out. */
    val alertsEnabled: Boolean = true,
    val coastFacingDeg: Double? = null,
    val altitudeM: Double? = null
)
