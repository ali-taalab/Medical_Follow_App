// Child Health Tracker — iPhone (PWA) version of the Android app.
// Plain ES modules, no build step. Data lives in IndexedDB on the device.
import { db } from './db.js';
import { STRINGS } from './i18n.js';
import { MANDATORY, OPTIONAL } from './schedule.js';
import { MEDICINE_GROUPS, medicineSuggestions } from './medicines.js';
import { readPrescription } from './scan.js';

const state = {
  child: null,
  vaccines: [],
  visits: [],
  lang: 'ar',
  filter: 'all',
  aiKey: '',
};

let editPhoto = null; // photo (data URL) of the visit being edited
let pickerTarget = null; // medicine row the medicine picker fills (null = add a new row)
let scanning = false;

// ---------------------------------------------------------------- i18n

function t(key, ...args) {
  let s = STRINGS[state.lang][key] ?? STRINGS.ar[key] ?? key;
  const nf = new Intl.NumberFormat(state.lang === 'ar' ? 'ar-EG' : 'en-US');
  args.forEach((a, i) => { s = s.split(`{${i + 1}}`).join(typeof a === 'number' ? nf.format(a) : String(a)); });
  return s;
}

function detectLang() {
  try {
    const saved = localStorage.getItem('lang');
    if (saved === 'ar' || saved === 'en') return saved;
  } catch (_) { /* storage blocked */ }
  return (navigator.language || 'ar').toLowerCase().startsWith('en') ? 'en' : 'ar';
}

function applyLang() {
  document.documentElement.lang = state.lang;
  document.documentElement.dir = state.lang === 'ar' ? 'rtl' : 'ltr';
  document.title = t('app_name');
}

// ---------------------------------------------------------------- dates
// Dates are stored as local 'YYYY-MM-DD' strings.

const pad = (n) => String(n).padStart(2, '0');
const iso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
const parseDate = (s) => { const [y, m, d] = s.split('-').map(Number); return new Date(y, m - 1, d); };
const today = () => iso(new Date());

function addDays(s, n) {
  const d = parseDate(s);
  d.setDate(d.getDate() + n);
  return iso(d);
}

/** Same semantics as java.time.LocalDate.plusMonths (clamps to month end). */
function addMonths(s, n) {
  const d = parseDate(s);
  const day = d.getDate();
  const r = new Date(d.getFullYear(), d.getMonth() + n, 1);
  const last = new Date(r.getFullYear(), r.getMonth() + 1, 0).getDate();
  r.setDate(Math.min(day, last));
  return iso(r);
}

function daysBetween(a, b) {
  return Math.round((parseDate(b) - parseDate(a)) / 86400000);
}

function fmtDate(s) {
  if (!s) return '';
  return new Intl.DateTimeFormat(state.lang === 'ar' ? 'ar-EG' : 'en-GB', {
    day: 'numeric', month: 'short', year: 'numeric',
  }).format(parseDate(s));
}

function fmtTime(hhmm) {
  if (!hhmm) return '';
  const [h, m] = hhmm.split(':').map(Number);
  if (Number.isNaN(h)) return hhmm;
  return new Intl.DateTimeFormat(state.lang === 'ar' ? 'ar-EG' : 'en-GB', {
    hour: 'numeric', minute: '2-digit',
  }).format(new Date(2000, 0, 1, h, m || 0));
}

/** Like java.time.Period.between. */
function ageOn(birth, on = today()) {
  if (on < birth) return { y: 0, m: 0, d: 0 };
  const B = parseDate(birth);
  const O = parseDate(on);
  let months = (O.getFullYear() - B.getFullYear()) * 12 + (O.getMonth() - B.getMonth());
  let days = O.getDate() - B.getDate();
  if (days < 0) {
    months -= 1;
    days = daysBetween(addMonths(birth, months), on);
  }
  return { y: Math.floor(months / 12), m: months % 12, d: days };
}

function ageText(a) {
  if (a.y > 0) return t('age_years_months', a.y, a.m);
  if (a.m > 0) return t('age_months_days', a.m, a.d);
  return t('age_days', a.d);
}

// ---------------------------------------------------------------- helpers

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
}[c]));

/** Converts Arabic-Indic / Persian digits and decimal separators so numbers parse. */
function parseDecimal(input) {
  const s = String(input || '')
    .replace(/[٠-٩]/g, (c) => String(c.charCodeAt(0) - 0x0660))
    .replace(/[۰-۹]/g, (c) => String(c.charCodeAt(0) - 0x06f0))
    .replace(/[٫,]/g, '.')
    .trim();
  if (!s) return null;
  const n = Number(s);
  return Number.isFinite(n) ? n : null;
}

const pretty = (n) => (n == null ? '' : String(Math.round(n * 100) / 100));

function toast(msg) {
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = msg;
  document.body.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => { el.classList.remove('show'); setTimeout(() => el.remove(), 300); }, 2200);
}

const ICONS = {
  home: '<path d="M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z"/>',
  syringe: '<path d="m18 2 4 4"/><path d="m17 7 3-3"/><path d="M19 9 8.7 19.3a2.4 2.4 0 0 1-3.4 0l-.6-.6a2.4 2.4 0 0 1 0-3.4L15 5"/><path d="m9 11 4 4"/><path d="m5 19-3 3"/><path d="m14 4 6 6"/>',
  steth: '<path d="M5 3H4a1 1 0 0 0-1 1v5a5 5 0 0 0 10 0V4a1 1 0 0 0-1-1h-1"/><path d="M8 14v1a6 6 0 0 0 12 0v-3"/><circle cx="20" cy="10" r="2"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  share: '<path d="M12 3v12"/><path d="m7 8 5-5 5 5"/><path d="M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6"/>',
  back: '<path d="m15 18-6-6 6-6"/>',
  edit: '<path d="M4 20h4L19 9l-4-4L4 16z"/><path d="m13 7 4 4"/>',
  trash: '<path d="M4 7h16"/><path d="M9 7V4h6v3"/><path d="M6 7l1 13h10l1-13"/>',
  more: '<circle cx="5" cy="12" r="1.5"/><circle cx="12" cy="12" r="1.5"/><circle cx="19" cy="12" r="1.5"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
  check: '<circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/>',
  circle: '<circle cx="12" cy="12" r="9"/>',
  pill: '<path d="M10.5 20.5a5 5 0 0 1-7-7l10-10a5 5 0 0 1 7 7z"/><path d="m8.5 8.5 7 7"/>',
  camera: '<path d="M3 8h4l2-3h6l2 3h4v12H3z"/><circle cx="12" cy="13" r="4"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  warn: '<path d="M12 3 2 21h20z"/><path d="M12 10v5M12 18v.01"/>',
  list: '<path d="M8 6h13M8 12h13M8 18h13"/><path d="M3 6h.01M3 12h.01M3 18h.01"/>',
  sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>',
  baby: '<circle cx="12" cy="12" r="9"/><path d="M9 10h.01M15 10h.01"/><path d="M9.5 15a3.5 3.5 0 0 0 5 0"/><path d="M12 3c-1 1.5-1 3 0 4"/>',
};
const icon = (name, cls = '') => `<svg class="ic ${cls}" viewBox="0 0 24 24" aria-hidden="true">${ICONS[name]}</svg>`;

