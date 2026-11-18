from pathlib import Path
import re
src = Path("app/src/main/java/com/qwen/opsmentor")

def edit(rel, fn):
    p = src / rel
    if not p.exists():
        return
    old = p.read_text(encoding="utf-8")
    new = fn(old)
    if new != old:
        p.write_text(new, encoding="utf-8")
        print("PATCH", rel)

def rep(rel, pairs):
    edit(rel, lambda s: __import__("functools").reduce(lambda v, pair: v.replace(pair[0], pair[1]), pairs, s))

rep("data/Pack.kt", [
    ('val quiz: Quiz, val content: String = "",\n)', 'val quiz: Quiz, val content: String = "", val media: MediaRef? = null,\n)'),
    ("Parts.from(", "Part.from("),
    ("val out: String, val read: String, val md: String,", 'val out: String, val read: String, val md: String = "",'),
])
rep("state/Courses.kt", [('val media: String = ,', 'val media: String = "",')])
rep("state/ReviewEngine.kt", [
    ("import com.qwen.opsmentor.data.Pack", "import com.qwen.opsmentor.data.Pack\nimport com.qwen.opsmentor.data.Chapter")
])

def dsl(s):
    s = s.replace("else LayoutParams(w, h)", "else ViewGroup.LayoutParams(w, h)")
    s = s.replace("TranslateAnimation(0f, 0f, dp(8f).toFloat(), 0f)", "TranslateAnimation(0f, 0f, context.dp(8f).toFloat(), 0f)")
    s = s.replace("ctx.dp(if (Tokens.planet) 12f else 12f))", "ctx.dp(if (Tokens.planet) 12f else 12f).toFloat())")
    if "fun muted(ctx: Context" not in s:
        s += '''

fun muted(ctx: Context, text: CharSequence, sizeSp: Float = 12.5f, maxLines: Int = 0): TextView =
    label(ctx, text, sizeSp, Tokens.sub, Tokens.weightB(), maxLines).apply { setLineSpacing(0f, 1.18f) }

fun mono(ctx: Context, text: String, output: Boolean = false): TextView = TextView(ctx).apply {
    this.text = text
    setTextColor(if (output) Tokens.sub else if (Tokens.dark) 0xFFCFE8D8.toInt() else 0xFF24533B.toInt())
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
    typeface = Typeface.MONOSPACE
    setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
    setLineSpacing(0f, 1.2f)
    flat(if (output) Tokens.l3 else Tokens.withAlpha(Tokens.pri, 0.08f), ctx.dp(10f).toFloat())
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
}

fun noteBox(ctx: Context, title: String, body: String): LinearLayout = vcol(ctx).apply {
    setPadding(ctx.dp(12f), ctx.dp(10f), ctx.dp(12f), ctx.dp(10f))
    flat(Tokens.priSoft, ctx.dp(10f).toFloat())
    if (title.isNotBlank()) addView(label(ctx, title, 13f, Tokens.pri, Tokens.weightM()))
    addView(muted(ctx, body, 12.5f))
}

fun emptyState(ctx: Context, icon: String, title: String, subtitle: String): LinearLayout = vcol(ctx).apply {
    gravity = Gravity.CENTER
    setPadding(ctx.dp(24f), ctx.dp(34f), ctx.dp(24f), ctx.dp(34f))
    addView(iconView(ctx, icon, Tokens.sub, 30f))
    addView(label(ctx, title, 15f, Tokens.ink, Tokens.weightM()),
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(12f) })
    addView(muted(ctx, subtitle, 12.5f),
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(5f) })
}
'''
    return s
edit("core/Dsl.kt", dsl)

# Bars is implemented in ui.Rows, not core.
for p in src.rglob("*.kt"):
    s = p.read_text(encoding="utf-8")
    if "import com.qwen.opsmentor.core.Bars" in s:
        p.write_text(s.replace("import com.qwen.opsmentor.core.Bars", "import com.qwen.opsmentor.ui.Bars"), encoding="utf-8")

# addView(..., t = ...) is the project's ViewGroup.add DSL, not Android's addView overload.
def convert_addview(p):
    s = p.read_text(encoding="utf-8")
    out, i, changed = [], 0, False
    while True:
        j = s.find(".addView(", i)
        if j < 0:
            out.append(s[i:])
            break
        out.append(s[i:j])
        k, depth, quote, esc = j + len(".addView("), 1, None, False
        while k < len(s) and depth:
            c = s[k]
            if quote:
                if esc: esc = False
                elif c == "\\\\": esc = True
                elif c == quote: quote = None
            else:
                if c in ('"', "'"): quote = c
                elif c == "(": depth += 1
                elif c == ")": depth -= 1
            k += 1
        chunk = s[j:k]
        if re.search(r"\bt\s*=", chunk):
            chunk = chunk.replace(".addView(", ".add(", 1)
            changed = True
        out.append(chunk)
        i = k
    new = "".join(out)
    if changed:
        if "package com.qwen.opsmentor.core" not in new and "import com.qwen.opsmentor.core.add" not in new:
            lines = new.splitlines()
            pos = 1
            while pos < len(lines) and (lines[pos].startswith("import ") or not lines[pos].strip()):
                pos += 1
            lines.insert(pos, "import com.qwen.opsmentor.core.add")
            new = "\n".join(lines) + "\n"
        p.write_text(new, encoding="utf-8")
        print("PATCH named addView", p)

