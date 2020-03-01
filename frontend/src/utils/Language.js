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
  },
  "zh": {
    'super_clipboard': '超级剪切板',
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