
import os, re, sys, collections

root = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/java"
srcs = []
for d, dirs, fs in os.walk(root):
    for f in fs:
        if f.endswith(".kt"):
            srcs.append(os.path.join(d, f))

decls = collections.defaultdict(set)
pkg_of = {}
for p in srcs:
    s = open(p, encoding="utf-8").read()
    m = re.search(r"^package\s+([\w.]+)", s, re.M)
    pkg = m.group(1) if m else "?"
    pkg_of[p] = pkg
    pat = re.compile(r"^(?:[A-Za-z@() \w]*?)(?:class|object|interface|enum class|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?([A-Za-z_]\w*)", re.M)
    for mm in pat.finditer(s):
        if mm.group(0).startswith("    "):
            continue
        decls[pkg].add(mm.group(1))
    for mm in re.finditer(r"^(?:[A-Za-z@ ]*?)(?:class|object|interface|enum class)\s+([A-Za-z_]\w*)", s, re.M):
        decls[pkg].add(mm.group(1))

allnames = set()
for v in decls.values():
    allnames |= v

print("== 可疑 import（本包内找不到该名字）==")
bad = 0
for p in srcs:
    for line in open(p, encoding="utf-8"):
        m = re.match(r"import\s+(com\.qwen\.opsmentor\.[\w.]+)", line)
        if not m:
            continue
        full = m.group(1)
        parts = full.split(".")
        pkg = ".".join(parts[2:-1])
        name = parts[-1]
        if pkg in decls and name in decls[pkg]:
            continue
        print("  %s <- %s" % (full, p))
        bad += 1
print("bad imports:", bad)

std = set("""String Integer Long Boolean Double Float Char Array List Map Set HashMap LinkedHashMap ArrayList
HashSet IntRange IntArray StringBuilder Runnable Handler Looper Uri File Bundle View ViewGroup TextView ImageView
EditText LinearLayout FrameLayout ScrollView HorizontalScrollView NestedScrollView GradientDrawable Paint Canvas Rect RectF
Path Color Matrix Bitmap BitmapFactory Typeface TextUtils TypedValue MotionEvent GestureDetector Dialog Window WindowManager
Gravity Build Locale SimpleDateFormat Date Calendar JSONObject JSONArray SharedPreferences Context Application Activity
AppCompatActivity MediaPlayer VideoView DocumentsContract OpenableColumns ContentValues Intent ClipData ClipboardManager
TextToSpeech UtteranceProgressListener ActivityResultContracts ActivityResultLauncher PathInterpolator ValueAnimator
AnimationSet AlphaAnimation TranslateAnimation ScaleAnimation DecelerateInterpolator ViewOutlineProvider Outline
PixelFormat DashPathEffect Shader LinearGradient RadialGradient TextUtils TruncateAt""".split())
use = re.compile(r"\b([A-Z][A-Za-z0-9]{2,})\b")
miss = collections.Counter()
for p in srcs:
    s = open(p, encoding="utf-8").read()
    for n in use.findall(s):
        if n in allnames or n in std:
            continue
        miss[n] += 1
print("== 未在本模块声明的大写标识符（前 40，含 android 系统类误报）==")
for k, v in miss.most_common(40):
    print("  %s x%d" % (k, v))
print("declared names:", len(allnames))
