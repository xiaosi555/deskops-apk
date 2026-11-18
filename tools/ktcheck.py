import os, sys


def templates_only(body):
    """只保留 ${...} 内部内容（含配对花括号），丢掉字面文本。"""
    res = []
    i = 0
    n = len(body)
    while i < n:
        if body.startswith('${', i):
            depth = 0
            j = i + 1
            start = j
            while j < n:
                ch = body[j]
                if ch == '{':
                    depth += 1
                elif ch == '}':
                    depth -= 1
                    if depth == 0:
                        break
                j += 1
            inner = body[start + 1:j]
            res.append('{')
            res.append(templates_only(inner))
            res.append('}')
            i = j + 1
            continue
        i += 1
    return ''.join(res)


def clean(src):
    out = []
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if c == '/' and nxt == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and nxt == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            out.append('\n')
            continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            out.append(' ')
            continue
        if c == '"':
            j = i + 1
            body = []
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == '"':
                    break
                body.append(src[j])
                j += 1
            out.append(templates_only(''.join(body)))
            i = j + 1
            continue
        if c == "'":
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == "'":
                    break
                j += 1
            i = j + 1
            out.append('x')
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def check(path):
    raw = open(path, encoding='utf-8').read()
    s = clean(raw)
    pairs = {'{': '}', '(': ')', '[': ']'}
    closers = {v: k for k, v in pairs.items()}
    stack = []
    lines = raw.split("\n")
    slines = s.split("\n")
    for ln, line in enumerate(slines):
        for c in line:
            if c in pairs:
                stack.append((c, ln + 1))
            elif c in closers:
                if not stack:
                    return "EXTRA %s at line %d: %s" % (c, ln + 1, lines[ln].strip()[:100])
                if pairs[stack[-1][0]] != c:
                    return "MISMATCH %s at line %d (want %s from line %d): %s" % (
                        c, ln + 1, pairs[stack[-1][0]], stack[-1][1], lines[ln].strip()[:100])
                stack.pop()
    if stack:
        c, ln = stack[-1]
        return "UNCLOSED %s opened line %d: %s" % (c, ln, lines[ln - 1].strip()[:100])
    return None


root = sys.argv[1] if len(sys.argv) > 1 else '.'
files = []
for d, dirs, fs in os.walk(root):
    if 'build' in d:
        continue
    for f in fs:
        if f.endswith('.kt'):
            files.append(os.path.join(d, f))
bad = 0
for p in sorted(files):
    r = check(p)
    if r:
        print(p, '->', r)
        bad += 1
print("checked", len(files), "files, issues:", bad)
