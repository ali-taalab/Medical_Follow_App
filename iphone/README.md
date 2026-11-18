# نسخة الآيفون – Child Health Tracker (iPhone)

نفس تطبيق الأندرويد بس كـ **Web App (PWA)** بيتثبت على الآيفون من Safari ويشتغل من غير إنترنت.
مش محتاج Mac ولا App Store ولا حساب مطور.

## التثبيت على الآيفون

1. افتح رابط التطبيق في **Safari** (لازم Safari).
2. اضغط زر المشاركة ⬆️ ← **إضافة إلى الشاشة الرئيسية (Add to Home Screen)**.
3. افتحه من الأيقونة اللي ظهرت. هيفتح كأنه تطبيق عادي، وبيشتغل من غير نت.

## المميزات

- جدول التطعيمات الإجبارية (وزارة الصحة المصرية) بيتعمل من تاريخ الميلاد + حالة كل تطعيم + تطعيمات اختيارية/إضافية.
- زيارات الدكتور: التشخيص، القياسات، الروشتة (الأدوية بالجرعة والمدة)، وصورة الروشتة من الكاميرا أو الصور.
- **قراءة الروشتة تلقائياً**: لما ترفع صورة الروشتة، Claude (ذكاء اصطناعي من Anthropic) بيقرأ منها التشخيص والأدوية والجرعات ويملأ الخانات، وانت تراجعها قبل الحفظ. محتاج مفتاح API من [console.anthropic.com](https://console.anthropic.com) تحطه في صفحة "بيانات الطفل" (بيتحفظ على الآيفون بس، وكل صورة بتتحاسب بمبلغ صغير).
- **قائمة أدوية الأطفال**: اختار الدواء من قائمة بأشهر أدوية الأطفال من الولادة لـ 6 سنين (بالاسم العلمي والأسماء التجارية في مصر)، أو اكتب الاسم والتطبيق يقترح عليك. القائمة للاختيار السريع بس، ومفيهاش جرعات عن قصد لأن الجرعة بتتحدد حسب وزن الطفل.
- **التذكيرات عن طريق تطبيق التقويم**: تطبيقات الويب على الآيفون مقدرش تعمل إشعارات مجدولة لوحدها، فالتطبيق بيضيف المواعيد للتقويم بتنبيه الساعة 9 الصبح قبلها بيوم (من صفحة "بيانات الطفل" أو من قايمة التطعيمات أو من صفحة الزيارة).
- مشاركة التقرير كامل على واتساب.
- **نسخة احتياطية**: البيانات متخزنة على الآيفون بس، فاحفظ نسخة احتياطية كل فترة (مثلاً على iCloud Drive) من صفحة "بيانات الطفل".

## Hosting

The app is plain HTML/CSS/JS (no build step) in this folder. The `Publish iPhone app (GitHub Pages)`
workflow deploys it on every push to `main` that touches `iphone/`. One-time setup:
**Settings → Pages → Build and deployment → Source: GitHub Actions**. The app is then served at
`https://ali-taalab.github.io/Medical_Follow_App/`.

Prescription reading uses the official Anthropic TypeScript SDK (`@anthropic-ai/sdk` 1.4.0), bundled for the
browser into `vendor/anthropic-sdk.js` (`esbuild --bundle --format=esm --platform=browser --minify`) so it's
cached offline with the rest of the app and loaded only when a photo is read. The request goes directly from the
phone to the Anthropic API with the parent's own key (`dangerouslyAllowBrowser`), uses `claude-opus-5-5` with
structured JSON output (`output_config.format`), and fills only empty fields so nothing typed is overwritten.

Run locally: `cd iphone && python3 -m http.server 8000` and open http://localhost:8000.
