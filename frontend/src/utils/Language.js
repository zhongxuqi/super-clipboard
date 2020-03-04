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
    'super_clipboard': 'Super Clipboard',
    'super_clipboard_desc': 'A powful and amazing clipboard manager',
    'float_window_design': 'Float Window Design',
    'feature_title': 'Why choose SuperClipboard',
    'keyword_search': 'Keyword Search',
    'window_min': 'Window Minize',
    'app_download': 'APP Downloader',
    'mobile': 'Mobile',
    'desktop': 'Desktop',
  },
  "zh": {
    'super_clipboard': '超级剪切板',
    'super_clipboard_desc': '一款极致、强大的剪切板管理工具',
    'float_window_design': '悬浮窗设计',
    'feature_title': '为什么选择剪切板',
    'keyword_search': '关键字搜索',
    'window_min': '窗口最小化',
    'app_download': '应用下载',
    'mobile': '移动端',
    'desktop': '桌面端',
  },
}

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