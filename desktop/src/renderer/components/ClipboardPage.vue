<template>
  <div class="scb-clipboard">
    <div class="scb-clipboard-topbar">
      <div class="scb-user-head">
        <b-button v-if="userID==''" variant="light" v-on:click="openLogin">{{textClickToLogin}}</b-button>
        <b-dropdown v-if="userID!=''" v-bind:text="userID" variant="success">
          <b-dropdown-item v-on:click="openChangePassword">{{textChangePassword}}</b-dropdown-item>
          <b-dropdown-item v-on:click="openLogin">{{textLoginAnotherAccount}}</b-dropdown-item>
        </b-dropdown>
      </div>
      <b-form class="scb-clipboard-keyword-form">
        <input class="scb-clipboard-keyword-form-input" v-model="keyword" v-bind:placeholder="textKeywordInputHint"/>
      </b-form>
      <div id="scb-topbar-sync-switch" class="scb-topbar-sync-switch">
        <b-form-checkbox v-model="syncState" name="check-button" switch>
          {{syncState?syncStateDesc:textContentSync}}
        </b-form-checkbox>
      </div>
      <b-popover target="scb-topbar-sync-switch" triggers="hover" placement="top">
        <template v-slot:title>{{textUsageNotice}}</template>
        {{textUsageNoticeContent}}<br/><b-button variant="link" style="margin:0rem;padding:0rem;" v-on:click="openOfficialLink">https://www.superclipboard.online</b-button>
      </b-popover>
      <b-button variant="light" style="margin-right:0.5rem" size="sm" v-on:click="openFeedback"><i class="iconfont icon-feedback"></i></b-button>
    </div>
    <div class="scb-cliboard-list">
      <div class="scb-cliboard-list-item" v-for="item in msgListFilter" v-bind:key="item.id">
        <ClipboardMessage v-bind:type="item.type" v-bind:syncState="syncState" v-bind:content="item.content" v-on:ondelete="deleteMsg(item)" v-on:onsync="syncMsg(item)" v-on:oncopy="copyMsg(item)"></ClipboardMessage>
      </div>
    </div>

    <b-modal ref="login-modal" hide-footer v-bind:title="loginMode=='login'?textLoginPage:textRegisterPage" size="lg">
      <form class="scb-login-form" @submit.stop.prevent v-if="loginMode=='login'">
        <b-form-group v-bind:label="textAccount" :invalid-feedback="loginAccountErr" :state="loginAccountErr==''?undefined:false">
          <b-form-input v-model="loginAccount" :state="loginAccountErr==''?undefined:false" required trim></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPassword" :invalid-feedback="loginPasswordErr" :state="loginPasswordErr==''?undefined:false">
          <b-form-input type="password" v-model="loginPassword" required :state="loginPasswordErr==''?undefined:false" trim></b-form-input>
        </b-form-group>
        <b-button variant="success" block v-on:click="login">{{textLogin}}</b-button>
        <b-button variant="outline-success" block v-on:click="changeLoginMode('register')">{{textGoRegister}}</b-button>
      </form>
      <form class="scb-register-form" @submit.stop.prevent v-if="loginMode=='register'">
        <b-form-group v-bind:label="textAccount" :invalid-feedback="registerAccountErr" :state="registerAccountErr==''?undefined:false">
          <b-form-input v-model="registerAccount" required :state="registerAccountErr==''?undefined:false" trim></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPassword" :invalid-feedback="registerPasswordErr" :state="registerPasswordErr==''?undefined:false">
          <b-form-input type="password" v-model="registerPassword" required :state="registerPasswordErr==''?undefined:false"></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPasswordRepeat" :invalid-feedback="registerPasswordRepeatErr" :state="registerPasswordRepeatErr==''?undefined:false">
          <b-form-input type="password" v-model="registerPasswordRepeat" required :state="registerPasswordRepeatErr==''?undefined:false"></b-form-input>
        </b-form-group>
        <div style="display:flex;flex-direction:row;justify-content: center;align-items: center;">
          <b-form-group v-bind:label="textCaptchaCode" style="flex:1" :invalid-feedback="registerCaptchaErr" :state="registerCaptchaErr==''?undefined:false">
            <b-form-input v-model="registerCaptcha" required :state="registerCaptchaErr==''?undefined:false"></b-form-input>
          </b-form-group>
          <img v-if="captchaID!=''" v-bind:src="`${host}/openapi/captcha/${captchaID}.png`" style="margin:0rem 1rem;height:4rem" v-on:click="getCaptchaID"/>
          <img v-if="captchaID==''" src="~@/assets/broken_image.png" style="margin:0rem 1rem;height:4rem" v-on:click="getCaptchaID"/>
        </div>
        <b-button variant="success" block v-on:click="register">{{textRegister}}</b-button>
        <b-button variant="outline-success" block v-on:click="changeLoginMode('login')">{{textGoLogin}}</b-button>
      </form>
    </b-modal>

    <b-modal ref="change-password-modal" hide-footer v-bind:title="textChangePassword" size="lg">
      <form class="scb-change-password-form" @submit.stop.prevent>
        <b-form-group v-bind:label="textOriginPassword" :invalid-feedback="changePasswordOriginPasswordErr" :state="changePasswordOriginPasswordErr==''?undefined:false">
          <b-form-input type="password" v-model="changePasswordOriginPassword" required :state="changePasswordOriginPasswordErr==''?undefined:false"></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textNewPassword" :invalid-feedback="changePasswordNewPasswordErr" :state="changePasswordNewPasswordErr==''?undefined:false">
          <b-form-input type="password" v-model="changePasswordNewPassword" required :state="changePasswordNewPasswordErr==''?undefined:false"></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textNewPasswordRepeat" :invalid-feedback="changePasswordNewPasswordRepeatErr" :state="changePasswordNewPasswordRepeatErr==''?undefined:false">
          <b-form-input type="password" v-model="changePasswordNewPasswordRepeat" required :state="changePasswordNewPasswordRepeatErr==''?undefined:false"></b-form-input>
        </b-form-group>
        <div style="display:flex;flex-direction:row;justify-content: center;align-items: center;">
          <b-button variant="outline-success" block v-on:click="hideChangePassword" style="flex:1;margin: 0rem 1rem 0rem 0rem">{{textCancel}}</b-button>
          <b-button variant="success" block v-on:click="changePassword" style="flex:1;margin: 0rem;">{{textSubmit}}</b-button>
        </div>
      </form>
    </b-modal>

    <b-modal ref="feedback-modal" hide-footer v-bind:title="textFeedbackPage" size="lg">
      <form class="scb-change-password-form" @submit.stop.prevent>
        <b-form-textarea
          v-model="feedbackContent"
          v-bind:placeholder="textInputHint"
          rows="3"
          style="margin-bottom:1rem"></b-form-textarea>
        <div style="display:flex;flex-direction:row;justify-content: center;align-items: center;">
          <b-button variant="outline-success" block v-on:click="hideFeedback" style="flex:1;margin: 0rem 1rem 0rem 0rem">{{textCancel}}</b-button>
          <b-button variant="success" block v-on:click="submitFeedback" style="flex:1;margin: 0rem;">{{textSubmit}}</b-button>
        </div>
      </form>
    </b-modal>
  </div>
