package com.project.speciesdetection.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.project.speciesdetection.core.helpers.Converters
import com.project.speciesdetection.data.local.species.LocalSpecies
import com.project.speciesdetection.data.local.species.SpeciesDao
import com.project.speciesdetection.data.local.species_class.LocalSpeciesClass
import com.project.speciesdetection.data.local.species_class.SpeciesClassDao

@Database(
    entities = [
        // --- Thêm các entity mới vào đây ---
        LocalSpeciesClass::class,
        LocalSpecies::class
    ],
    version = 2, // <-- TĂNG VERSION LÊN
    exportSchema = false // Tùy chọn, đặt là false nếu không cần xuất schema
)
@TypeConverters(Converters::class) // <-- THÊM TYPE CONVERTERS
abstract class AppDatabase : RoomDatabase() {

    // --- Thêm các hàm abstract cho DAO mới ---
    abstract fun speciesClassDao(): SpeciesClassDao
    abstract fun speciesDao(): SpeciesDao

    // Giữ lại các DAO cũ nếu có
    // abstract fun someOldDao(): SomeOldDao
}