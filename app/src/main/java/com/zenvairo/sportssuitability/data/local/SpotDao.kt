package com.zenvairo.sportssuitability.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpotDao {
    @Query("SELECT * FROM spots WHERE sportType = :sportType ORDER BY name")
    fun observeBySport(sportType: SportType): Flow<List<Spot>>

    @Insert
    suspend fun insert(spot: Spot): Long

    @Delete
    suspend fun delete(spot: Spot)

    @Query("UPDATE spots SET alertsEnabled = :enabled WHERE id = :spotId")
    suspend fun setAlertsEnabled(spotId: Long, enabled: Boolean)

    @Query("UPDATE spots SET coastFacingDeg = :facingDeg WHERE id = :spotId")
    suspend fun setCoastFacing(spotId: Long, facingDeg: Double?)

    @Query("UPDATE spots SET altitudeM = :altitudeM WHERE id = :spotId")
    suspend fun setAltitude(spotId: Long, altitudeM: Double?)

    @Query("SELECT * FROM spots WHERE alertsEnabled = 1 ORDER BY sportType, name")
    suspend fun spotsWithAlerts(): List<Spot>

    @Query("SELECT * FROM spots ORDER BY sportType, name")
    suspend fun allSpots(): List<Spot>
}
