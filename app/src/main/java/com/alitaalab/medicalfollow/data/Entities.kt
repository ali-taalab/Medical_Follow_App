package com.alitaalab.medicalfollow.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

/** The child's profile. The app keeps a single profile with id = 1. */
@Entity(tableName = "child")
data class Child(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val birthDate: LocalDate,
    val gender: String = GENDER_MALE,
    val bloodType: String = "",
    val allergies: String = "",
    val notes: String = "",
) {
    companion object {
        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"
    }
}

/**
 * A vaccine dose. Scheduled doses keep their age offset ([offsetMonths] / [offsetDays]) so
 * their due date can be recalculated if the birth date is edited. Custom doses have a null
 * [offsetMonths] and a due date chosen by the user.
 */
@Entity(tableName = "vaccines")
data class Vaccine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val dueDate: LocalDate,
    val offsetMonths: Int? = null,
    val offsetDays: Int = 0,
    val mandatory: Boolean = true,
    val takenDate: LocalDate? = null,
    val notes: String = "",
    val sortOrder: Int = 0,
) {
    val isTaken: Boolean get() = takenDate != null
    val isScheduled: Boolean get() = offsetMonths != null
}

/** A doctor visit (past or upcoming). */
@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: String = "",
    val doctorName: String = "",
    val specialty: String = "",
    val clinic: String = "",
    val reason: String = "",
    val diagnosis: String = "",
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val temperatureC: Double? = null,
    val notes: String = "",
    val prescriptionImage: String? = null,
)

/** One medicine line of a visit's prescription (الروشتة). */
@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = Visit::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("visitId")],
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val visitId: Long,
    val name: String,
    val dose: String = "",
    val frequency: String = "",
    val duration: String = "",
)

data class VisitWithMeds(
    @Embedded val visit: Visit,
    @Relation(parentColumn = "id", entityColumn = "visitId")
    val medications: List<Medication>,
)
