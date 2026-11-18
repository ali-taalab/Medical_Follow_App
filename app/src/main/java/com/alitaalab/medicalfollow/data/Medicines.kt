package com.alitaalab.medicalfollow.data

// Generated from iphone/medicines.js so both apps show the same list.
// Common children's medicines (age 0-6) in Egypt, for quick selection when writing a prescription.
// A picking list only, not medical advice; no doses on purpose (children's doses depend on weight).

data class MedicineItem(val ar: String, val en: String, val brands: List<String>) {
    fun generic(arabic: Boolean) = if (arabic) ar else en
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return q.isEmpty() || (listOf(ar, en) + brands).any { it.lowercase().contains(q) }
    }
}

data class MedicineGroup(val ar: String, val en: String, val items: List<MedicineItem>) {
    fun title(arabic: Boolean) = if (arabic) ar else en
}

object Medicines {
    val groups: List<MedicineGroup> = listOf(
        MedicineGroup(
            "خافض حرارة ومسكن", "Fever & pain",
            listOf(
                MedicineItem("باراسيتامول", "Paracetamol", listOf("Cetal", "Abimol", "Panadol Baby & Infant")),
                MedicineItem("إيبوبروفين", "Ibuprofen", listOf("Brufen", "Nurofen for Children")),
            ),
        ),
        MedicineGroup(
            "مضاد حيوي", "Antibiotics",
            listOf(
                MedicineItem("أموكسيسيلين", "Amoxicillin", listOf("E-Mox", "Amoxil")),
                MedicineItem("أموكسيسيلين + كلافيولانيك", "Amoxicillin + Clavulanate", listOf("Augmentin", "Curam", "Megamox", "Hibiotic")),
                MedicineItem("أزيثرومايسين", "Azithromycin", listOf("Zithromax", "Xithrone")),
                MedicineItem("كلاريثرومايسين", "Clarithromycin", listOf("Klacid")),
                MedicineItem("سيفيكسيم", "Cefixime", listOf("Suprax")),
                MedicineItem("سيفوروكسيم", "Cefuroxime", listOf("Zinnat")),
                MedicineItem("سيفاليكسين", "Cephalexin", listOf("Ceporex")),
                MedicineItem("سيفادروكسيل", "Cefadroxil", listOf("Duricef")),
                MedicineItem("سيفترياكسون (حقن)", "Ceftriaxone (injection)", listOf("Rocephin")),
            ),
        ),
        MedicineGroup(
            "معدة وإسهال وترجيع", "Stomach, diarrhea & vomiting",
            listOf(
                MedicineItem("محلول معالجة الجفاف", "Oral rehydration salts (ORS)", listOf("Rehydran")),
                MedicineItem("زنك", "Zinc", listOf()),
                MedicineItem("بروبيوتيك", "Probiotic", listOf("BioGaia drops", "Enterogermina")),
                MedicineItem("نيتازوكسانيد", "Nitazoxanide", listOf("Nanazoxid")),
                MedicineItem("ميترونيدازول", "Metronidazole", listOf("Flagyl")),
                MedicineItem("أوندانسيترون (للترجيع)", "Ondansetron (vomiting)", listOf("Zofran", "Danset")),
                MedicineItem("سيميثيكون (للمغص والغازات)", "Simethicone (colic & gas)", listOf("Disflatyl")),
                MedicineItem("لاكتيولوز (للإمساك)", "Lactulose (constipation)", listOf("Duphalac")),
                MedicineItem("لبوس جلسرين (للإمساك)", "Glycerin suppositories (constipation)", listOf()),
            ),
        ),
        MedicineGroup(
            "ديدان", "Worms",
            listOf(
                MedicineItem("ميبيندازول", "Mebendazole", listOf("Vermox")),
                MedicineItem("ألبيندازول", "Albendazole", listOf("Alzental")),
            ),
        ),
        MedicineGroup(
            "حساسية", "Allergy",
            listOf(
                MedicineItem("سيتريزين", "Cetirizine", listOf("Zyrtec")),
                MedicineItem("ديسلوراتادين", "Desloratadine", listOf("Aerius")),
                MedicineItem("لوراتادين", "Loratadine", listOf("Claritine")),
                MedicineItem("كيتوتيفين", "Ketotifen", listOf("Zaditen")),
            ),
        ),
        MedicineGroup(
            "صدر وكحة وحساسية صدر", "Chest, cough & asthma",
            listOf(
                MedicineItem("سالبيوتامول (جلسة/بخاخة)", "Salbutamol (nebulizer/inhaler)", listOf("Farcolin", "Ventolin")),
                MedicineItem("بوديزونيد (جلسة)", "Budesonide (nebulizer)", listOf("Pulmicort")),
                MedicineItem("مونتيلوكاست", "Montelukast", listOf("Singulair")),
                MedicineItem("أمبروكسول (مذيب بلغم)", "Ambroxol (mucolytic)", listOf("Mucosolvan")),
                MedicineItem("بريدنيزولون", "Prednisolone", listOf()),
            ),
        ),
        MedicineGroup(
            "أنف وأذن وعين", "Nose, ear & eye",
            listOf(
                MedicineItem("محلول ملحي للأنف", "Saline nasal drops", listOf("Otrivin Baby", "Physiomer")),
                MedicineItem("قطرة عين توبراميسين", "Tobramycin eye drops", listOf("Tobrex")),
                MedicineItem("قطرة أذن", "Ear drops", listOf()),
            ),
        ),
        MedicineGroup(
            "جلد وفم", "Skin & mouth",
            listOf(
                MedicineItem("كريم للتسلخات", "Diaper rash cream", listOf("Sudocrem", "Bepanthen")),
                MedicineItem("نيستاتين (لفطريات الفم)", "Nystatin (oral thrush)", listOf("Mycostatin")),
                MedicineItem("ميكونازول جل للفم", "Miconazole oral gel", listOf("Daktarin oral gel")),
                MedicineItem("فيوسيديك أسيد كريم", "Fusidic acid cream", listOf("Fucidin")),
                MedicineItem("ميوبيروسين مرهم", "Mupirocin ointment", listOf("Bactroban")),
                MedicineItem("هيدروكورتيزون كريم 1%", "Hydrocortisone 1% cream", listOf()),
                MedicineItem("كالامين لوشن (للهرش)", "Calamine lotion (itching)", listOf()),
            ),
        ),
        MedicineGroup(
            "فيتامينات ومكملات", "Vitamins & supplements",
            listOf(
                MedicineItem("فيتامين د", "Vitamin D", listOf("Vidrop", "Devarol-S")),
                MedicineItem("حديد", "Iron", listOf()),
                MedicineItem("فيتامينات متعددة", "Multivitamin", listOf("Pharmaton Kiddi")),
                MedicineItem("كالسيوم", "Calcium", listOf()),
            ),
        ),
    )

    /** Names offered while typing: generic names and "Brand (Generic)". */
    fun suggestions(arabic: Boolean): List<String> = groups.flatMap { g ->
        g.items.flatMap { m -> listOf(m.generic(arabic)) + m.brands.map { "$it (${m.generic(arabic)})" } }
    }
}
