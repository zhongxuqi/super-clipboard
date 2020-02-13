var lang = ''

var languageMap = {
  'en': {
    'catch_clipboard_content': 'Catch New Clipboard Content',
    'receive_clipboard_content': 'Receive New Clipboard Content',
    'copy_clipboard_content': 'Copy Clipboard Content'
  },
  'zh': {
    'catch_clipboard_content': '获取到新的剪切板内容',
    'receive_clipboard_content': '接收到新的剪切板内容',
    'copy_clipboard_content': '复制剪切板内容'
  }
}

export default {
  setLanguage: function (l) {
    if (l.search('zh') === 0) {
      lang = 'zh'
    } else {
      lang = 'en'
    }
  },
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
