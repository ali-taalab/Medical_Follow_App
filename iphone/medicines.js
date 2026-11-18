// Common children's medicines (age 0–6) in Egypt, for quick selection when writing a prescription.
// Grouped by use. Each entry: generic name (ar / en) + common brand names.
// This is a picking list only — it is not medical advice and contains no doses on purpose:
// children's doses depend on weight and must come from the doctor.
export const MEDICINE_GROUPS = [
  {
    ar: 'خافض حرارة ومسكن', en: 'Fever & pain',
    items: [
      { ar: 'باراسيتامول', en: 'Paracetamol', brands: ['Cetal', 'Abimol', 'Panadol Baby & Infant'] },
      { ar: 'إيبوبروفين', en: 'Ibuprofen', brands: ['Brufen', 'Nurofen for Children'] }],
  },
  {
    ar: 'مضاد حيوي', en: 'Antibiotics',
    items: [
      { ar: 'أموكسيسيلين', en: 'Amoxicillin', brands: ['E-Mox', 'Amoxil'] },
      { ar: 'أموكسيسيلين + كلافيولانيك', en: 'Amoxicillin + Clavulanate', brands: ['Augmentin', 'Curam', 'Megamox', 'Hibiotic'] },
      { ar: 'أزيثرومايسين', en: 'Azithromycin', brands: ['Zithromax', 'Xithrone'] },
      { ar: 'كلاريثرومايسين', en: 'Clarithromycin', brands: ['Klacid'] },
      { ar: 'سيفيكسيم', en: 'Cefixime', brands: ['Suprax'] },
      { ar: 'سيفوروكسيم', en: 'Cefuroxime', brands: ['Zinnat'] },
      { ar: 'سيفاليكسين', en: 'Cephalexin', brands: ['Ceporex'] },
      { ar: 'سيفادروكسيل', en: 'Cefadroxil', brands: ['Duricef'] },
      { ar: 'سيفترياكسون (حقن)', en: 'Ceftriaxone (injection)', brands: ['Rocephin'] }],
  },
  {
    ar: 'معدة وإسهال وترجيع', en: 'Stomach, diarrhea & vomiting',
    items: [
      { ar: 'محلول معالجة الجفاف', en: 'Oral rehydration salts (ORS)', brands: ['Rehydran'] },
      { ar: 'زنك', en: 'Zinc', brands: [] },
      { ar: 'بروبيوتيك', en: 'Probiotic', brands: ['BioGaia drops', 'Enterogermina'] },
      { ar: 'نيتازوكسانيد', en: 'Nitazoxanide', brands: ['Nanazoxid'] },
      { ar: 'ميترونيدازول', en: 'Metronidazole', brands: ['Flagyl'] },
      { ar: 'أوندانسيترون (للترجيع)', en: 'Ondansetron (vomiting)', brands: ['Zofran', 'Danset'] },
      { ar: 'سيميثيكون (للمغص والغازات)', en: 'Simethicone (colic & gas)', brands: ['Disflatyl'] },
      { ar: 'لاكتيولوز (للإمساك)', en: 'Lactulose (constipation)', brands: ['Duphalac'] },
      { ar: 'لبوس جلسرين (للإمساك)', en: 'Glycerin suppositories (constipation)', brands: [] }],
  },
  {
    ar: 'ديدان', en: 'Worms',
    items: [
      { ar: 'ميبيندازول', en: 'Mebendazole', brands: ['Vermox'] },
      { ar: 'ألبيندازول', en: 'Albendazole', brands: ['Alzental'] }],
  },
  {
    ar: 'حساسية', en: 'Allergy',
    items: [
      { ar: 'سيتريزين', en: 'Cetirizine', brands: ['Zyrtec'] },
      { ar: 'ديسلوراتادين', en: 'Desloratadine', brands: ['Aerius'] },
      { ar: 'لوراتادين', en: 'Loratadine', brands: ['Claritine'] },
      { ar: 'كيتوتيفين', en: 'Ketotifen', brands: ['Zaditen'] }],
  },
  {
    ar: 'صدر وكحة وحساسية صدر', en: 'Chest, cough & asthma',
    items: [
      { ar: 'سالبيوتامول (جلسة/بخاخة)', en: 'Salbutamol (nebulizer/inhaler)', brands: ['Farcolin', 'Ventolin'] },
      { ar: 'بوديزونيد (جلسة)', en: 'Budesonide (nebulizer)', brands: ['Pulmicort'] },
      { ar: 'مونتيلوكاست', en: 'Montelukast', brands: ['Singulair'] },
      { ar: 'أمبروكسول (مذيب بلغم)', en: 'Ambroxol (mucolytic)', brands: ['Mucosolvan'] },
      { ar: 'بريدنيزولون', en: 'Prednisolone', brands: [] }],
  },
  {
    ar: 'أنف وأذن وعين', en: 'Nose, ear & eye',
    items: [
      { ar: 'محلول ملحي للأنف', en: 'Saline nasal drops', brands: ['Otrivin Baby', 'Physiomer'] },
      { ar: 'قطرة عين توبراميسين', en: 'Tobramycin eye drops', brands: ['Tobrex'] },
      { ar: 'قطرة أذن', en: 'Ear drops', brands: [] }],
  },
  {
    ar: 'جلد وفم', en: 'Skin & mouth',
    items: [
      { ar: 'كريم للتسلخات', en: 'Diaper rash cream', brands: ['Sudocrem', 'Bepanthen'] },
      { ar: 'نيستاتين (لفطريات الفم)', en: 'Nystatin (oral thrush)', brands: ['Mycostatin'] },
      { ar: 'ميكونازول جل للفم', en: 'Miconazole oral gel', brands: ['Daktarin oral gel'] },
      { ar: 'فيوسيديك أسيد كريم', en: 'Fusidic acid cream', brands: ['Fucidin'] },
      { ar: 'ميوبيروسين مرهم', en: 'Mupirocin ointment', brands: ['Bactroban'] },
      { ar: 'هيدروكورتيزون كريم 1%', en: 'Hydrocortisone 1% cream', brands: [] },
      { ar: 'كالامين لوشن (للهرش)', en: 'Calamine lotion (itching)', brands: [] }],
  },
  {
    ar: 'فيتامينات ومكملات', en: 'Vitamins & supplements',
    items: [
      { ar: 'فيتامين د', en: 'Vitamin D', brands: ['Vidrop', 'Devarol-S'] },
      { ar: 'حديد', en: 'Iron', brands: [] },
      { ar: 'فيتامينات متعددة', en: 'Multivitamin', brands: ['Pharmaton Kiddi'] },
      { ar: 'كالسيوم', en: 'Calcium', brands: [] }],
  }];

/** Flat list of names used for type-ahead suggestions: "Brand (Generic)" and the generic names. */
export function medicineSuggestions(lang) {
  const out = [];
  MEDICINE_GROUPS.forEach((g) => g.items.forEach((m) => {
    out.push(lang === 'ar' ? m.ar : m.en);
    m.brands.forEach((b) => out.push(`${b} (${lang === 'ar' ? m.ar : m.en})`));
  }));
  return out;
}
