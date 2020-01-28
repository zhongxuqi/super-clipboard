var lang = ''

if (lang === '') {
  var localLang = navigator.language || navigator.userLanguage
  if (typeof localLang === 'string' && localLang.indexOf('zh') >= 0) {
    lang = 'zh'
  }
}

var items = window.location.pathname.split('/')
var lastPath = items[items.length - 1]
if (lastPath === 'en' || lastPath === 'zh') {
  lang = lastPath
}

if (lang !== 'en' && lang !== 'zh') {
  lang = ''
}

var languageMap = {
  'en': {
    'clipboard': 'ClipBoard'
  },
  'zh': {
    'clipboard': '剪切板'
  }
}

export default {
  getLanguage: function () {
    return lang
  },
  getLanguageDesc: function () {
    return {
      '': 'English',
      'en': 'English',
      'zh': '中文'
    }[lang]
  },
  getLanguageText: function (key) {
    var currLang = lang
    if (currLang === '') {
      currLang = 'en'
    }
    return languageMap[currLang][key]
  }
}
