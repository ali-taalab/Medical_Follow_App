package com.alitaalab.medicalfollow.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Repository(private val db: AppDatabase) {
    private val childDao = db.childDao()
    private val vaccineDao = db.vaccineDao()
    private val visitDao = db.visitDao()

    val child: Flow<Child?> = childDao.observe()
    val vaccines: Flow<List<Vaccine>> = vaccineDao.observeAll()
    val visits: Flow<List<VisitWithMeds>> = visitDao.observeAll()

    fun observeVisit(id: Long): Flow<VisitWithMeds?> = visitDao.observe(id)
    suspend fun getVisit(id: Long): VisitWithMeds? = visitDao.get(id)

    /**
     * Saves the profile. On first save the vaccination schedule is generated; if the birth date
     * changes, due dates of scheduled doses are recalculated.
     */
    suspend fun saveChild(child: Child) = db.withTransaction {
        val previous = childDao.get()
        childDao.upsert(child)
        if (previous == null) {
            if (vaccineDao.getAll().isEmpty()) {
                vaccineDao.insertAll(VaccineSchedule.build(VaccineSchedule.mandatory, child.birthDate, true))
            }
        } else if (previous.birthDate != child.birthDate) {
            val updated = vaccineDao.getAll()
                .filter { it.offsetMonths != null }
                .map {
                    it.copy(dueDate = VaccineSchedule.dueDate(child.birthDate, it.offsetMonths!!, it.offsetDays))
                }
            vaccineDao.updateAll(updated)
        }
        Unit
    }

    /** Adds the optional vaccines that are not in the list yet. Returns how many were added. */
    suspend fun addOptionalVaccines(birth: LocalDate): Int = db.withTransaction {
        val existing = vaccineDao.getAll().map { it.name }.toSet()
        val toAdd = VaccineSchedule.build(VaccineSchedule.optional, birth, false)
            .map { it.copy(sortOrder = it.sortOrder + 100) }
            .filter { it.name !in existing }
        vaccineDao.insertAll(toAdd)
        toAdd.size
    }

    suspend fun saveVaccine(vaccine: Vaccine) = vaccineDao.upsert(vaccine)
    suspend fun deleteVaccine(vaccine: Vaccine) = vaccineDao.delete(vaccine)

    suspend fun saveVisit(visit: Visit, meds: List<Medication>): Long = visitDao.save(visit, meds)
    suspend fun deleteVisit(visit: Visit) = visitDao.delete(visit)
}
