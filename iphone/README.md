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
- **التذكيرات عن طريق تطبيق التقويم**: تطبيقات الويب على الآيفون مقدرش تعمل إشعارات مجدولة لوحدها، فالتطبيق بيضيف المواعيد للتقويم بتنبيه الساعة 9 الصبح قبلها بيوم (من صفحة "بيانات الطفل" أو من قايمة التطعيمات أو من صفحة الزيارة).
- مشاركة التقرير كامل على واتساب.
- **نسخة احتياطية**: البيانات متخزنة على الآيفون بس، فاحفظ نسخة احتياطية كل فترة (مثلاً على iCloud Drive) من صفحة "بيانات الطفل".

## Hosting

The app is plain HTML/CSS/JS (no build step) in this folder. The `Publish iPhone app (GitHub Pages)`
workflow deploys it on every push to `main` that touches `iphone/`. One-time setup:
**Settings → Pages → Build and deployment → Source: GitHub Actions**. The app is then served at
`https://ali-taalab.github.io/Medical_Follow_App/`.

Run locally: `cd iphone && python3 -m http.server 8000` and open http://localhost:8000.