// ---------------------------------------------------------------- data

function sortVaccines() {
  state.vaccines.sort((a, b) => (a.dueDate < b.dueDate ? -1 : a.dueDate > b.dueDate ? 1 : (a.sortOrder - b.sortOrder) || (a.id - b.id)));
}

function sortVisits() {
  state.visits.sort((a, b) => (b.date + (b.time || '')).localeCompare(a.date + (a.time || '')) || b.id - a.id);
}

async function loadAll() {
  state.child = (await db.kvGet('child')) || null;
  state.vaccines = await db.getAll('vaccines');
  state.visits = await db.getAll('visits');
  sortVaccines();
  sortVisits();
}

function buildVaccines(templates, birth, mandatory, orderBase) {
  const ar = state.lang === 'ar';
  return templates.map((tp, i) => ({
    name: ar ? tp.ar : tp.en,
    description: ar ? tp.dar : tp.den,
    dueDate: addDays(addMonths(birth, tp.m), tp.d),
    offsetMonths: tp.m,
    offsetDays: tp.d,
    mandatory,
    takenDate: null,
    notes: '',
    sortOrder: orderBase + i,
  }));
}

async function saveChild(child) {
  const previous = state.child;
  await db.kvSet('child', child);
  state.child = child;
  if (!previous) {
    if (state.vaccines.length === 0) {
      for (const v of buildVaccines(MANDATORY, child.birthDate, true, 0)) {
        v.id = await db.put('vaccines', v);
        state.vaccines.push(v);
      }
    }
  } else if (previous.birthDate !== child.birthDate) {
    const changed = state.vaccines.filter((v) => v.offsetMonths != null);
    changed.forEach((v) => { v.dueDate = addDays(addMonths(child.birthDate, v.offsetMonths), v.offsetDays); });
    await db.putMany('vaccines', changed);
  }
  sortVaccines();
}

async function saveVaccine(v) {
  const id = await db.put('vaccines', v);
  v.id = id;
  const i = state.vaccines.findIndex((x) => x.id === id);
  if (i >= 0) state.vaccines[i] = v; else state.vaccines.push(v);
  sortVaccines();
}

async function deleteVaccine(id) {
  await db.delete('vaccines', id);
  state.vaccines = state.vaccines.filter((v) => v.id !== id);
}

async function addOptionalVaccines() {
  const existing = new Set(state.vaccines.map((v) => v.name));
  const toAdd = buildVaccines(OPTIONAL, state.child.birthDate, false, 100).filter((v) => !existing.has(v.name));
  for (const v of toAdd) {
    v.id = await db.put('vaccines', v);
    state.vaccines.push(v);
  }
  sortVaccines();
  return toAdd.length;
}

async function saveVisit(visit) {
  if (!visit.id) delete visit.id;
  const id = await db.put('visits', visit);
  visit.id = id;
  const i = state.visits.findIndex((x) => x.id === id);
  if (i >= 0) state.visits[i] = visit; else state.visits.push(visit);
  sortVisits();
  return id;
}

async function deleteVisit(id) {
  await db.delete('visits', id);
  state.visits = state.visits.filter((v) => v.id !== id);
}

function vaccineStatus(v, now = today()) {
  if (v.takenDate) return 'taken';
  if (v.dueDate < now) return 'overdue';
  if (v.dueDate <= addDays(now, 14)) return 'soon';
  return 'upcoming';
}

const STATUS_LABEL = { taken: 'status_taken', overdue: 'status_overdue', soon: 'status_due_soon', upcoming: 'status_upcoming' };
const chip = (s) => `<span class="chip ${s}">${esc(t(STATUS_LABEL[s]))}</span>`;

function groupKey(v) {
  if (v.offsetMonths == null) return Number.MAX_SAFE_INTEGER;
  return v.offsetMonths * 100 + (v.offsetDays > 0 ? 1 : 0);
}

function groupLabel(key) {
  if (key === Number.MAX_SAFE_INTEGER) return t('group_custom');
  const months = Math.floor(key / 100);
  const days = key % 100;
  if (months === 0 && days === 0) return t('group_birth');
  if (months === 0) return t('group_first_month');
  return t('group_months', months);
}

// ---------------------------------------------------------------- report / share

function visitText(v) {
  const lines = [];
  lines.push(`📅 ${fmtDate(v.date)}${v.time ? ' ' + fmtTime(v.time) : ''}`);
  const doctor = [v.doctorName, v.specialty, v.clinic].filter(Boolean).join(' - ');
  if (doctor) lines.push(`${t('doctor')}: ${doctor}`);
  if (v.reason) lines.push(`${t('reason')}: ${v.reason}`);
  if (v.diagnosis) lines.push(`${t('diagnosis')}: ${v.diagnosis}`);
  if (v.weightKg != null) lines.push(`${t('weight_kg')}: ${pretty(v.weightKg)}`);
  if (v.heightCm != null) lines.push(`${t('height_cm')}: ${pretty(v.heightCm)}`);
  if (v.temperatureC != null) lines.push(`${t('temperature_c')}: ${pretty(v.temperatureC)}`);
  if (v.medications?.length) {
    lines.push(`${t('prescription')}:`);
    v.medications.forEach((m) => {
      const d = [m.dose, m.frequency, m.duration].filter(Boolean).join(' - ');
      lines.push(`  💊 ${m.name}${d ? ` (${d})` : ''}`);
    });
  }
  if (v.notes) lines.push(`${t('notes')}: ${v.notes}`);
  return lines.join('\n');
}

function fullReport() {
  const c = state.child;
  const out = [];
  out.push(t('report_title', c.name));
  out.push(`${t('birth_date')}: ${fmtDate(c.birthDate)}`);
  out.push(`${t('age')}: ${ageText(ageOn(c.birthDate))}`);
  if (c.bloodType) out.push(`${t('blood_type')}: ${c.bloodType}`);
  if (c.allergies) out.push(`${t('allergies')}: ${c.allergies}`);
  out.push('');
  const taken = state.vaccines.filter((v) => v.takenDate).sort((a, b) => a.takenDate.localeCompare(b.takenDate));
  const remaining = state.vaccines.filter((v) => !v.takenDate);
  out.push(`✅ ${t('vaccines_taken')} (${taken.length})`);
  taken.forEach((v) => out.push(`  • ${v.name} — ${fmtDate(v.takenDate)}`));
  out.push('');
  out.push(`⏳ ${t('vaccines_remaining')} (${remaining.length})`);
  remaining.forEach((v) => out.push(`  • ${v.name} — ${fmtDate(v.dueDate)}`));
  out.push('');
  out.push(`🩺 ${t('tab_visits')} (${state.visits.length})`);
  state.visits.forEach((v) => { out.push('—————'); out.push(visitText(v)); });
  return out.join('\n');
}

