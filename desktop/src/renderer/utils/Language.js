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
    'clipboard': 'ClipBoard',
    'expand': 'Expand',
    'fold': 'Fold',
    'content_sync': 'Content Sync',
    'device_total': '%d device total',
    'keyword_input_hint': 'input keyword...',
    'delete': 'Delete',
    'sync': 'Sync',
    'on': 'On',
    'off': 'Off',
    'copied': 'Copied',
    'click_to_login': 'Click to login',
    'login_page': 'Login Page',
    'register_page': 'Register Page',
    'account': 'Account',
    'password': 'Password',
    'password_repeat': 'Password Repeat',
    'captcha_code': 'Captcha Code',
    'login': 'Login',
    'go_register': 'Go Register',
    'register': 'Register',
    'go_login': 'Go Login',
    'required': 'Required',
    'password_repeat_wrong': 'Password Repeat Wrong'
  },
  'zh': {
    'clipboard': '剪切板',
    'expand': '展开',
    'fold': '折叠',
    'content_sync': '内容同步',
    'device_total': '一共%d个设备',
    'keyword_input_hint': '输入关键字...',
    'delete': '删除',
    'sync': '同步',
    'on': '开',
    'off': '关',
    'copied': '复制成功',
    'click_to_login': '点击登录',
    'login_page': '登录页面',
    'register_page': '注册页面',
    'account': '帐号',
    'password': '密码',
    'password_repeat': '密码（重复）',
    'captcha_code': '验证码',
    'login': '登录',
    'go_register': '去注册',
    'register': '注册',
    'go_login': '去登录',
    'required': '必填',
    'password_repeat_wrong': '密码重复错误'
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
