package com.alitaalab.medicalfollow.data

import java.time.LocalDate
import java.util.Locale

/**
 * Vaccination schedule templates.
 *
 * [mandatory] follows the Egyptian Ministry of Health compulsory schedule (التطعيمات الإجبارية).
 * Schedules change from time to time, so every entry can be edited or deleted in the app and the
 * user is reminded to compare it with the child's vaccination card.
 */
object VaccineSchedule {

    data class Template(
        val nameAr: String,
        val nameEn: String,
        val descAr: String,
        val descEn: String,
        val months: Int,
        val days: Int = 0,
    )

    val mandatory: List<Template> = listOf(
        Template(
            "الالتهاب الكبدي الوبائي B - الجرعة الصفرية", "Hepatitis B – birth dose",
            "خلال 24 ساعة من الولادة", "Within 24 hours of birth", 0, 0,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الصفرية", "Polio (oral, OPV) – zero dose",
            "خلال الأسبوع الأول بعد الولادة", "During the first week after birth", 0, 0,
        ),
        Template(
            "الدرن (BCG)", "Tuberculosis (BCG)",
            "حقنة في الكتف الأيسر خلال الشهر الأول", "Left shoulder injection, first month", 0, 7,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الأولى", "Polio (oral, OPV) – dose 1",
            "نقط بالفم", "Oral drops", 2,
        ),
        Template(
            "الخماسي - الجرعة الأولى", "Pentavalent (DTP-HepB-Hib) – dose 1",
            "دفتيريا، تيتانوس، سعال ديكي، كبدي B، هيموفيلس", "Diphtheria, tetanus, pertussis, hepatitis B, Hib", 2,
        ),
        Template(
            "شلل الأطفال (بالحقن IPV) - الجرعة الأولى", "Polio (injected, IPV) – dose 1",
            "حقنة", "Injection", 2,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الثانية", "Polio (oral, OPV) – dose 2",
            "نقط بالفم", "Oral drops", 4,
        ),
        Template(
            "الخماسي - الجرعة الثانية", "Pentavalent (DTP-HepB-Hib) – dose 2",
            "دفتيريا، تيتانوس، سعال ديكي، كبدي B، هيموفيلس", "Diphtheria, tetanus, pertussis, hepatitis B, Hib", 4,
        ),
        Template(
            "شلل الأطفال (بالحقن IPV) - الجرعة الثانية", "Polio (injected, IPV) – dose 2",
            "حقنة", "Injection", 4,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الثالثة", "Polio (oral, OPV) – dose 3",
            "نقط بالفم", "Oral drops", 6,
        ),
        Template(
            "الخماسي - الجرعة الثالثة", "Pentavalent (DTP-HepB-Hib) – dose 3",
            "دفتيريا، تيتانوس، سعال ديكي، كبدي B، هيموفيلس", "Diphtheria, tetanus, pertussis, hepatitis B, Hib", 6,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الرابعة", "Polio (oral, OPV) – dose 4",
            "نقط بالفم", "Oral drops", 9,
        ),
        Template(
            "فيتامين أ - الجرعة الأولى", "Vitamin A – dose 1",
            "100,000 وحدة دولية بالفم", "100,000 IU by mouth", 9,
        ),
        Template(
            "شلل الأطفال (بالفم) - الجرعة الخامسة", "Polio (oral, OPV) – dose 5",
            "نقط بالفم", "Oral drops", 12,
        ),
        Template(
            "الثلاثي الفيروسي (MMR) - الجرعة الأولى", "MMR (measles, mumps, rubella) – dose 1",
            "حصبة، غدة نكافية، حصبة ألماني", "Measles, mumps, rubella", 12,
        ),
        Template(
            "شلل الأطفال (بالفم) - جرعة منشطة", "Polio (oral, OPV) – booster",
            "نقط بالفم", "Oral drops", 18,
        ),
        Template(
            "الثلاثي البكتيري (DTP) - جرعة منشطة", "DTP – booster",
            "دفتيريا، تيتانوس، سعال ديكي", "Diphtheria, tetanus, pertussis", 18,
        ),
        Template(
            "الثلاثي الفيروسي (MMR) - الجرعة الثانية", "MMR (measles, mumps, rubella) – dose 2",
            "حصبة، غدة نكافية، حصبة ألماني", "Measles, mumps, rubella", 18,
        ),
        Template(
            "فيتامين أ - الجرعة الثانية", "Vitamin A – dose 2",
            "200,000 وحدة دولية بالفم", "200,000 IU by mouth", 18,
        ),
    )

    /** Common optional (paid) vaccines that the user can add from the vaccines screen. */
    val optional: List<Template> = listOf(
        Template("الروتا - الجرعة الأولى", "Rotavirus – dose 1", "اختياري - نقط بالفم", "Optional – oral drops", 2),
        Template("المكورات الرئوية - الجرعة الأولى", "Pneumococcal (PCV) – dose 1", "اختياري - حقنة", "Optional – injection", 2),
        Template("الروتا - الجرعة الثانية", "Rotavirus – dose 2", "اختياري - نقط بالفم", "Optional – oral drops", 4),
        Template("المكورات الرئوية - الجرعة الثانية", "Pneumococcal (PCV) – dose 2", "اختياري - حقنة", "Optional – injection", 4),
        Template("المكورات الرئوية - الجرعة الثالثة", "Pneumococcal (PCV) – dose 3", "اختياري - حقنة", "Optional – injection", 6),
        Template("الإنفلونزا الموسمية", "Seasonal influenza", "اختياري - سنوياً بعد 6 شهور", "Optional – yearly after 6 months", 6),
        Template("الالتهاب السحائي", "Meningococcal", "اختياري - حسب توصية الطبيب", "Optional – as advised by doctor", 9),
        Template("الجديري المائي - الجرعة الأولى", "Chickenpox (varicella) – dose 1", "اختياري - حقنة", "Optional – injection", 12),
        Template("الالتهاب الكبدي A - الجرعة الأولى", "Hepatitis A – dose 1", "اختياري - حقنة", "Optional – injection", 12),
        Template("الالتهاب الكبدي A - الجرعة الثانية", "Hepatitis A – dose 2", "اختياري - حقنة", "Optional – injection", 18),
    )

    fun dueDate(birth: LocalDate, months: Int, days: Int): LocalDate =
        birth.plusMonths(months.toLong()).plusDays(days.toLong())

    fun build(templates: List<Template>, birth: LocalDate, mandatory: Boolean): List<Vaccine> {
        val arabic = Locale.getDefault().language == "ar"
        return templates.mapIndexed { index, t ->
            Vaccine(
                name = if (arabic) t.nameAr else t.nameEn,
                description = if (arabic) t.descAr else t.descEn,
                dueDate = dueDate(birth, t.months, t.days),
                offsetMonths = t.months,
                offsetDays = t.days,
                mandatory = mandatory,
                sortOrder = index,
            )
        }
    }
}