async function shareText(text) {
  if (navigator.share) {
    try { await navigator.share({ text }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  try {
    await navigator.clipboard.writeText(text);
    toast(t('copied'));
  } catch (_) { /* nothing else to try */ }
}

/** Shares a file through the iOS share sheet, or downloads it elsewhere. */
async function shareFile(name, type, content) {
  const file = new File([content], name, { type });
  if (navigator.canShare && navigator.canShare({ files: [file] })) {
    try { await navigator.share({ files: [file] }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  const url = URL.createObjectURL(file);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

// ---------------------------------------------------------------- calendar (.ics)
// iPhone web apps cannot schedule local notifications, so reminders go to the Calendar app.

function icsEscape(s) {
  return String(s).replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');
}

function icsEvent({ uid, date, time, title, description }) {
  const d = date.replace(/-/g, '');
  const now = new Date();
  const stamp = `${now.getUTCFullYear()}${pad(now.getUTCMonth() + 1)}${pad(now.getUTCDate())}T${pad(now.getUTCHours())}${pad(now.getUTCMinutes())}00Z`;
  const lines = ['BEGIN:VEVENT', `UID:${uid}@child-health-tracker`, `DTSTAMP:${stamp}`];
  const alarms = [];
  if (time) {
    lines.push(`DTSTART:${d}T${time.replace(':', '')}00`, 'DURATION:PT1H');
    alarms.push('-P1D', '-PT2H');
  } else {
    lines.push(`DTSTART;VALUE=DATE:${d}`, `DTEND;VALUE=DATE:${addDays(date, 1).replace(/-/g, '')}`);
    alarms.push('-PT15H', 'PT9H'); // 9 AM the day before, and 9 AM on the day
  }
  lines.push(`SUMMARY:${icsEscape(title)}`);
  if (description) lines.push(`DESCRIPTION:${icsEscape(description)}`);
  alarms.forEach((tr) => lines.push('BEGIN:VALARM', 'ACTION:DISPLAY', `DESCRIPTION:${icsEscape(title)}`, `TRIGGER:${tr}`, 'END:VALARM'));
  lines.push('END:VEVENT');
  return lines;
}

function openCalendar(events, filename) {
  const ics = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Child Health Tracker//EN', 'CALSCALE:GREGORIAN',
    ...events.flatMap(icsEvent), 'END:VCALENDAR'].join('\r\n');
  const blob = new Blob([ics], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  // Safari opens text/calendar files with its "Add to Calendar" screen.
  if (/iPhone|iPad|iPod/.test(navigator.userAgent)) {
    window.location.href = url;
  } else {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
  }
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

function vaccineEvent(v) {
  return {
    uid: `vaccine-${v.id}`,
    date: v.dueDate,
    title: `💉 ${v.name} — ${state.child?.name ?? ''}`,
    description: v.description,
  };
}

function visitEvent(v) {
  const who = [v.doctorName, v.specialty].filter(Boolean).join(' - ');
  return {
    uid: `visit-${v.id}`,
    date: v.date,
    time: v.time,
    title: `🩺 ${who || v.reason || t('visit')} — ${state.child?.name ?? ''}`,
    description: [v.clinic, v.reason].filter(Boolean).join('\n'),
  };
}

// ---------------------------------------------------------------- backup

async function exportBackup() {
  const data = {
    app: 'child-health-tracker',
    version: 1,
    exportedAt: new Date().toISOString(),
    child: state.child,
    vaccines: state.vaccines,
    visits: state.visits,
  };
  const name = `child-health-backup-${today()}.json`;
  await shareFile(name, 'application/json', JSON.stringify(data));
}

async function importBackup(file) {
  let data;
  try {
    data = JSON.parse(await file.text());
    if (data.app !== 'child-health-tracker' || !data.child || !Array.isArray(data.vaccines) || !Array.isArray(data.visits)) {
      throw new Error('bad file');
    }
  } catch (_) {
    toast(t('import_failed'));
    return;
  }
  if (!confirm(t('confirm_import'))) return;
  await db.clear('vaccines');
  await db.clear('visits');
  await db.kvSet('child', data.child);
  await db.putMany('vaccines', data.vaccines);
  await db.putMany('visits', data.visits);
  await loadAll();
  toast(t('import_done'));
  go('#/home');
}

async function eraseAll() {
  if (!confirm(t('confirm_erase'))) return;
  await db.clear('vaccines');
  await db.clear('visits');
  await db.clear('kv');
  state.aiKey = '';
  await loadAll();
  go('#/home');
}

// ---------------------------------------------------------------- images

async function compressImage(file) {
  const url = URL.createObjectURL(file);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();
    const max = 1600;
    const scale = Math.min(1, max / Math.max(img.naturalWidth, img.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(img.naturalWidth * scale);
    canvas.height = Math.round(img.naturalHeight * scale);
    canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL('image/jpeg', 0.8);
  } finally {
    URL.revokeObjectURL(url);
  }
}

// ---------------------------------------------------------------- layout

const TABS = [
  ['#/home', 'tab_home', 'home'],
  ['#/vaccines', 'tab_vaccines', 'syringe'],
  ['#/visits', 'tab_visits', 'steth'],
  ['#/profile', 'tab_profile', 'user'],
];

function headerHtml({ title, back = false, actions = '' }) {
  return `<header class="topbar">
    <div class="topbar-side">${back ? `<button class="iconbtn back" data-action="back" aria-label="${esc(t('back'))}">${icon('back')}</button>` : ''}</div>
    <h1>${esc(title)}</h1>
    <div class="topbar-side end">${actions}</div>
  </header>`;
}

function layout(route, header, body) {
  const tabs = TABS.some(([href]) => href === route)
    ? `<nav class="tabbar">${TABS.map(([href, label, ic]) => `
        <a href="${href}" class="${href === route ? 'active' : ''}">${icon(ic)}<span>${esc(t(label))}</span></a>`).join('')}
      </nav>`
    : '';
  return `${header}<main class="content ${tabs ? 'with-tabs' : ''}">${body}</main>${tabs}`;
}

// ---------------------------------------------------------------- screens

function profileForm(child, isSetup) {
  const c = child || {};
  const gender = c.gender || 'male';
  return `<form id="profileForm" data-form="profile" class="stack">
    ${isSetup ? `<p class="intro">${esc(t('setup_intro'))}</p>` : ''}
    <label class="field"><span>${esc(t('child_name'))}</span>
      <input name="name" required value="${esc(c.name)}" autocomplete="off"></label>
    <label class="field"><span>${esc(t('birth_date'))}</span>
      <input name="birthDate" type="date" required max="${today()}" value="${esc(c.birthDate)}"></label>
    <div class="segmented">
      <label><input type="radio" name="gender" value="male" ${gender === 'male' ? 'checked' : ''}><span>${esc(t('male'))}</span></label>
      <label><input type="radio" name="gender" value="female" ${gender === 'female' ? 'checked' : ''}><span>${esc(t('female'))}</span></label>
    </div>
    <label class="field"><span>${esc(t('blood_type'))}</span><input name="bloodType" value="${esc(c.bloodType)}"></label>
    <label class="field"><span>${esc(t('allergies'))}</span><textarea name="allergies" rows="2">${esc(c.allergies)}</textarea></label>
    <label class="field"><span>${esc(t('notes'))}</span><textarea name="notes" rows="3">${esc(c.notes)}</textarea></label>
    ${!isSetup ? `<p class="hint">${esc(t('birth_change_hint'))}</p>` : ''}
    <button class="btn primary" type="submit">${esc(t(isSetup ? 'start' : 'save'))}</button>
  </form>`;
}

function screenSetup() {
  return headerHtml({ title: t('setup_title') }) + `<main class="content">${profileForm(null, true)}</main>`;
}

function isIosBrowser() {
  return /iPhone|iPad|iPod/.test(navigator.userAgent) && !navigator.standalone;
}

function installBanner() {
  let dismissed = false;
  try { dismissed = localStorage.getItem('installDismissed') === '1'; } catch (_) { /* ignore */ }
  if (dismissed || !isIosBrowser()) return '';
  return `<div class="card banner">
    <strong>${esc(t('install_title'))}</strong>
    <p>${esc(t('install_steps'))}</p>
    <button class="btn small" data-action="dismiss-install">${esc(t('dismiss'))}</button>
  </div>`;
}

function vaccineRowMini(v) {
  return `<a class="card row" href="#/vaccines">
    <div class="grow"><div class="title">${esc(v.name)}</div><div class="sub">${esc(fmtDate(v.dueDate))}</div></div>
    ${chip(vaccineStatus(v))}
  </a>`;
}

function visitRowMini(v) {
  const title = [v.doctorName, v.specialty].filter(Boolean).join(' - ') || v.reason || t('visit');
  return `<a class="card row" href="#/visit/${v.id}">
    ${icon('steth', 'accent')}
    <div class="grow">
      <div class="title">${esc(title)}</div>
      <div class="sub">${esc(fmtDate(v.date))}${v.time ? ' • ' + esc(fmtTime(v.time)) : ''}</div>
      ${v.diagnosis ? `<div class="sub">${esc(v.diagnosis)}</div>` : ''}
    </div>
  </a>`;
}

function screenHome(route) {
  const c = state.child;
  const now = today();
  const total = state.vaccines.length;
  const taken = state.vaccines.filter((v) => v.takenDate).length;
  const overdue = state.vaccines.filter((v) => vaccineStatus(v, now) === 'overdue');
  const next = state.vaccines.filter((v) => !v.takenDate && v.dueDate >= now).slice(0, 4);
  const upcoming = state.visits.filter((v) => v.date >= now).sort((a, b) => a.date.localeCompare(b.date)).slice(0, 3);
  const last = state.visits.find((v) => v.date <= now);

  const body = `
    ${installBanner()}
    <div class="card hero">
      ${icon('baby', 'hero-ic')}
      <div>
        <div class="hero-name">${esc(c.name)}</div>
        <div>${esc(ageText(ageOn(c.birthDate)))}</div>
        <div class="sub">${esc(t('birth_date'))}: ${esc(fmtDate(c.birthDate))}</div>
      </div>
    </div>
    ${total ? `<a class="card" href="#/vaccines">
      <div class="title">${esc(t('vaccine_progress', taken, total))}</div>
      <div class="progress"><div style="width:${Math.round((taken / total) * 100)}%"></div></div>
    </a>` : ''}
    ${overdue.length ? `<a class="card danger" href="#/vaccines">
      <div class="title">${icon('warn')} ${esc(t('overdue_count', overdue.length))}</div>
      ${overdue.slice(0, 5).map((v) => `<div class="sub">• ${esc(v.name)} (${esc(fmtDate(v.dueDate))})</div>`).join('')}
    </a>` : ''}
    <h2>${esc(t('next_vaccines'))}</h2>
    ${next.length ? next.map(vaccineRowMini).join('') : `<p class="muted">${esc(t('no_upcoming_vaccines'))}</p>`}
    <h2>${esc(t('upcoming_visits'))}</h2>
    ${upcoming.length ? upcoming.map(visitRowMini).join('') : `<p class="muted">${esc(t('no_upcoming_visits'))}</p>`}
    <a class="btn outline" href="#/visit-edit/0">${icon('plus')} ${esc(t('add_visit'))}</a>
    ${last ? `<h2>${esc(t('last_visit'))}</h2>${visitRowMini(last)}` : ''}
  `;
  const actions = `<button class="iconbtn" data-action="share-report" aria-label="${esc(t('share_report'))}">${icon('share')}</button>`;
  return layout(route, headerHtml({ title: t('app_name'), actions }), body);
}

function screenVaccines(route) {
  const all = state.vaccines;
  const list = state.filter === 'remaining' ? all.filter((v) => !v.takenDate)
    : state.filter === 'taken' ? all.filter((v) => v.takenDate) : all;
  const groups = new Map();
  list.forEach((v) => {
    const k = groupKey(v);
    if (!groups.has(k)) groups.set(k, []);
    groups.get(k).push(v);
  });
  const keys = [...groups.keys()].sort((a, b) => a - b);
  const now = today();
  const filterBtn = (f, label, n) => `<button class="${state.filter === f ? 'on' : ''}" data-action="filter" data-filter="${f}">${esc(t(label, n))}</button>`;

  const body = `
    <div class="filters">
      ${filterBtn('all', 'filter_all', all.length)}
      ${filterBtn('remaining', 'filter_remaining', all.filter((v) => !v.takenDate).length)}
      ${filterBtn('taken', 'filter_taken', all.filter((v) => v.takenDate).length)}
    </div>
    ${list.length === 0 ? `<div class="empty">${icon('syringe')}<p>${esc(t('no_vaccines'))}</p></div>` : ''}
    ${keys.map((k) => `<h2>${esc(groupLabel(k))}</h2>${groups.get(k).map((v) => {
      const s = vaccineStatus(v, now);
      return `<div class="card row vaccine">
        <button class="toggle ${s}" data-action="toggle-vaccine" data-id="${v.id}" aria-label="${esc(t('mark_taken'))}">${icon(v.takenDate ? 'check' : 'circle')}</button>
        <div class="grow" data-action="edit-vaccine" data-id="${v.id}">
          <div class="title">${esc(v.name)}</div>
          ${v.description ? `<div class="sub">${esc(v.description)}</div>` : ''}
          <div class="sub status-${s}">${esc(v.takenDate ? t('taken_on', fmtDate(v.takenDate)) : t('due_on', fmtDate(v.dueDate)))}</div>
          ${v.notes ? `<div class="sub">📝 ${esc(v.notes)}</div>` : ''}
        </div>
        ${chip(s)}
      </div>`;
    }).join('')}`).join('')}
    <p class="hint">${esc(t('schedule_disclaimer'))}</p>
  `;
  const actions = `
    <button class="iconbtn" data-action="vaccine-menu" aria-label="${esc(t('settings'))}">${icon('more')}</button>
    <button class="iconbtn" data-action="new-vaccine" aria-label="${esc(t('add_vaccine'))}">${icon('plus')}</button>`;
  return layout(route, headerHtml({ title: t('tab_vaccines'), actions }), body);
}

function visitCard(v, upcoming) {
  const doctor = [v.doctorName, v.specialty].filter(Boolean).join(' - ');
  const meds = v.medications || [];
  return `<a class="card visit ${upcoming ? 'upcoming' : ''}" href="#/visit/${v.id}">
    <div class="visit-top">
      <span class="date">${esc(fmtDate(v.date))}${v.time ? ' • ' + esc(fmtTime(v.time)) : ''}</span>
      <span class="badges">${v.photo ? icon('camera') : ''}${meds.length ? `${icon('pill')}<span>${meds.length}</span>` : ''}</span>
    </div>
    ${doctor ? `<div class="title">${esc(doctor)}</div>` : ''}
    ${v.reason ? `<div class="sub">${esc(t('reason'))}: ${esc(v.reason)}</div>` : ''}
    ${v.diagnosis ? `<div class="sub">${esc(t('diagnosis'))}: ${esc(v.diagnosis)}</div>` : ''}
    ${meds.length ? `<div class="sub muted">${esc(meds.map((m) => m.name).join('، '))}</div>` : ''}
  </a>`;
}

function screenVisits(route) {
  const now = today();
  const upcoming = state.visits.filter((v) => v.date > now).sort((a, b) => a.date.localeCompare(b.date));
  const past = state.visits.filter((v) => v.date <= now);
  const body = state.visits.length === 0
    ? `<div class="empty">${icon('steth')}<p>${esc(t('no_visits'))}</p></div>`
    : `${upcoming.length ? `<h2>${esc(t('upcoming_visits'))}</h2>${upcoming.map((v) => visitCard(v, true)).join('')}` : ''}
       ${past.length ? `<h2>${esc(t('past_visits'))}</h2>${past.map((v) => visitCard(v, false)).join('')}` : ''}`;
  const actions = `<button class="iconbtn" data-action="go" data-href="#/visit-edit/0" aria-label="${esc(t('add_visit'))}">${icon('plus')}</button>`;
  return layout(route, headerHtml({ title: t('tab_visits'), actions }),
    body + `<a class="btn primary" href="#/visit-edit/0">${icon('plus')} ${esc(t('add_visit'))}</a>`);
}

function labeled(label, value) {
  if (value == null || value === '') return '';
  return `<div class="kv"><span>${esc(label)}</span><b>${esc(value)}</b></div>`;
}

function screenVisitDetail(route, id) {
  const v = state.visits.find((x) => x.id === id);
  if (!v) return layout(route, headerHtml({ title: t('visit_details'), back: true }), '');
  const meds = v.medications || [];
  const future = v.date >= today();
  const actions = `
    ${future ? `<button class="iconbtn" data-action="visit-calendar" data-id="${v.id}" aria-label="${esc(t('add_to_calendar'))}">${icon('calendar')}</button>` : ''}
    <button class="iconbtn" data-action="share-visit" data-id="${v.id}" aria-label="${esc(t('share'))}">${icon('share')}</button>
    <button class="iconbtn" data-action="go" data-href="#/visit-edit/${v.id}" aria-label="${esc(t('edit'))}">${icon('edit')}</button>
    <button class="iconbtn danger" data-action="delete-visit" data-id="${v.id}" aria-label="${esc(t('delete'))}">${icon('trash')}</button>`;
  const body = `
    <div class="bigdate">${esc(fmtDate(v.date))}${v.time ? ' • ' + esc(fmtTime(v.time)) : ''}</div>
    <div class="card">
      ${labeled(t('doctor'), v.doctorName)}
      ${labeled(t('specialty'), v.specialty)}
      ${labeled(t('clinic'), v.clinic)}
      ${labeled(t('reason'), v.reason)}
      ${labeled(t('diagnosis'), v.diagnosis)}
      ${labeled(t('weight_kg'), pretty(v.weightKg))}
      ${labeled(t('height_cm'), pretty(v.heightCm))}
      ${labeled(t('temperature_c'), pretty(v.temperatureC))}
    </div>
    <h2>${esc(t('prescription'))}</h2>
    ${meds.length ? meds.map((m) => `<div class="card row">${icon('pill', 'accent')}<div class="grow">
        <div class="title">${esc(m.name)}</div>
        ${m.dose ? `<div class="sub">${esc(t('dose'))}: ${esc(m.dose)}</div>` : ''}
        ${m.frequency ? `<div class="sub">${esc(t('frequency'))}: ${esc(m.frequency)}</div>` : ''}
        ${m.duration ? `<div class="sub">${esc(t('duration'))}: ${esc(m.duration)}</div>` : ''}
      </div></div>`).join('') : `<p class="muted">${esc(t('no_medications'))}</p>`}
    ${v.photo ? `<h2>${esc(t('prescription_photo'))}</h2>
      <img class="photo" src="${v.photo}" alt="${esc(t('prescription_photo'))}" data-action="view-photo" data-id="${v.id}">` : ''}
    ${v.notes ? `<h2>${esc(t('notes'))}</h2><p class="pre">${esc(v.notes)}</p>` : ''}
  `;
  return layout(route, headerHtml({ title: t('visit_details'), back: true, actions }), body);
}

function medRow(m = {}) {
  return `<div class="card med-row stack">
    <div class="row">
      <label class="field grow"><span>${esc(t('medicine_name'))}</span><input data-med="name" list="medList" autocomplete="off" value="${esc(m.name)}"></label>
      <button type="button" class="iconbtn" data-action="open-med-picker" aria-label="${esc(t('choose_from_list'))}">${icon('list')}</button>
      <button type="button" class="iconbtn danger" data-action="remove-med" aria-label="${esc(t('delete'))}">${icon('trash')}</button>
    </div>
    <label class="field"><span>${esc(t('dose_hint'))}</span><input data-med="dose" value="${esc(m.dose)}"></label>
    <div class="row gap">
      <label class="field grow"><span>${esc(t('frequency_hint'))}</span><input data-med="frequency" value="${esc(m.frequency)}"></label>
      <label class="field grow"><span>${esc(t('duration_hint'))}</span><input data-med="duration" value="${esc(m.duration)}"></label>
    </div>
  </div>`;
}

function photoBlock() {
  return editPhoto
    ? `<div class="photo-wrap"><img class="photo" src="${editPhoto}" alt="">
        <button type="button" class="iconbtn on-photo" data-action="remove-photo" aria-label="${esc(t('remove_photo'))}">${icon('close')}</button></div>`
    : '';
}

/** The "read prescription" area under the photo. */
function scanBlock() {
  if (!editPhoto) return '';
  if (!state.aiKey) return `<p class="hint">${esc(t('scan_need_key'))}</p>`;
  if (scanning) return `<div class="scan-status"><span class="spinner"></span>${esc(t('scanning'))}</div>`;
  return `<button type="button" class="btn outline" data-action="scan-rx">${icon('sparkle')} ${esc(t('scan_btn'))}</button>`;
}

function refreshScanBlock() {
  const el = document.getElementById('scanBlock');
  if (el) el.innerHTML = scanBlock();
}

/** Fills the visit form from a scanned prescription without overwriting what the parent typed. */
function applyScan(form, r) {
  const setIfEmpty = (name, value) => {
    const field = form.elements[name];
    if (field && value && !field.value.trim()) field.value = value;
  };
  setIfEmpty('diagnosis', r.diagnosis);
  setIfEmpty('doctorName', r.doctor_name);
  setIfEmpty('specialty', r.specialty);
  setIfEmpty('clinic', r.clinic);
  setIfEmpty('notes', r.notes);
  // A new visit defaults to today; use the date written on the prescription instead.
  if (Number(form.dataset.id) === 0 && /^\d{4}-\d{2}-\d{2}$/.test(r.visit_date || '') && r.visit_date <= today()) {
    form.elements.date.value = r.visit_date;
  }
  const existing = new Set([...form.querySelectorAll('[data-med="name"]')].map((i) => i.value.trim().toLowerCase()).filter(Boolean));
  const meds = document.getElementById('meds');
  // Drop empty rows left from "add medicine" so scanned lines aren't mixed with blanks.
  meds.querySelectorAll('.med-row').forEach((row) => {
    if (![...row.querySelectorAll('input')].some((i) => i.value.trim())) row.remove();
  });
  (r.medications || []).filter((m) => m.name && !existing.has(m.name.trim().toLowerCase()))
    .forEach((m) => meds.insertAdjacentHTML('beforeend', medRow(m)));
}

async function scanPrescription() {
  const form = document.getElementById('visitForm');
  if (!form || !editPhoto || !state.aiKey || scanning) return;
  scanning = true;
  refreshScanBlock();
  const photo = editPhoto;
  try {
    const result = await readPrescription({ apiKey: state.aiKey, dataUrl: photo, lang: state.lang });
    // Ignore the result if the parent left the form or changed the photo meanwhile.
    if (document.getElementById('visitForm') !== form || editPhoto !== photo) return;
    if (!result.is_prescription) { toast(t('scan_not_rx')); return; }
    applyScan(form, result);
    toast(t('scan_done'));
  } catch (err) {
    toast(t({ key: 'scan_err_key', offline: 'scan_err_offline', refusal: 'scan_err_refusal' }[err.code] || 'scan_err'));
  } finally {
    scanning = false;
    refreshScanBlock();
  }
}

// ---------------------------------------------------------------- medicine picker

function medicinePickerList(query) {
  const q = query.trim().toLowerCase();
  const match = (m) => !q || [m.ar, m.en, ...m.brands].some((x) => x.toLowerCase().includes(q));
  const groups = MEDICINE_GROUPS
    .map((g) => ({ g, items: g.items.filter(match) }))
    .filter((x) => x.items.length);
  if (!groups.length) return `<p class="muted">${esc(t('no_results'))}</p>`;
  return groups.map(({ g, items }) => `<h2>${esc(state.lang === 'ar' ? g.ar : g.en)}</h2>
    ${items.map((m) => {
      const generic = state.lang === 'ar' ? m.ar : m.en;
      return `<div class="card med-pick">
        <button type="button" class="med-generic" data-action="pick-medicine" data-name="${esc(generic)}">${esc(generic)}
          ${state.lang === 'ar' ? `<span class="sub">${esc(m.en)}</span>` : ''}</button>
        ${m.brands.length ? `<div class="brands">${m.brands.map((b) => `<button type="button" class="brand" data-action="pick-medicine" data-name="${esc(`${b} (${generic})`)}">${esc(b)}</button>`).join('')}</div>` : ''}
      </div>`;
    }).join('')}`).join('');
}

function openMedicinePicker(row) {
  pickerTarget = row;
  openSheet(`<div class="stack picker">
    <div class="row">
      <h3 class="grow">${esc(t('choose_from_list'))}</h3>
      <button type="button" class="iconbtn" data-action="close-sheet" aria-label="${esc(t('close'))}">${icon('close')}</button>
    </div>
    <input type="search" data-action="med-search" placeholder="${esc(t('search_medicine'))}" autocomplete="off">
    <p class="hint">${esc(t('med_list_hint'))}</p>
    <div id="medPickerList">${medicinePickerList('')}</div>
  </div>`);
}

function screenVisitEdit(route, id) {
  const existing = id ? state.visits.find((x) => x.id === id) : null;
  const v = existing || { date: today(), medications: [] };
  const actions = `<button class="textbtn" type="submit" form="visitForm">${esc(t('save'))}</button>`;
  const body = `<form id="visitForm" data-form="visit" data-id="${existing ? existing.id : 0}" class="stack">
    <div class="row gap">
      <label class="field grow"><span>${esc(t('visit_date'))}</span><input type="date" name="date" required value="${esc(v.date)}"></label>
      <label class="field"><span>${esc(t('time_hint'))}</span><input type="time" name="time" value="${esc(v.time)}"></label>
    </div>
    <label class="field"><span>${esc(t('doctor_name'))}</span><input name="doctorName" value="${esc(v.doctorName)}"></label>
    <label class="field"><span>${esc(t('specialty'))}</span><input name="specialty" value="${esc(v.specialty)}"></label>
    <label class="field"><span>${esc(t('clinic'))}</span><input name="clinic" value="${esc(v.clinic)}"></label>
    <label class="field"><span>${esc(t('reason'))}</span><textarea name="reason" rows="2">${esc(v.reason)}</textarea></label>
    <label class="field"><span>${esc(t('diagnosis'))}</span><textarea name="diagnosis" rows="2">${esc(v.diagnosis)}</textarea></label>
    <h2>${esc(t('measurements'))}</h2>
    <div class="row gap">
      <label class="field grow"><span>${esc(t('weight_kg'))}</span><input name="weightKg" inputmode="decimal" value="${esc(pretty(v.weightKg))}"></label>
      <label class="field grow"><span>${esc(t('height_cm'))}</span><input name="heightCm" inputmode="decimal" value="${esc(pretty(v.heightCm))}"></label>
      <label class="field grow"><span>${esc(t('temperature_c'))}</span><input name="temperatureC" inputmode="decimal" value="${esc(pretty(v.temperatureC))}"></label>
    </div>
    <h2>${esc(t('prescription_photo'))}</h2>
    <div id="photoBlock">${photoBlock()}</div>
    <label class="btn outline file">${icon('camera')} <span id="photoLabel">${esc(t(editPhoto ? 'change_photo' : 'choose_photo'))}</span>
      <input type="file" accept="image/*" data-action="pick-photo" hidden></label>
    <div id="scanBlock">${scanBlock()}</div>
    <h2>${esc(t('prescription'))}</h2>
    <div id="meds" class="stack">${(v.medications || []).map(medRow).join('')}</div>
    <div class="row gap">
      <button type="button" class="btn outline grow" data-action="add-med">${icon('plus')} ${esc(t('add_medication'))}</button>
      <button type="button" class="btn outline grow" data-action="add-from-list">${icon('list')} ${esc(t('add_from_list'))}</button>
    </div>
    <datalist id="medList">${medicineSuggestions(state.lang).map((n) => `<option value="${esc(n)}"></option>`).join('')}</datalist>
    <label class="field"><span>${esc(t('notes'))}</span><textarea name="notes" rows="3">${esc(v.notes)}</textarea></label>
    <button class="btn primary" type="submit">${esc(t('save'))}</button>
  </form>`;
  return layout(route, headerHtml({ title: t(existing ? 'edit_visit' : 'add_visit'), back: true, actions }), body);
}

function screenProfile(route) {
  const body = `${profileForm(state.child, false)}
    <h2>${esc(t('settings'))}</h2>
    <div class="card stack">
      <label class="field"><span>${esc(t('language'))}</span>
        <select data-action="language">
          <option value="ar" ${state.lang === 'ar' ? 'selected' : ''}>العربية</option>
          <option value="en" ${state.lang === 'en' ? 'selected' : ''}>English</option>
        </select></label>
      <button class="btn outline" data-action="calendar-all">${icon('calendar')} ${esc(t('calendar_all'))}</button>
      <p class="hint">${esc(t('calendar_hint'))}</p>
    </div>
    <h2>${esc(t('ai_title'))}</h2>
    <form class="card stack" data-form="ai-key">
      <p class="hint">${esc(t('ai_hint'))}</p>
      <label class="field"><span>${esc(t('ai_key'))}</span>
        <input type="password" name="key" autocomplete="off" placeholder="${state.aiKey ? '••••••••' + esc(state.aiKey.slice(-4)) : 'sk-ant-…'}"></label>
      <button class="btn outline" type="submit">${esc(t('save'))}</button>
      ${state.aiKey ? `<button type="button" class="btn danger-outline" data-action="remove-ai-key">${esc(t('remove_key'))}</button>` : ''}
    </form>
    <h2>${esc(t('backup'))}</h2>
    <div class="card stack">
      <p class="hint">${esc(t('backup_hint'))}</p>
      <button class="btn outline" data-action="export-backup">${esc(t('export_backup'))}</button>
      <label class="btn outline file">${esc(t('import_backup'))}
        <input type="file" accept="application/json,.json" data-action="import-backup" hidden></label>
      <button class="btn danger-outline" data-action="erase-all">${esc(t('erase_all'))}</button>
    </div>`;
  return layout(route, headerHtml({ title: t('tab_profile') }), body);
}

// ---------------------------------------------------------------- sheets (dialogs)

function openSheet(html) {
  closeSheet();
  const wrap = document.createElement('div');
  wrap.className = 'sheet-backdrop';
  wrap.innerHTML = `<div class="sheet" role="dialog">${html}</div>`;
  wrap.addEventListener('click', (e) => { if (e.target === wrap) closeSheet(); });
  document.body.appendChild(wrap);
  requestAnimationFrame(() => wrap.classList.add('open'));
}

function closeSheet() {
  document.querySelectorAll('.sheet-backdrop').forEach((el) => el.remove());
}

function vaccineSheet(v) {
  const isNew = !v.id;
  openSheet(`<form data-form="vaccine" data-id="${v.id || 0}" class="stack">
    <h3>${esc(t(isNew ? 'add_vaccine' : 'edit_vaccine'))}</h3>
    <label class="field"><span>${esc(t('vaccine_name'))}</span><input name="name" required value="${esc(v.name)}"></label>
    <label class="field"><span>${esc(t('description'))}</span><input name="description" value="${esc(v.description)}"></label>
    <label class="field"><span>${esc(t('due_date'))}</span><input type="date" name="dueDate" required value="${esc(v.dueDate)}"></label>
    <label class="switch"><span>${esc(t('mark_taken'))}</span><input type="checkbox" name="taken" ${v.takenDate ? 'checked' : ''} data-action="taken-switch"><i></i></label>
    <label class="field" id="takenDateField" ${v.takenDate ? '' : 'hidden'}><span>${esc(t('taken_date'))}</span>
      <input type="date" name="takenDate" value="${esc(v.takenDate || today())}"></label>
    <label class="field"><span>${esc(t('vaccine_notes_hint'))}</span><textarea name="notes" rows="2">${esc(v.notes)}</textarea></label>
    <div class="row gap">
      <button type="button" class="btn outline grow" data-action="close-sheet">${esc(t('cancel'))}</button>
      <button type="submit" class="btn primary grow">${esc(t('save'))}</button>
    </div>
    ${!isNew ? `<button type="button" class="btn danger-outline" data-action="delete-vaccine" data-id="${v.id}">${esc(t('delete'))}</button>` : ''}
  </form>`);
}

// ---------------------------------------------------------------- routing

function go(hash) {
  if (location.hash === hash) render(); else location.hash = hash;
}

let lastHash = null;

function render() {
  const app = document.getElementById('app');
  closeSheet();
  if (!state.child) {
    app.innerHTML = screenSetup();
    return;
  }
  const hash = location.hash || '#/home';
  const [, name, param] = hash.split('/');
  const id = Number(param) || 0;
  let html;
  switch (name) {
    case 'vaccines': html = screenVaccines('#/vaccines'); break;
    case 'visits': html = screenVisits('#/visits'); break;
    case 'profile': html = screenProfile('#/profile'); break;
    case 'visit': html = screenVisitDetail(hash, id); break;
    case 'visit-edit': {
      const existing = id ? state.visits.find((x) => x.id === id) : null;
      editPhoto = existing?.photo || null;
      html = screenVisitEdit(hash, id);
      break;
    }
    default: html = screenHome('#/home');
  }
  app.innerHTML = html;
  if (hash !== lastHash) window.scrollTo(0, 0);
  lastHash = hash;
}

// ---------------------------------------------------------------- events

const actions = {
  back: () => { if (history.length > 1) history.back(); else go('#/home'); },
  go: (el) => go(el.dataset.href),
  'share-report': () => shareText(fullReport()),
  'share-visit': (el) => {
    const v = state.visits.find((x) => x.id === Number(el.dataset.id));
    if (v) shareText(visitText(v));
  },
  'visit-calendar': (el) => {
    const v = state.visits.find((x) => x.id === Number(el.dataset.id));
    if (v) openCalendar([visitEvent(v)], 'visit.ics');
  },
  'calendar-all': () => {
    const now = today();
    const events = [
      ...state.vaccines.filter((v) => !v.takenDate && v.dueDate >= now).map(vaccineEvent),
      ...state.visits.filter((v) => v.date >= now).map(visitEvent),
    ];
    if (events.length === 0) { toast(t('no_upcoming_vaccines')); return; }
    openCalendar(events, 'child-health.ics');
  },
  'delete-visit': async (el) => {
    if (!confirm(t('confirm_delete_visit'))) return;
    await deleteVisit(Number(el.dataset.id));
    history.back();
  },
  'view-photo': (el) => {
    const v = state.visits.find((x) => x.id === Number(el.dataset.id));
    if (!v?.photo) return;
    const viewer = document.createElement('div');
    viewer.className = 'viewer';
    viewer.innerHTML = `<img src="${v.photo}" alt=""><button class="iconbtn" aria-label="${esc(t('close'))}">${icon('close')}</button>`;
    viewer.addEventListener('click', () => viewer.remove());
    document.body.appendChild(viewer);
  },
  filter: (el) => { state.filter = el.dataset.filter; render(); },
  'toggle-vaccine': async (el) => {
    const v = state.vaccines.find((x) => x.id === Number(el.dataset.id));
    if (!v) return;
    await saveVaccine({ ...v, takenDate: v.takenDate ? null : today() });
    render();
  },
  'edit-vaccine': (el) => {
    const v = state.vaccines.find((x) => x.id === Number(el.dataset.id));
    if (v) vaccineSheet(v);
  },
  'new-vaccine': () => vaccineSheet({ name: '', description: '', dueDate: today(), takenDate: null, notes: '' }),
  'vaccine-menu': () => openSheet(`<div class="stack">
      <button class="btn outline" data-action="add-optional">${icon('plus')} ${esc(t('add_optional_vaccines'))}</button>
      <button class="btn outline" data-action="calendar-all">${icon('calendar')} ${esc(t('calendar_all'))}</button>
      <p class="hint">${esc(t('calendar_hint'))}</p>
      <button class="btn" data-action="close-sheet">${esc(t('cancel'))}</button>
    </div>`),
  'add-optional': async () => {
    const n = await addOptionalVaccines();
    render();
    toast(t('optional_added', n));
  },
  'close-sheet': () => closeSheet(),
  'delete-vaccine': async (el) => {
    const v = state.vaccines.find((x) => x.id === Number(el.dataset.id));
    if (!v || !confirm(t('confirm_delete_vaccine', v.name))) return;
    await deleteVaccine(v.id);
    render();
  },
  'add-med': () => {
    document.getElementById('meds').insertAdjacentHTML('beforeend', medRow());
    const rows = document.querySelectorAll('.med-row');
    rows[rows.length - 1].querySelector('input').focus();
  },
  'remove-med': (el) => el.closest('.med-row').remove(),
  'remove-photo': () => {
    editPhoto = null;
    document.getElementById('photoBlock').innerHTML = '';
    document.getElementById('photoLabel').textContent = t('choose_photo');
    refreshScanBlock();
  },
  'scan-rx': () => scanPrescription(),
  'open-med-picker': (el) => openMedicinePicker(el.closest('.med-row')),
  'add-from-list': () => openMedicinePicker(null),
  'pick-medicine': (el) => {
    let row = pickerTarget && document.body.contains(pickerTarget) ? pickerTarget : null;
    if (!row) {
      const meds = document.getElementById('meds');
      if (!meds) return;
      meds.insertAdjacentHTML('beforeend', medRow());
      row = meds.lastElementChild;
    }
    row.querySelector('[data-med="name"]').value = el.dataset.name;
    closeSheet();
    pickerTarget = null;
    row.querySelector('[data-med="dose"]').focus();
  },
  'remove-ai-key': async () => {
    state.aiKey = '';
    await db.kvSet('aiKey', '');
    render();
    toast(t('ai_removed'));
  },
  'dismiss-install': (el) => {
    try { localStorage.setItem('installDismissed', '1'); } catch (_) { /* ignore */ }
    el.closest('.banner').remove();
  },
  'export-backup': () => exportBackup(),
  'erase-all': () => eraseAll(),
};

const changeActions = {
  'taken-switch': (el) => { document.getElementById('takenDateField').hidden = !el.checked; },
  'pick-photo': async (el) => {
    const file = el.files?.[0];
    if (!file) return;
    editPhoto = await compressImage(file);
    el.value = '';
    document.getElementById('photoBlock').innerHTML = photoBlock();
    document.getElementById('photoLabel').textContent = t('change_photo');
    refreshScanBlock();
    // Read the new photo straight away when a key is set.
    if (state.aiKey) scanPrescription();
  },
  'import-backup': async (el) => {
    const file = el.files?.[0];
    el.value = '';
    if (file) await importBackup(file);
  },
  language: (el) => {
    state.lang = el.value;
    try { localStorage.setItem('lang', state.lang); } catch (_) { /* ignore */ }
    applyLang();
    render();
  },
};

const forms = {
  'ai-key': async (form) => {
    const key = String(new FormData(form).get('key') || '').trim();
    if (!key) return;
    state.aiKey = key;
    await db.kvSet('aiKey', key);
    render();
    toast(t('ai_saved'));
  },
  profile: async (form) => {
    const f = new FormData(form);
    const name = String(f.get('name') || '').trim();
    const birthDate = String(f.get('birthDate') || '');
    if (!name) { toast(t('error_name')); return; }
    if (!birthDate) { toast(t('error_birth')); return; }
    const isSetup = !state.child;
    await saveChild({
      name,
      birthDate,
      gender: f.get('gender') || 'male',
      bloodType: String(f.get('bloodType') || '').trim(),
      allergies: String(f.get('allergies') || '').trim(),
      notes: String(f.get('notes') || '').trim(),
    });
    if (isSetup) {
      if ('storage' in navigator && navigator.storage.persist) navigator.storage.persist().catch(() => {});
      go('#/home');
    } else {
      toast(t('saved'));
    }
  },
  vaccine: async (form) => {
    const f = new FormData(form);
    const id = Number(form.dataset.id);
    const initial = id ? state.vaccines.find((x) => x.id === id) : null;
    const dueDate = String(f.get('dueDate'));
    // Moving a scheduled dose's date turns it into a custom entry so a birth-date edit won't move it back.
    const keepSchedule = initial && initial.offsetMonths != null && initial.dueDate === dueDate;
    const v = {
      ...(initial || { mandatory: false, sortOrder: 0, offsetDays: 0 }),
      name: String(f.get('name')).trim(),
      description: String(f.get('description') || '').trim(),
      dueDate,
      offsetMonths: keepSchedule ? initial.offsetMonths : null,
      takenDate: f.get('taken') ? String(f.get('takenDate') || today()) : null,
      notes: String(f.get('notes') || '').trim(),
    };
    if (!id) delete v.id;
    await saveVaccine(v);
    render();
  },
  visit: async (form) => {
    const f = new FormData(form);
    const id = Number(form.dataset.id);
    const s = (k) => String(f.get(k) || '').trim();
    const medications = [...form.querySelectorAll('.med-row')].map((row) => {
      const val = (k) => row.querySelector(`[data-med="${k}"]`).value.trim();
      return { name: val('name'), dose: val('dose'), frequency: val('frequency'), duration: val('duration') };
    }).filter((m) => m.name);
    const visit = {
      id: id || undefined,
      date: s('date') || today(),
      time: s('time'),
      doctorName: s('doctorName'),
      specialty: s('specialty'),
      clinic: s('clinic'),
      reason: s('reason'),
      diagnosis: s('diagnosis'),
      weightKg: parseDecimal(f.get('weightKg')),
      heightCm: parseDecimal(f.get('heightCm')),
      temperatureC: parseDecimal(f.get('temperatureC')),
      notes: s('notes'),
      medications,
      photo: editPhoto,
    };
    const savedId = await saveVisit(visit);
    if (id) history.back(); else location.replace(`#/visit/${savedId}`);
  },
};

document.addEventListener('click', (e) => {
  const el = e.target.closest('[data-action]');
  if (!el || el.tagName === 'SELECT' || el.type === 'file' || el.type === 'checkbox') return;
  const fn = actions[el.dataset.action];
  if (fn) { e.preventDefault(); fn(el, e); }
});

document.addEventListener('change', (e) => {
  const fn = changeActions[e.target.dataset?.action];
  if (fn) fn(e.target, e);
});

document.addEventListener('input', (e) => {
  if (e.target.dataset?.action === 'med-search') {
    document.getElementById('medPickerList').innerHTML = medicinePickerList(e.target.value);
  }
});

document.addEventListener('submit', (e) => {
  const form = e.target;
  const fn = forms[form.dataset.form];
  if (fn) { e.preventDefault(); fn(form); }
});

window.addEventListener('hashchange', render);

// ---------------------------------------------------------------- start

async function start() {
  state.lang = detectLang();
  applyLang();
  await loadAll();
  state.aiKey = (await db.kvGet('aiKey')) || '';
  render();
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }
}

start();
