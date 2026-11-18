// Reads a photo of a doctor's prescription (روشتة) with Claude and returns structured data.
// The SDK is bundled locally (vendor/) and loaded only when a scan is requested,
// so the rest of the app keeps working offline.
//
// The API key belongs to the parent and is stored only on their phone; the request goes
// straight from the phone to the Anthropic API (hence dangerouslyAllowBrowser).

const MODEL = 'claude-opus-5-5';

const MED_SCHEMA = {
  type: 'object',
  properties: {
    name: { type: 'string' },
    dose: { type: 'string' },
    frequency: { type: 'string' },
    duration: { type: 'string' },
  },
  required: ['name', 'dose', 'frequency', 'duration'],
  additionalProperties: false,
};

const SCHEMA = {
  type: 'object',
  properties: {
    is_prescription: { type: 'boolean' },
    diagnosis: { type: 'string' },
    doctor_name: { type: 'string' },
    specialty: { type: 'string' },
    clinic: { type: 'string' },
    visit_date: { type: 'string', description: 'YYYY-MM-DD, or empty if not written' },
    medications: { type: 'array', items: MED_SCHEMA },
    notes: { type: 'string' },
  },
  required: ['is_prescription', 'diagnosis', 'doctor_name', 'specialty', 'clinic', 'visit_date', 'medications', 'notes'],
  additionalProperties: false,
};

function systemPrompt(lang) {
  const language = lang === 'ar' ? 'Egyptian Arabic' : 'English';
  return `You read photos of doctors' prescriptions for a parent's child-health tracking app in Egypt. \
The parent photographs the prescription after a pediatric visit, and your output pre-fills the visit form; \
the parent reviews and corrects it before saving.

Prescriptions are often handwritten, mix Arabic and English, and use Latin abbreviations \
(e.g. "tds", "bid", "q8h", "x 5d", "susp", "supp"). Read them the way an Egyptian pharmacist would.

- medications: one entry per medicine line. Keep the medicine name as written, in Latin letters, \
including strength and form when written (e.g. "Augmentin 457mg susp"). Write dose, frequency and \
duration in ${language} in plain words a parent understands (e.g. "5 ml", "every 8 hours", "5 days").
- diagnosis: the condition the doctor wrote, in ${language}; keep the medical term in brackets if useful.
- notes: other instructions on the paper (follow-up date, tests, diet), in ${language}.
- If something is not on the paper or you cannot read it confidently, return an empty string for it. \
Do not guess: a wrong medicine name is worse than a blank one the parent fills in.
- If the photo is not a prescription, set is_prescription to false and leave everything else empty.`;
}

export class ScanError extends Error {
  constructor(code) {
    super(code);
    this.code = code; // 'key' | 'offline' | 'refusal' | 'failed'
  }
}

/**
 * @param {{apiKey: string, dataUrl: string, lang: 'ar'|'en'}} opts
 * @returns {Promise<{is_prescription:boolean, diagnosis:string, doctor_name:string, specialty:string,
 *   clinic:string, visit_date:string, medications:{name:string,dose:string,frequency:string,duration:string}[], notes:string}>}
 */
export async function readPrescription({ apiKey, dataUrl, lang }) {
  let Anthropic;
  try {
    ({ default: Anthropic } = await import('./vendor/anthropic-sdk.js'));
  } catch (_) {
    throw new ScanError('offline');
  }
  const client = new Anthropic({ apiKey, dangerouslyAllowBrowser: true });
  const [meta, data] = dataUrl.split(',');
  const mediaType = (meta.match(/^data:(.*?);/) || [])[1] || 'image/jpeg';

  let response;
  try {
    response = await client.beta.messages.create({
      model: MODEL,
      max_tokens: 16000,
      // On a safety decline, let the API retry on a suitable fallback model.
      betas: ['server-side-fallback-2026-07-01'],
      fallbacks: 'default',
      output_config: {
        effort: 'high', // handwriting is hard; accuracy matters more than a few seconds
        format: { type: 'json_schema', schema: SCHEMA },
      },
      system: systemPrompt(lang),
      messages: [{
        role: 'user',
        content: [
          { type: 'image', source: { type: 'base64', media_type: mediaType, data } },
          { type: 'text', text: 'Read this prescription.' },
        ],
      }],
    });
  } catch (err) {
    if (err instanceof Anthropic.AuthenticationError || err instanceof Anthropic.PermissionDeniedError) {
      throw new ScanError('key');
    }
    if (err instanceof Anthropic.APIConnectionError) throw new ScanError('offline');
    throw new ScanError('failed');
  }

  if (response.stop_reason === 'refusal') throw new ScanError('refusal');
  if (response.stop_reason === 'max_tokens') throw new ScanError('failed');
  const text = response.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
  try {
    return JSON.parse(text);
  } catch (_) {
    throw new ScanError('failed');
  }
}
