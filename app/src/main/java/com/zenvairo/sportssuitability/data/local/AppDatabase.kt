package com.zenvairo.sportssuitability.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class SportTypeConverters {
    @TypeConverter
    fun fromSportType(value: SportType): String = value.name

    @TypeConverter
    fun toSportType(value: String): SportType = SportType.valueOf(value)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spots ADD COLUMN alertsEnabled INTEGER NOT NULL DEFAULT 0")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spots ADD COLUMN coastFacingDeg REAL")
        db.execSQL("ALTER TABLE spots ADD COLUMN altitudeM REAL")
    }
}

/**
 * The morning alert used to be opt-in per spot and defaulted to off, which meant a fresh
 * install (or an upgrade) never notified anything until the user found the switch. Alerts are
 * now on by default, so existing spots are opted in too; the card switch still turns them off.
 */
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE spots SET alertsEnabled = 1")
    }
}

@Database(entities = [Spot::class], version = 4, exportSchema = false)
@TypeConverters(SportTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sports_suitability.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
