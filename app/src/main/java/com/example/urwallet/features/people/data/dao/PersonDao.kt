package com.example.urwallet.features.people.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urwallet.features.people.data.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>): List<Long>

    @Update
    suspend fun updatePerson(person: PersonEntity): Int

    @Delete
    suspend fun deletePerson(person: PersonEntity): Int

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deletePersonById(id: Long): Int

    @Query("SELECT * FROM people ORDER BY name ASC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY name ASC")
    suspend fun getAllPeopleSync(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    fun getPersonById(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonByIdSync(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getPersonByPhone(phoneNumber: String): PersonEntity?

    @Query("SELECT * FROM people WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getPersonByName(name: String): PersonEntity?

    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPeople(query: String): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun getPeopleCount(): Int

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople(): Int
}
