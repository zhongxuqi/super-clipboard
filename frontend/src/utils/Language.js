function getUrlVars() {
  var vars = {};
  window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
    vars[key] = value;
  });
  return vars;
}

function getUrlParam(parameter, defaultvalue){
  var urlparameter = defaultvalue;
  if(window.location.href.indexOf(parameter) > -1){
    urlparameter = getUrlVars()[parameter];
  }
  return urlparameter;
}
var lang = getUrlParam("lang", '')

if (lang == "") {
  var localLang = navigator.language || navigator.userLanguage;
  if (typeof localLang == "string" && localLang.indexOf("zh") >= 0) {
    lang = "zh"
  }
}

var items = window.location.pathname.split('/')
var lastPath = items[items.length - 1]
if (lastPath === "en" || lastPath === "zh") {
  lang = lastPath
}

if (lang != "en" && lang != "zh") {
  lang = ""
}

var languageMap = {
  "en": {
    'html_title': 'SuperClipboard',
    'html_keywords': 'clipboard,clipboard manager,clipboard sync',
    'super_clipboard': 'Super Clipboard',
    'super_clipboard_desc': 'Clipboard manager and sync tool',
    'float_window_design': 'Float Window Design',
    'feature_title': 'Why choose SuperClipboard',
    'keyword_search': 'Keyword Search',
    'window_min': 'Window Minimize',
    'app_download': 'APP Downloader',
    'mobile': 'Mobile',
    'desktop': 'Desktop',
    'clipboard_sync': 'Clipboard Sync',
    'coming_soon': 'Coming soon',
  },
  "zh": {
    'html_title': '超级剪切板',
    'html_keywords': '剪切板,剪切板管理,剪切板同步',
    'super_clipboard': '超级剪切板',
    'super_clipboard_desc': '剪切板管理、同步工具',
    'float_window_design': '悬浮窗设计',
    'feature_title': '为什么选择剪切板',
    'keyword_search': '关键字搜索',
    'window_min': '窗口最小化',
    'app_download': '应用下载',
    'mobile': '移动端',
    'desktop': '桌面端',
    'clipboard_sync': '剪切板同步',
    'coming_soon': '敬情期待',
  },
}

// init html title and meta
document.getElementsByTagName('title')[0].innerText = languageMap[lang]['html_title']
let meta = document.createElement('meta')
meta.name = 'keywords'
meta.content = languageMap[lang]['html_keywords']
document.getElementsByTagName('head')[0].appendChild(meta)

export default {
  getLanguage: function() {
    return lang
  },
  getLanguageDesc: function() {
    return {
      '': 'English',
      'en': 'English',
      'zh': '中文',
    }[lang]
  },
  getLanguageText: function(key) {
    var currLang = lang
    if (currLang == "") {
      currLang = "en"
    }
    return languageMap[currLang][key]
  },
}