</template>

<script>
import ClipboardMessage from './ClipboardMessage'
import { ipcRenderer, shell } from 'electron'
import Language from '../utils/Language'
import Consts from '../../common/Consts'
import Hash from '../utils/Hash'

export default {
  name: 'clipboard-page',
  components: {
    ClipboardMessage
  },
  data: function () {
    return {
      textKeywordInputHint: Language.getLanguageText('keyword_input_hint'),
      textContentSync: Language.getLanguageText('sync_clipboard'),
      textOn: Language.getLanguageText('on'),
      textOff: Language.getLanguageText('off'),
      textClickToLogin: Language.getLanguageText('click_to_login'),
      textLoginPage: Language.getLanguageText('login_page'),
      textRegisterPage: Language.getLanguageText('register_page'),
      textAccount: Language.getLanguageText('account'),
      textPassword: Language.getLanguageText('password'),
      textLogin: Language.getLanguageText('login'),
      textGoRegister: Language.getLanguageText('go_register'),
      textRegister: Language.getLanguageText('register'),
      textGoLogin: Language.getLanguageText('go_login'),
      textPasswordRepeat: Language.getLanguageText('password_repeat'),
      textCaptchaCode: Language.getLanguageText('captcha_code'),
      textChangePassword: Language.getLanguageText('change_password'),
      textLoginAnotherAccount: Language.getLanguageText('login_another_account'),
      textOriginPassword: Language.getLanguageText('origin_password'),
      textNewPassword: Language.getLanguageText('new_password'),
      textNewPasswordRepeat: Language.getLanguageText('new_password_repeat'),
      textCancel: Language.getLanguageText('cancel'),
      textSubmit: Language.getLanguageText('submit'),
      textFeedbackPage: Language.getLanguageText('feedback_page'),
      textInputHint: Language.getLanguageText('input_hint'),
      textUsageNotice: Language.getLanguageText('usage_notice'),
      textUsageNoticeContent: Language.getLanguageText('usage_notice_content'),

      host: Consts.Host,
      msgList: [],
      keyword: '',
      syncState: false,
      deviceNum: 0,
      userID: '',

      // login modal
      loginMode: 'login',

      loginAccount: '',
      loginAccountErr: '',
      loginPassword: '',
      loginPasswordErr: '',

      registerAccount: '',
      registerAccountErr: '',
      registerPassword: '',
      registerPasswordErr: '',
      registerPasswordRepeat: '',
      registerPasswordRepeatErr: '',
      registerCaptcha: '',
      registerCaptchaErr: '',
      captchaID: '',

      // change password
      changePasswordOriginPassword: '',
      changePasswordOriginPasswordErr: '',
      changePasswordNewPassword: '',
      changePasswordNewPasswordErr: '',
      changePasswordNewPasswordRepeat: '',
      changePasswordNewPasswordRepeatErr: '',

      // feedback
      feedbackContent: ''
    }
  },
  methods: {
    onAddMessage: function (event, arg) {
      this.msgList = [arg, ...this.msgList]
    },
    onDeleteMessage: function (event, arg) {
      this.msgList = this.msgList.filter(function (item) {
        if (item.id === arg.id) return false
        return true
      })
    },
    syncMsg: function (msg) {
      ipcRenderer.send('clipboard-message-action-sync', msg)
    },
    deleteMsg: function (msg) {
      ipcRenderer.send('clipboard-message-action-delete', msg)
    },
    copyMsg: function (msg) {
      ipcRenderer.send('clipboard-message-action-copy', msg.content)
    },
    openLogin: function () {
      this.getCaptchaID()
      this.$refs['login-modal'].show()
    },
    changeLoginMode: function (mode) {
      this.loginMode = mode
    },
    getCaptchaID: function () {
      ipcRenderer.send('request-get_captcha_id', {})
    },
    login: function () {
      let hasErr = false
      this.loginAccount = this.loginAccount.trim()
      this.loginPassword = this.loginPassword.trim()
      if (this.loginAccount === '') {
        this.loginAccountErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.loginPassword === '') {
        this.loginPasswordErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (hasErr) {
        return
      }
      var currTime = parseInt(new Date().getTime() / 1000)
      var text = this.loginAccount + '-' + Hash.sha256(this.loginPassword) + '-' + currTime
      ipcRenderer.send('request-login', {
        app_id: Consts.AppID,
        account: this.loginAccount,
        time: currTime,
        token: Hash.sha256(text)
      })
    },
    register: function () {
      let hasErr = false
      this.registerAccount = this.registerAccount.trim()
      this.registerPassword = this.registerPassword.trim()
      this.registerPasswordRepeat = this.registerPasswordRepeat.trim()
      this.registerCaptcha = this.registerCaptcha.trim()
      if (this.registerAccount === '') {
        this.registerAccountErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.registerPassword === '') {
        this.registerPasswordErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.registerPasswordRepeat === '') {
        this.registerPasswordRepeatErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.registerPassword !== this.registerPasswordRepeat) {
        this.registerPasswordRepeatErr = Language.getLanguageText('password_repeat_wrong')
        hasErr = true
      }
      if (this.registerCaptcha === '') {
        this.registerCaptchaErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (hasErr) {
        return
      }
      ipcRenderer.send('request-register', {
        app_id: Consts.AppID,
        account: this.registerAccount,
        password: Hash.sha256(this.registerPassword),
        captcha_id: this.captchaID,
        captcha_solution: this.registerCaptcha
      })
    },
    openChangePassword: function () {
      this.$refs['change-password-modal'].show()
    },
    hideChangePassword: function () {
      this.$refs['change-password-modal'].hide()
    },
    changePassword: function () {
      let hasErr = false
      this.changePasswordOriginPassword = this.changePasswordOriginPassword.trim()
      this.changePasswordNewPassword = this.changePasswordNewPassword.trim()
      this.changePasswordNewPasswordRepeat = this.changePasswordNewPasswordRepeat.trim()
      if (this.changePasswordOriginPassword === '') {
        this.changePasswordOriginPasswordErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.changePasswordNewPassword === '') {
        this.changePasswordNewPasswordErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.changePasswordNewPasswordRepeat === '') {
        this.changePasswordNewPasswordRepeatErr = Language.getLanguageText('required')
        hasErr = true
      }
      if (this.changePasswordNewPassword !== this.changePasswordNewPasswordRepeat) {
        this.changePasswordNewPasswordRepeatErr = Language.getLanguageText('password_repeat_wrong')
        hasErr = true
      }
      if (hasErr) {
        return
      }
      var currTime = parseInt(new Date().getTime() / 1000)
      var text = this.userID + '-' + Hash.sha256(this.changePasswordOriginPassword) + '-' + currTime
      ipcRenderer.send('request-change_password', {
        app_id: Consts.AppID,
        account: this.userID,
        time: currTime,
        token: Hash.sha256(text),
        password: Hash.sha256(this.changePasswordNewPassword)
      })
    },
    openFeedback: function () {
      this.$refs['feedback-modal'].show()
    },
    hideFeedback: function () {
      this.$refs['feedback-modal'].hide()
    },
    submitFeedback: function () {
      this.feedbackContent = this.feedbackContent.trim()
      if (this.feedbackContent === '') {
        this.hideFeedback()
        return
      }
      ipcRenderer.send('request-feedback', {
        app_id: Consts.AppID,
        type: 1,
        context: '',
        message: this.feedbackContent
      })
      this.hideFeedback()
    },
    openOfficialLink: function () {
      shell.openExternal('https://www.superclipboard.online')
    }
  },
  computed: {
    msgListFilter: function () {
      return this.msgList.filter(function (item) {
        if (this.keyword === '') return true
        return item.content.search(this.keyword) >= 0
      }.bind(this))
    },
    syncStateDesc: function () {
      return Language.getLanguageText('device_total').replace('%d', `${this.deviceNum}`)
    }
  },
  watch: {
    syncState: function (newValue, oldValue) {
      setTimeout(function () {
        if (this.userID === '') {
          this.syncState = false
          this.openLogin()
        } else {
          ipcRenderer.send('clipboard-sync-state-toggle', {state: newValue})
        }
      }.bind(this), 100)
    },
    loginAccount: function (newValue, oldValue) {
      this.loginAccountErr = ''
    },
    loginPassword: function (newValue, oldValue) {
      this.loginPasswordErr = ''
    },
    registerAccount: function (newValue, oldValue) {
      this.registerAccountErr = ''
    },
    registerPassword: function (newValue, oldValue) {
      this.registerPasswordErr = ''
    },
    registerPasswordRepeat: function (newValue, oldValue) {
      this.registerPasswordRepeatErr = ''
    },
    registerCaptcha: function (newValue, oldValue) {
      this.registerCaptchaErr = ''
    },
    changePasswordOriginPassword: function (newValue, oldValue) {
      this.changePasswordOriginPasswordErr = ''
    },
    changePasswordNewPassword: function (newValue, oldValue) {
      this.changePasswordNewPasswordErr = ''
    },
    changePasswordNewPasswordRepeat: function (newValue, oldValue) {
      this.changePasswordNewPasswordRepeatErr = ''
    }
  },
  mounted: function () {
    ipcRenderer.send('clipboard-message-connect', '')
    ipcRenderer.on('clipboard-message-add', this.onAddMessage.bind(this))
    ipcRenderer.on('clipboard-message-delete', this.onDeleteMessage.bind(this))
    this.getCaptchaID()
    this.syncState = ipcRenderer.sendSync('clipboard-sync-state')
    this.userID = ipcRenderer.sendSync('get-user-id')
    ipcRenderer.on('clipboard-sync-state-sync', function (event, arg) {
      this.syncState = arg.state
    }.bind(this))
    ipcRenderer.on('clipboard-sync-state-device', function (event, arg) {
      this.deviceNum = arg.deviceNum
    }.bind(this))
    ipcRenderer.on('response-get_captcha_id', function (event, resp) {
      this.captchaID = resp.data.captcha_id
    }.bind(this))
    ipcRenderer.on('response-login', function (event, resp) {
      if (resp.errno === 0) {
        this.userID = resp.data.account
        this.$refs['login-modal'].hide()
        ipcRenderer.send('set-user-id', this.userID)
      } else {
        if (resp.errno === 4000012) {
          this.loginPasswordErr = Language.getLanguageText('password_wrong')
        } else if (resp.errno === 4000010) {
          this.loginAccountErr = Language.getLanguageText('account_not_exists')
        }
      }
    }.bind(this))
    ipcRenderer.on('response-register', function (event, resp) {
      if (resp.errno === 0) {
        this.userID = resp.data.account
        this.$refs['login-modal'].hide()
        ipcRenderer.send('set-user-id', this.userID)
      } else {
        if (resp.errno === 4000030) {
          this.registerCaptchaErr = Language.getLanguageText('captcha_code_wrong')
        } else if (resp.errno === 4000011) {
          this.registerAccountErr = Language.getLanguageText('account_exists')
        }
        this.getCaptchaID()
      }
    }.bind(this))
    ipcRenderer.on('response-change_password', function (event, resp) {
      if (resp.errno === 0) {
        this.$refs['change-password-modal'].hide()
      } else {
        if (resp.errno === 4000012) {
          this.changePasswordOriginPasswordErr = Language.getLanguageText('password_wrong')
        }
      }
    }.bind(this))
    this.$refs['login-modal'].$root.$on('bv::modal::hidden', function (bvEvent, modalId) {
      this.loginMode = 'login'
      this.loginAccount = ''
      this.loginAccountErr = ''
      this.loginPassword = ''
      this.loginPasswordErr = ''
      this.registerAccount = ''
      this.registerAccountErr = ''
      this.registerPassword = ''
      this.registerPasswordErr = ''
      this.registerPasswordRepeat = ''
      this.registerPasswordRepeatErr = ''
      this.registerCaptcha = ''
      this.registerCaptchaErr = ''
    }.bind(this))
    this.$refs['change-password-modal'].$root.$on('bv::modal::hidden', function (bvEvent, modalId) {
      this.changePasswordOriginPassword = ''
      this.changePasswordOriginPasswordErr = ''
      this.changePasswordNewPassword = ''
      this.changePasswordNewPasswordErr = ''
      this.changePasswordNewPasswordRepeat = ''
      this.changePasswordNewPasswordRepeatErr = ''
    }.bind(this))
    this.$refs['feedback-modal'].$root.$on('bv::modal::hidden', function (bvEvent, modalId) {
      this.feedbackContent = ''
    }.bind(this))
  },
  beforeDestroy: function () {
    ipcRenderer.removeAllListeners('clipboard-message-add')
    ipcRenderer.removeAllListeners('clipboard-message-delete')
    ipcRenderer.removeAllListeners('clipboard-sync-state-sync')
    ipcRenderer.removeAllListeners('response-get_captcha_id')
    ipcRenderer.removeAllListeners('response-login')
    ipcRenderer.removeAllListeners('response-register')
    ipcRenderer.removeAllListeners('response-change_password')
  }
}
</script>

<style scoped>
.scb-clipboard {
  height: 100%;
  width: 100%;
  overflow: hidden;
  background-image: linear-gradient(120deg, #8fd3f4 0%, #84fab0 100%);
}

.scb-clipboard-topbar {
  height: 4.0rem;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.scb-clipboard-keyword-form {
  box-sizing: border-box;
  margin: 0rem 0.5rem 0rem 0rem;
  flex: 1;
}

.scb-clipboard-keyword-form-input {
  background-color: rgba(255, 255, 255, 0.4);
  border: none;
  outline: none;
  border-radius: 0.5rem;
  padding: 0.5rem 1rem;
  display: block;
  width: 100%;
}

.scb-user-head {
  display: block;
  margin: 0rem 0.5rem;
}

.scb-topbar-sync-switch {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  margin-right: 0.5rem;
  padding: 0.2rem 0.6rem;
  background-color: rgba(255, 255, 255, 0.4);;
  border-radius: 0.5rem;
  color: #2c2c2c;
  font-weight: bold;
}

.scb-topbar-sync-switch-text {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.vue-js-switch {
  margin: 0rem !important;
}

.scb-cliboard-list {
  box-sizing: border-box;
  padding: 0.3rem;
  height: calc(100% - 4.0rem);
  overflow-y: scroll;
  overflow-x: hidden;
}

.scb-cliboard-list::-webkit-scrollbar {
    width: 0.5rem;
    background-color: transparent;
}

.scb-cliboard-list::-webkit-scrollbar-thumb {
    border-radius: 0.5rem;
    box-shadow: inset 0 0 6px rgba(0,0,0,.3);
    background-color: #555;
}

.scb-cliboard-list::-webkit-scrollbar-track {
    box-shadow: inset 0 0 0.3rem rgba(0,0,0,0.3);
    border-radius: 0.5rem;
    background-color: transparent;
}

.scb-cliboard-list-item {
  box-sizing: border-box;
  padding: 0.3rem;
}
</style>