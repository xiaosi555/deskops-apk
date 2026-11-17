package com.xiaosi555.deskops;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 4102, PICK_FOLDER = 4103, PICK_FILES = 4104, SAVE_FILE = 4105;
    private WebView webView;
    private FrameLayout root;
    private WebChromeClient.CustomViewCallback fullScreenCallback;
    private View fullScreenView;
    private ValueCallback<Uri[]> fileCallback;
    private WebViewAssetLoader assetLoader;
    private final Map<String, Uri> mediaUris = new ConcurrentHashMap<>();
    private final Map<String, Long> mediaSizes = new ConcurrentHashMap<>();
    private final AtomicInteger mediaSerial = new AtomicInteger();
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private String pendingRequest, pendingKind, saveName, saveMime;
    private byte[] saveBytes;
    private TextToSpeech tts;
    private boolean ttsReady;
    private final ArrayList<String> voiceCallbacks = new ArrayList<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setSupportMultipleWindows(false);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setDefaultTextEncodingName("UTF-8");
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidMedia");

        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())
                        && u.getPath() != null && u.getPath().startsWith("/media/")) {
                    return streamMedia(u.getLastPathSegment(), req.getRequestHeaders());
                }
                return assetLoader.shouldInterceptRequest(u);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri target = req.getUrl();
                if ("appassets.androidplatform.net".equals(target.getHost())) return false;
                if (req.isForMainFrame()) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, target)); }
                    catch (ActivityNotFoundException ignored) { Toast.makeText(MainActivity.this, "没有可打开此链接的应用", Toast.LENGTH_SHORT).show(); }
                    return true;
                }
                return false;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith("https://appassets.androidplatform.net/assets/index.html")) {
                    String probe = "(function(){return JSON.stringify({title:document.title,bridge:!!window.AndroidMedia,"
                            + "mediaPicker:!!(window.AndroidMedia&&typeof window.AndroidMedia.pickMediaFolder==='function'),"
                            + "tts:!!(window.AndroidMedia&&typeof window.AndroidMedia.speakTts==='function'),"
                            + "back:typeof window.__androidHandleBack==='function',shell:!!document.querySelector('.app'),"
                            + "theme:document.documentElement.getAttribute('data-theme')});})()";
                    view.evaluateJavascript(probe, value -> Log.i("DeskOpsSmoke", "APP_PAGE_READY:" + value));
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try { startActivityForResult(p.createIntent(), FILE_CHOOSER); return true; }
                catch (RuntimeException e) {
                    if (fileCallback != null) fileCallback.onReceiveValue(null);
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "未找到可用的文件选择器", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                if (fullScreenView != null) { cb.onCustomViewHidden(); return; }
                fullScreenView = v; fullScreenCallback = cb;
                webView.setVisibility(View.GONE);
                root.addView(v, new FrameLayout.LayoutParams(-1, -1));
                getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
            }
            @Override public void onHideCustomView() { hideFullScreen(); }
        });
        initTts();
        if (state == null) webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        else webView.restoreState(state);
    }

    private void hideFullScreen() {
        if (fullScreenView == null) return;
        root.removeView(fullScreenView); fullScreenView = null;
        webView.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if (fullScreenCallback != null) { fullScreenCallback.onCustomViewHidden(); fullScreenCallback = null; }
    }

    public class AndroidBridge {
        @JavascriptInterface public void pickMediaFolder(String id, String kind) {
            runOnUiThread(() -> {
                pendingRequest = id; pendingKind = kind;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
                try { startActivityForResult(i, PICK_FOLDER); }
                catch (ActivityNotFoundException e) { finishRequest(id, new JSONObject()); }
            });
        }
        @JavascriptInterface public void pickMediaFiles(String id, String kind) {
            runOnUiThread(() -> {
                pendingRequest = id; pendingKind = kind;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
                i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(i, PICK_FILES); }
                catch (ActivityNotFoundException e) { finishRequest(id, new JSONObject()); }
            });
        }
        @JavascriptInterface public void registerMediaUri(String id, String uriText) {
            io.execute(() -> {
                try {
                    Uri u = Uri.parse(uriText);
                    try (InputStream probe = getContentResolver().openInputStream(u)) {
                        if (probe == null) throw new IllegalStateException("Media permission expired");
                    }
                    String key = register(u, querySize(u));
                    JSONObject o = new JSONObject(); o.put("id", key); o.put("url", mediaUrl(key));
                    finishRequest(id, o);
                } catch (Exception e) { finishRequest(id, new JSONObject()); }
            });
        }
        @JavascriptInterface public void copyText(String value) {
            runOnUiThread(() -> {
                try {
                    ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("复制内容", value == null ? "" : value));
                } catch (Exception e) { Toast.makeText(MainActivity.this, "复制失败", Toast.LENGTH_SHORT).show(); }
            });
        }
        @JavascriptInterface public void saveTextFile(String name, String mime, String base64) {
            runOnUiThread(() -> {
                try {
                    saveBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                    saveName = safeName(name);
                    saveMime = mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE); i.setType(saveMime);
                    i.putExtra(Intent.EXTRA_TITLE, saveName);
                    startActivityForResult(i, SAVE_FILE);
                } catch (Exception e) {
                    saveBytes = null;
                    Toast.makeText(MainActivity.this, "无法创建保存文件，请重试", Toast.LENGTH_LONG).show();
                }
            });
        }
        @JavascriptInterface public void getTtsVoices(String id) {
            runOnUiThread(() -> {
                if (tts == null) { finishRequest(id, new JSONObject()); return; }
                if (!ttsReady) { voiceCallbacks.add(id); return; }
                finishRequest(id, voicePayload());
            });
        }
        @JavascriptInterface public void speakTts(String id, String text, float rate, String voice) {
            runOnUiThread(() -> speakNative(id, text, rate, voice));
        }
        @JavascriptInterface public void stopTts() {
            runOnUiThread(() -> { if (tts != null) tts.stop(); });
        }
    }

    private void initTts() {
        try {
            tts = new TextToSpeech(this, status -> {
                ttsReady = status == TextToSpeech.SUCCESS;
                if (ttsReady) {
                    tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String id) { notifyTts(id, "start"); }
                        @Override public void onDone(String id) { notifyTts(id, "end"); }
                        @Override public void onError(String id) { notifyTts(id, "error"); }
                        @Override public void onError(String id, int code) { notifyTts(id, "error"); }
                    });
                }
                ArrayList<String> pending = new ArrayList<>(voiceCallbacks);
                voiceCallbacks.clear();
                for (String id : pending) finishRequest(id, voicePayload());
            });
        } catch (Exception e) { ttsReady = false; }
    }
    private JSONObject voicePayload() {
        JSONObject o = new JSONObject(); JSONArray a = new JSONArray();
        try {
            o.put("ready", ttsReady);
            if (ttsReady && tts.getVoices() != null) for (android.speech.tts.Voice v : tts.getVoices()) {
                JSONObject x = new JSONObject();
                x.put("id", v.getName()); x.put("name", v.getName());
                x.put("lang", v.getLocale() == null ? "" : v.getLocale().toLanguageTag()); a.put(x);
            }
            o.put("voices", a);
        } catch (Exception ignored) { }
        return o;
    }
    private void speakNative(String id, String text, float rate, String voiceName) {
        if (!ttsReady || tts == null) { notifyTts(id, "error"); return; }
        try {
            tts.stop(); tts.setSpeechRate(Math.max(.5f, Math.min(2f, rate)));
            boolean selected = false;
            if (voiceName != null && !voiceName.isEmpty() && tts.getVoices() != null) {
                for (android.speech.tts.Voice v : tts.getVoices()) if (voiceName.equals(v.getName())) {
                    tts.setVoice(v); selected = true; break;
                }
            }
            if (!selected) tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
        } catch (Exception e) { notifyTts(id, "error"); }
    }
    private void notifyTts(String id, String event) {
        runOnUiThread(() -> {
            if (webView == null) return;
            webView.evaluateJavascript("window.__nativeTtsEvent&&window.__nativeTtsEvent("
                    + JSONObject.quote(id) + "," + JSONObject.quote(event) + ");", null);
        });
    }
    private void finishRequest(String id, JSONObject data) {
        if (webView == null || id == null) return;
        String json = data == null ? "null" : data.toString();
        webView.post(() -> webView.evaluateJavascript("window.__nativeMediaResult&&window.__nativeMediaResult("
                + JSONObject.quote(id) + "," + JSONObject.quote(json) + ");", null));
    }

    private String register(Uri uri, long size) {
        String id = "m" + mediaSerial.incrementAndGet(); mediaUris.put(id, uri);
        if (size > 0) mediaSizes.put(id, size); return id;
    }
    private String mediaUrl(String id) { return "https://appassets.androidplatform.net/media/" + id; }
    private JSONObject docInfo(Uri uri) {
        JSONObject o = new JSONObject();
        try (Cursor c = getContentResolver().query(uri, new String[]{
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_MIME_TYPE
        }, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                o.put("name", c.isNull(0) ? "" : c.getString(0));
                o.put("size", c.isNull(1) ? 0 : c.getLong(1));
                o.put("mtime", c.isNull(2) ? 0 : c.getLong(2));
                o.put("mime", c.isNull(3) ? "" : c.getString(3));
            }
        } catch (Exception ignored) { }
        return o;
    }
    private String displayName(Uri u) {
        String n = docInfo(u).optString("name", ""); return n.isEmpty() ? "media" : n;
    }
    private long querySize(Uri u) {
        long n = docInfo(u).optLong("size", -1);
        if (n > 0) return n;
        try (AssetFileDescriptor a = getContentResolver().openAssetFileDescriptor(u, "r")) {
            if (a != null && a.getLength() > 0) return a.getLength();
        } catch (Exception ignored) { }
        return -1;
    }
    private boolean isMedia(String name, String mime, String kind) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if ("video".equals(kind) && m.startsWith("video/")) return true;
        if ("audio".equals(kind) && m.startsWith("audio/")) return true;
        int dot = n.lastIndexOf('.'); String ext = dot < 0 ? "" : n.substring(dot + 1);
        Set<String> v = new HashSet<>(), a = new HashSet<>();
        java.util.Collections.addAll(v, "mp4","m4v","mov","webm","mkv","avi","ogv","3gp","flv","wmv","mpg","mpeg");
        java.util.Collections.addAll(a, "mp3","m4a","aac","wav","ogg","oga","flac","opus","amr","wma","m4b","aiff","mka");
        return "video".equals(kind) ? v.contains(ext) : a.contains(ext);
    }
    private JSONObject item(Uri uri, String name, String rel, long size, long mtime, String mime) throws Exception {
        String id = register(uri, size); JSONObject o = new JSONObject();
        o.put("id", id); o.put("uri", uri.toString()); o.put("url", mediaUrl(id));
        o.put("name", name); o.put("rel", rel); o.put("size", Math.max(0, size));
        o.put("mtime", Math.max(0, mtime)); o.put("mime", mime == null ? "" : mime); return o;
    }
    private JSONObject folderResult(Uri tree, String kind) throws Exception {
        String rootId = DocumentsContract.getTreeDocumentId(tree);
        JSONObject result = new JSONObject(); JSONArray items = new JSONArray();
        collect(tree, rootId, "", 0, kind, items);
        result.put("name", displayName(DocumentsContract.buildDocumentUriUsingTree(tree, rootId)));
        result.put("items", items); return result;
    }
    private void collect(Uri tree, String parentId, String prefix, int depth, String kind, JSONArray items) {
        if (depth > 4) return;
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] cols = {DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try (Cursor c = getContentResolver().query(children, cols, null, null, null)) {
            if (c == null) return;
            while (c.moveToNext()) {
                String id = c.getString(0), name = c.getString(1), mime = c.getString(2);
                long size = c.isNull(3) ? 0 : c.getLong(3), mtime = c.isNull(4) ? 0 : c.getLong(4);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    if (name != null && !name.startsWith(".")) collect(tree, id, prefix + name + "/", depth + 1, kind, items);
                } else if (isMedia(name, mime, kind)) {
                    try { items.put(item(DocumentsContract.buildDocumentUriUsingTree(tree, id),
                            name, prefix + name, size, mtime, mime)); } catch (Exception ignored) { }
                }
            }
        } catch (Exception ignored) { }
    }
    private JSONObject pickedFiles(Intent data, String kind) throws Exception {
        JSONObject result = new JSONObject(); JSONArray items = new JSONArray();
        ArrayList<Uri> uris = new ArrayList<>();
        if (data.getClipData() != null) {
            for (int i=0; i<data.getClipData().getItemCount(); i++) {
                Uri u=data.getClipData().getItemAt(i).getUri(); if(u!=null)uris.add(u);
            }
        } else if (data.getData()!=null) uris.add(data.getData());
        for (Uri u : uris) {
            try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(Exception ignored) { }
            JSONObject meta=docInfo(u); String name=meta.optString("name","");
            if(name.isEmpty())name=displayName(u);
            String mime=meta.optString("mime",""); if(mime.isEmpty())mime=getContentResolver().getType(u);
            if(isMedia(name,mime,kind)) items.put(item(u,name,name,meta.optLong("size",0),meta.optLong("mtime",0),mime));
        }
        result.put("name",""); result.put("items",items); return result;
    }
    private void finishPicker(int code, Intent data, boolean folder) {
        String id=pendingRequest, kind=pendingKind; pendingRequest=null; pendingKind=null;
        if(id==null)return;
        if(code!=RESULT_OK||data==null){JSONObject o=new JSONObject();try{o.put("cancelled",true);}catch(Exception ignored){}finishRequest(id,o);return;}
        final Intent picked = data;
        io.execute(() -> {
            try {
                JSONObject result;
                if(folder){
                    Uri tree=picked.getData(); if(tree==null)throw new IllegalStateException("No folder");
                    int flags=picked.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    try{getContentResolver().takePersistableUriPermission(tree,flags|Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}
                    result=folderResult(tree,kind);
                } else result=pickedFiles(picked,kind);
                finishRequest(id,result);
            } catch(Exception e) {
                JSONObject o=new JSONObject();try{o.put("items",new JSONArray());}catch(Exception ignored){}
                finishRequest(id,o);
                runOnUiThread(() -> Toast.makeText(MainActivity.this,"读取媒体文件失败，请换一个文件夹或文件",Toast.LENGTH_LONG).show());
            }
        });
    }
    private WebResourceResponse streamMedia(String id, Map<String,String> reqHeaders) {
        Uri uri=mediaUris.get(id);
        if(uri==null)return response("text/plain",404,"Not Found",new HashMap<>(),new ByteArrayInputStream(new byte[0]));
        try {
            long size=mediaSizes.containsKey(id)?mediaSizes.get(id):querySize(uri);
            String mime=getContentResolver().getType(uri);
            if(mime==null||mime.isEmpty())mime=mimeFromName(displayName(uri));
            String range=null;
            if(reqHeaders!=null)for(Map.Entry<String,String> e:reqHeaders.entrySet())if("range".equalsIgnoreCase(e.getKey())){range=e.getValue();break;}
            InputStream in=getContentResolver().openInputStream(uri); if(in==null)throw new IllegalStateException("open failed");
            Map<String,String> headers=new HashMap<>();headers.put("Accept-Ranges","bytes");headers.put("Cache-Control","no-store");
            if(range!=null&&size>0){
                Matcher m=Pattern.compile("bytes=(\\d*)-(\\d*)").matcher(range.trim());
                if(m.matches()){
                    long start, end;
                    if(m.group(1).isEmpty()){
                        long suffix=Long.parseLong(m.group(2));start=Math.max(0,size-suffix);end=size-1;
                    }else{
                        start=Long.parseLong(m.group(1));end=m.group(2).isEmpty()?size-1:Long.parseLong(m.group(2));
                    }
                    if(start>=size||end<start){in.close();headers.put("Content-Range","bytes */"+size);return response(mime,416,"Range Not Satisfiable",headers,new ByteArrayInputStream(new byte[0]));}
                    end=Math.min(end,size-1);long skip=start;
                    while(skip>0){long n=in.skip(skip);if(n<=0){if(in.read()==-1)break;n=1;}skip-=n;}
                    long len=end-start+1;headers.put("Content-Range","bytes "+start+"-"+end+"/"+size);headers.put("Content-Length",String.valueOf(len));
                    return response(mime,206,"Partial Content",headers,new LimitedStream(in,len));
                }
            }
            if(size>0)headers.put("Content-Length",String.valueOf(size));
            return response(mime,200,"OK",headers,in);
        } catch(Exception e) { return response("text/plain",404,"Not Found",new HashMap<>(),new ByteArrayInputStream(new byte[0])); }
    }
    private WebResourceResponse response(String mime,int code,String reason,Map<String,String> headers,InputStream in) {
        return new WebResourceResponse(mime,null,code,reason,headers,in);
    }
    private static class LimitedStream extends InputStream {
        private final InputStream in; private long left;
        LimitedStream(InputStream source,long count){in=source;left=count;}
        @Override public int read() throws java.io.IOException {if(left<=0)return -1;int v=in.read();if(v>=0)left--;return v;}
        @Override public int read(byte[] b,int off,int len)throws java.io.IOException{if(left<=0)return -1;int n=in.read(b,off,(int)Math.min(len,left));if(n>0)left-=n;return n;}
        @Override public void close()throws java.io.IOException{in.close();}
    }
    private String mimeFromName(String name) {
        String n=name.toLowerCase(Locale.ROOT);
        if(n.endsWith(".mp4")||n.endsWith(".m4v"))return "video/mp4";
        if(n.endsWith(".m4a"))return "audio/mp4"; if(n.endsWith(".mov"))return "video/quicktime";
        if(n.endsWith(".webm"))return "video/webm"; if(n.endsWith(".mp3"))return "audio/mpeg";
        if(n.endsWith(".wav"))return "audio/wav"; if(n.endsWith(".ogg")||n.endsWith(".oga"))return "audio/ogg";
        if(n.endsWith(".aac"))return "audio/aac"; if(n.endsWith(".flac"))return "audio/flac";
        if(n.endsWith(".3gp"))return "video/3gpp"; return "application/octet-stream";
    }
    private String safeName(String n) {
        if(n==null||n.isEmpty())return "export.txt";
        return n.replace("/", "_").replace("\\", "_").replace(":", "_").replace("*", "_")
                .replace("?", "_").replace("\"", "_").replace("<", "_").replace(">", "_").replace("|", "_");
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==FILE_CHOOSER&&fileCallback!=null){
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));fileCallback=null;
        } else if(requestCode==PICK_FOLDER)finishPicker(resultCode,data,true);
        else if(requestCode==PICK_FILES)finishPicker(resultCode,data,false);
        else if(requestCode==SAVE_FILE){
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null&&saveBytes!=null){
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")){
                    if(out!=null){out.write(saveBytes);out.flush();}Toast.makeText(this,"文件已保存",Toast.LENGTH_SHORT).show();
                }catch(Exception e){Toast.makeText(this,"保存失败，请重试",Toast.LENGTH_LONG).show();}
            }
            saveBytes=null;
        }
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(webView!=null)webView.saveState(out);}
    @Override public void onBackPressed(){
        if(fullScreenView!=null){hideFullScreen();return;}
        if(webView!=null){
            webView.evaluateJavascript("(window.__androidHandleBack&&window.__androidHandleBack())===true", value -> {
                if ("true".equals(value)) return;
                if (webView != null && webView.canGoBack()) webView.goBack();
                else finish();
            });
            return;
        }
        super.onBackPressed();
    }
    @Override protected void onDestroy(){
        if(fileCallback!=null)fileCallback.onReceiveValue(null);
        if(tts!=null){tts.stop();tts.shutdown();}
        io.shutdownNow();
        if(webView!=null){webView.stopLoading();webView.removeJavascriptInterface("AndroidMedia");webView.destroy();}
        super.onDestroy();
    }
}