(function () {
  'use strict';
  if (window.__androidCompatInstalled || !window.AndroidMedia) return;
  window.__androidCompatInstalled = true;

  var pending = Object.create(null);
  var requestSeq = 0;
  var utterances = Object.create(null);
  var nativeVoices = [];
  var nativeTtsReady = false;
  var nativeVoicesRequested = false;
  var originalSpeech = window.speechSynthesis || null;
  var OriginalUtterance = window.SpeechSynthesisUtterance || null;

  function nativeRequest(method, args) {
    return new Promise(function (resolve, reject) {
      var id = 'android_' + (++requestSeq) + '_' + Date.now();
      var fn = window.AndroidMedia && window.AndroidMedia[method];
      if (typeof fn !== 'function') {
        reject(new Error('Android bridge method unavailable: ' + method));
        return;
      }
      pending[id] = { resolve: resolve, reject: reject };
      try { fn.apply(window.AndroidMedia, [id].concat(args || [])); }
      catch (e) { delete pending[id]; reject(e); }
    });
  }

  window.__nativeMediaResult = function (id, raw) {
    var item = pending[id];
    if (!item) return;
    delete pending[id];
    try { item.resolve(typeof raw === 'string' ? JSON.parse(raw) : raw); }
    catch (e) { item.reject(e); }
  };

  function mediaFile(row) {
    return {
      name: row.name || 'media',
      size: Number(row.size) || 0,
      lastModified: Number(row.mtime) || 0,
      type: row.mime || '',
      webkitRelativePath: row.rel || row.name || '',
      __androidFile: true,
      __androidUri: row.uri || '',
      __androidUrl: row.url || '',
      __androidId: row.id || ''
    };
  }

  function mediaItems(data, folder) {
    return (data && Array.isArray(data.items) ? data.items : []).map(function (row) {
      var file = mediaFile(row);
      var rel = row.rel || row.name || file.name;
      var parts = rel.split('/');
      var mod = folder && parts.length > 1 ? parts[0] : '';
      if (typeof window.toItem === 'function') return window.toItem(file, mod, rel);
      return { file: file, fname: file.name, name: file.name.replace(/\\.[^.]+$/, ''),
        rel: rel, size: file.size, mtime: file.lastModified, mod: mod };
    });
  }

  // Only the picker implementation changes. The original import UI and course logic stay intact.
  if (typeof window.pickFolder === 'function') {
    window.pickFolder = function (kind) {
      return nativeRequest('pickMediaFolder', [kind]).then(function (data) {
        if (!data || data.cancelled) return null;
        return { items: mediaItems(data, true), name: data.name || '', dirHandle: null };
      }).catch(function () { return null; });
    };
    window.pickFolder.__androidNative = true;
  }
  if (typeof window.pickFiles === 'function') {
    window.pickFiles = function (kind) {
      return nativeRequest('pickMediaFiles', [kind]).then(function (data) {
        if (!data || data.cancelled) return null;
        return { items: mediaItems(data, false), name: '' };
      }).catch(function () { return null; });
    };
    window.pickFiles.__androidNative = true;
  }

  // Stream URI-backed files from Android; do not load large videos into JS memory.
  var createObjectURL = URL.createObjectURL.bind(URL);
  var revokeObjectURL = URL.revokeObjectURL.bind(URL);
  URL.createObjectURL = function (object) {
    if (object && object.__androidFile && object.__androidUrl) return object.__androidUrl;
    return createObjectURL(object);
  };
  URL.revokeObjectURL = function (url) {
    if (typeof url === 'string' && url.indexOf('https://appassets.androidplatform.net/media/') === 0) return;
    return revokeObjectURL(url);
  };

  // Re-register persistable SAF URIs after Android process restart.
  if (typeof window.idbGet === 'function' && !window.idbGet.__androidWrapped) {
    var originalIdbGet = window.idbGet;
    var wrappedIdbGet = async function (key) {
      var value = await originalIdbGet.apply(this, arguments);
      if (String(key || '').indexOf('blob:') === 0 && value && value.__androidFile && value.__androidUri) {
        try {
          var fresh = await nativeRequest('registerMediaUri', [value.__androidUri]);
          if (fresh && fresh.url) value = Object.assign({}, value, { __androidUrl: fresh.url, __androidId: fresh.id || '' });
        } catch (e) { }
      }
      return value;
    };
    wrappedIdbGet.__androidWrapped = true;
    window.idbGet = wrappedIdbGet;
  }

  function SpeechUtterance(text) {
    this.text = String(text == null ? '' : text);
    this.lang = 'zh-CN'; this.rate = 1; this.pitch = 1; this.volume = 1; this.voice = null;
    this.onstart = null; this.onend = null; this.onerror = null; this.onpause = null; this.onresume = null;
  }

  function dispatchVoiceChange() {
    try {
      if (typeof speech.onvoiceschanged === 'function') speech.onvoiceschanged();
      if (typeof Event === 'function') speech.dispatchEvent(new Event('voiceschanged'));
    } catch (e) { }
  }

  function refreshVoices() {
    if (nativeVoicesRequested) return;
    nativeVoicesRequested = true;
    nativeRequest('getTtsVoices', []).then(function (data) {
      nativeTtsReady = !!(data && data.ready);
      nativeVoices = (data && Array.isArray(data.voices) ? data.voices : []).map(function (v) {
        return { voiceURI: v.id || v.name || '', name: v.name || v.id || '系统语音',
          lang: v.lang || 'zh-CN', localService: true, default: false };
      });
      dispatchVoiceChange();
    }).catch(function () { nativeTtsReady = false; });
  }

  var speech = {
    onvoiceschanged: null,
    getVoices: function () {
      refreshVoices();
      if (nativeVoices.length) return nativeVoices.slice();
      try { return originalSpeech && originalSpeech.getVoices ? originalSpeech.getVoices() : []; }
      catch (e) { return []; }
    },
    speak: function (utterance) {
      if (!utterance) return;
      refreshVoices();
      var id = 'tts_' + (++requestSeq) + '_' + Date.now();
      if (nativeTtsReady) {
        utterances[id] = utterance;
        try {
          var voice = utterance.voice ? (utterance.voice.voiceURI || utterance.voice.name || '') : '';
          window.AndroidMedia.speakTts(id, String(utterance.text || ''), Number(utterance.rate) || 1, voice);
          return;
        } catch (e) { delete utterances[id]; }
      }
      if (originalSpeech && OriginalUtterance) {
        try {
          var fallback = new OriginalUtterance(utterance.text || '');
          fallback.lang = utterance.lang || 'zh-CN'; fallback.rate = utterance.rate || 1; fallback.pitch = utterance.pitch || 1;
          fallback.onstart = utterance.onstart; fallback.onend = utterance.onend; fallback.onerror = utterance.onerror;
          originalSpeech.speak(fallback); return;
        } catch (e) { }
      }
      if (typeof utterance.onerror === 'function') utterance.onerror({ error: 'not-available' });
    },
    cancel: function () {
      utterances = Object.create(null);
      try { window.AndroidMedia.stopTts(); } catch (e) { }
      try { if (originalSpeech && originalSpeech.cancel) originalSpeech.cancel(); } catch (e) { }
    },
    pause: function () {
      try { window.AndroidMedia.stopTts(); } catch (e) { }
      try { if (originalSpeech && originalSpeech.pause) originalSpeech.pause(); } catch (e) { }
    },
    resume: function () { try { if (originalSpeech && originalSpeech.resume) originalSpeech.resume(); } catch (e) { } },
    addEventListener: function (name, fn) {
      if (name === 'voiceschanged' && typeof fn === 'function') {
        var old = speech.onvoiceschanged;
        speech.onvoiceschanged = function (e) { if (old) old(e); fn(e); };
      }
    },
    dispatchEvent: function () { return true; }
  };
  Object.defineProperties(speech, {
    speaking: { get: function () { return Object.keys(utterances).length > 0; } },
    pending: { get: function () { return false; } },
    paused: { get: function () { return false; } }
  });

  window.SpeechSynthesisUtterance = SpeechUtterance;
  try { Object.defineProperty(window, 'speechSynthesis', { configurable: true, enumerable: true, value: speech }); }
  catch (e) { window.speechSynthesis = speech; }

  window.__nativeTtsEvent = function (id, eventName) {
    var utterance = utterances[id];
    if (!utterance) return;
    if (eventName === 'start') {
      if (typeof utterance.onstart === 'function') utterance.onstart({ type: 'start', utterance: utterance });
    } else if (eventName === 'end') {
      delete utterances[id];
      if (typeof utterance.onend === 'function') utterance.onend({ type: 'end', utterance: utterance });
    } else {
      delete utterances[id];
      if (typeof utterance.onerror === 'function') utterance.onerror({ type: 'error', error: 'interrupted', utterance: utterance });
    }
  };

  try {
    Object.defineProperty(window.navigator, 'clipboard', {
      configurable: true,
      value: { writeText: function (value) {
        try { window.AndroidMedia.copyText(String(value == null ? '' : value)); return Promise.resolve(); }
        catch (e) { return Promise.reject(e); }
      } }
    });
  } catch (e) { }

  document.addEventListener('click', function (event) {
    var anchor = event.target && event.target.closest ? event.target.closest('a[download]') : null;
    if (!anchor || !anchor.href || anchor.href.indexOf('blob:') !== 0) return;
    event.preventDefault(); event.stopImmediatePropagation();
    fetch(anchor.href).then(function (response) { return response.blob(); }).then(function (blob) {
      return new Promise(function (resolve, reject) {
        var reader = new FileReader();
        reader.onload = function () { resolve(String(reader.result || '').split(',').pop()); };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      }).then(function (base64) {
        var mime = String(blob.type || 'application/octet-stream').split(';')[0];
        window.AndroidMedia.saveTextFile(anchor.download || 'export.txt', mime, base64);
      });
    }).catch(function () {
      try { window.AndroidMedia.copyText('导出失败，请重试或使用复制全文'); } catch (e) { }
    });
  }, true);

  window.__androidHandleBack = function () {
    try {
      if (document.querySelector('.sheetmask')) { if (typeof closeSheet === 'function') closeSheet(); return true; }
      var exportPanel = document.getElementById('xmod');
      if (exportPanel && exportPanel.classList.contains('on')) { if (typeof closeExport === 'function') closeExport(); return true; }
      if (typeof view !== 'undefined' && (view !== 'mine' || (typeof navStack !== 'undefined' && navStack.length > 0)
          || (typeof selectedMarkCourse !== 'undefined' && selectedMarkCourse)
          || (typeof ioDetail !== 'undefined' && ioDetail))) {
        if (typeof navBack === 'function') navBack();
        return true;
      }
    } catch (e) { }
    return false;
  };

  refreshVoices();
})();
