package com.alitaalab.medicalfollow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ChildDao {
    @Query("SELECT * FROM child WHERE id = 1")
    fun observe(): Flow<Child?>

    @Query("SELECT * FROM child WHERE id = 1")
    suspend fun get(): Child?

    @Upsert
    suspend fun upsert(child: Child)
}

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccines ORDER BY dueDate, sortOrder, id")
    fun observeAll(): Flow<List<Vaccine>>

    @Query("SELECT * FROM vaccines ORDER BY dueDate, sortOrder, id")
    suspend fun getAll(): List<Vaccine>

    @Query("SELECT * FROM vaccines WHERE takenDate IS NULL AND dueDate <= :until ORDER BY dueDate")
    suspend fun pendingUntil(until: LocalDate): List<Vaccine>

    @Insert
    suspend fun insertAll(vaccines: List<Vaccine>)

    @Upsert
    suspend fun upsert(vaccine: Vaccine)

    @Update
    suspend fun updateAll(vaccines: List<Vaccine>)

    @Delete
    suspend fun delete(vaccine: Vaccine)
}

@Dao
abstract class VisitDao {
    @Transaction
    @Query("SELECT * FROM visits ORDER BY date DESC, time DESC, id DESC")
    abstract fun observeAll(): Flow<List<VisitWithMeds>>

    @Transaction
    @Query("SELECT * FROM visits WHERE id = :id")
    abstract fun observe(id: Long): Flow<VisitWithMeds?>

    @Transaction
    @Query("SELECT * FROM visits WHERE id = :id")
    abstract suspend fun get(id: Long): VisitWithMeds?

    @Query("SELECT * FROM visits WHERE date BETWEEN :from AND :to ORDER BY date")
    abstract suspend fun visitsBetween(from: LocalDate, to: LocalDate): List<Visit>

    @Insert
    protected abstract suspend fun insertVisit(visit: Visit): Long

    @Update
    protected abstract suspend fun updateVisit(visit: Visit)

    @Insert
    protected abstract suspend fun insertMedications(meds: List<Medication>)

    @Query("DELETE FROM medications WHERE visitId = :visitId")
    protected abstract suspend fun deleteMedications(visitId: Long)

    @Delete
    abstract suspend fun delete(visit: Visit)

    /** Inserts or updates a visit and replaces its prescription lines. Returns the visit id. */
    @Transaction
    open suspend fun save(visit: Visit, meds: List<Medication>): Long {
        val id = if (visit.id == 0L) {
            insertVisit(visit)
        } else {
            updateVisit(visit)
            deleteMedications(visit.id)
            visit.id
        }
        insertMedications(meds.map { it.copy(id = 0, visitId = id) })
        return id
    }
}