for p in src.rglob("*.kt"):
    convert_addview(p)

rep("core/Components.kt", [
    ("ctx.dp(Tokens.rInner()))", "ctx.dp(Tokens.rInner()).toFloat())"),
    ("android.graphics.drawable.GradientDrawable.TL_BR", "android.graphics.drawable.GradientDrawable.Orientation.TL_BR"),
    ("attachedToWindow", "isAttachedToWindow"),
    ("Math.abs(Math.sin(t + i)).toFloat()", "kotlin.math.abs(kotlin.math.sin((t + i).toDouble())).toFloat()"),
])

# Serialization extensions are top-level functions in the data package.
for rel in ["io/Exporter.kt", "io/Importer.kt", "media/MediaCourses.kt"]:
    def add_import(s):
        if "import com.qwen.opsmentor.data.toJson" not in s:
            lines = s.splitlines()
            lines.insert(1, "import com.qwen.opsmentor.data.toJson")
            s = "\n".join(lines) + "\n"
        return s
    edit(rel, add_import)

rep("io/Exporter.kt", [
    ('val EXT = mapOf("json" to "json" to "JSON 存档", "md" to "md" to "Markdown 文档", "csv" to "csv" to "CSV 表格")',
     'val EXT = mapOf("json" to ("json" to "JSON 存档"), "md" to ("md" to "Markdown 文档"), "csv" to ("csv" to "CSV 表格"))')
])

def exporter(s):
    if "private object MarkdownKit" not in s:
        s += '''

private object MarkdownKit {
    fun plain(parts: List<com.qwen.opsmentor.data.Part>): String = parts.joinToString("") { it.text }
}
private fun org.json.JSONArray.joinToString(separator: String): String =
    (0 until length()).joinToString(separator) { optString(it) }
'''
    return s
edit("io/Exporter.kt", exporter)
edit("io/Importer.kt", lambda s: s if "private fun org.json.JSONArray.joinToString" in s else s + '''
private fun org.json.JSONArray.joinToString(separator: String): String =
    (0 until length()).joinToString(separator) { optString(it) }
''')

def media(s):
    if "import android.graphics.Bitmap" not in s:
        s = s.replace("import android.content.Context", "import android.content.Context\nimport android.graphics.Bitmap")
    if "import java.io.FileOutputStream" not in s:
        s = s.replace("import java.io.File", "import java.io.File\nimport java.io.FileOutputStream")
    s = s.replace("compareBy({ it.time }, { naturalKey(it.name) })", "compareBy<MediaItem>({ it.time }, { naturalKey(it.name) })")
    s = s.replace("compareBy({ naturalKey(it.name) }, { it.time })", "compareBy<MediaItem>({ naturalKey(it.name) }, { it.time })")
    return s
edit("media/MediaCourses.kt", media)

def main(s):
    for imp in ["import java.io.File", "import java.io.FileOutputStream", "import android.graphics.Bitmap", "import android.graphics.Canvas", "import android.graphics.Rect"]:
        if imp not in s:
            s = s.replace("package com.qwen.opsmentor.ui", "package com.qwen.opsmentor.ui\n" + imp, 1)
    return s.replace("job?.invoke(list)", "job?.invoke(uris)")
edit("ui/MainActivity.kt", main)

def mini(s):
    for imp in ["import com.qwen.opsmentor.core.flat", "import com.qwen.opsmentor.core.circle"]:
        if imp not in s:
            s = s.replace("import com.qwen.opsmentor.core.dp", "import com.qwen.opsmentor.core.dp\n" + imp, 1)
    return s.replace("fun render(view: String) {", "fun render(view: String) {\n        val ctx = context")
edit("ui/MiniBar.kt", mini)

def crop(s):
    s = s.replace("onDone: (String))", "onDone: (String) -> Unit)")
    a = s.find('        tool("rotateCw", "旋转")')
    b = s.find("        col.addView(bar)", a)
    if a >= 0 and b > a:
        block = s[a:b]
        s = s[:a] + s[b:]
        anchor = s.find("        stage.post { center() }")
        if anchor >= 0:
            s = s[:anchor] + block + "\n" + s[anchor:]
    return s
edit("ui/CropEditor.kt", crop)

def registry(s):
    marker = '            put("quiz", JSONObject().put("q", c.quiz.q).put("a", c.quiz.a).put("hint", c.quiz.hint))'
    extra = marker + '\n            c.media?.let { put("media", JSONObject().put("kind", it.kind).put("fname", it.fname).put("rel", it.uri).put("size", it.size).put("dur", it.dur)) }'
    return s.replace(marker, extra)
edit("data/PackRegistry.kt", registry)

# Remove duplicate import lines without changing source line endings.
for p in src.rglob("*.kt"):
    lines = p.read_text(encoding="utf-8").splitlines()
    seen, out = set(), []
    for line in lines:
        if line.startswith("import "):
            if line in seen:
                continue
            seen.add(line)
        out.append(line)
    p.write_text("\n".join(out) + "\n", encoding="utf-8")
print("Repair pass complete.")
