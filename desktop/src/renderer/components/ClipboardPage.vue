<template>
  <div class="scb-clipboard">
    <div class="scb-clipboard-topbar">
      <div class="scb-user-head">
        <b-button v-if="userID==''" variant="outline-success" v-on:click="openLogin">{{textClickToLogin}}</b-button>
        <b-dropdown v-if="userID!=''" v-bind:text="userID" variant="success">
          <b-dropdown-item href="#">Action</b-dropdown-item>
          <b-dropdown-item href="#">Another action</b-dropdown-item>
          <b-dropdown-item href="#">Something else here</b-dropdown-item>
        </b-dropdown>
      </div>
      <b-form class="scb-clipboard-keyword-form">
        <input class="scb-clipboard-keyword-form-input" v-model="keyword" v-bind:placeholder="textKeywordInputHint"/>
      </b-form>
      <div class="scb-topbar-sync-switch">
        <b-form-checkbox v-model="syncState" name="check-button" switch>
          {{syncState?syncStateDesc:textContentSync}}
        </b-form-checkbox>
      </div>
    </div>
    <div class="scb-cliboard-list">
      <div class="scb-cliboard-list-item" v-for="item in msgListFilter" v-bind:key="item.id">
        <ClipboardMessage v-bind:type="item.type" v-bind:syncState="syncState" v-bind:content="item.content" v-on:ondelete="deleteMsg(item)" v-on:onsync="syncMsg(item)" v-on:oncopy="copyMsg(item)"></ClipboardMessage>
      </div>
    </div>

    <b-modal ref="login-modal" hide-footer v-bind:title="loginMode=='login'?textLoginPage:textRegisterPage" size="sm">
      <form class="scb-login-form" @submit.stop.prevent v-if="loginMode=='login'">
        <b-form-group v-bind:label="textAccount">
          <b-form-input v-model="loginAccount" required></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPassword">
          <b-form-input v-model="loginPassword" required></b-form-input>
        </b-form-group>
        <b-button variant="success" block>{{textLogin}}</b-button>
        <b-button variant="outline-success" block v-on:click="changeLoginMode('register')">{{textGoRegister}}</b-button>
      </form>
      <form class="scb-register-form" @submit.stop.prevent v-if="loginMode=='register'">
        <b-form-group v-bind:label="textAccount">
          <b-form-input v-model="registerAccount" required></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPassword">
          <b-form-input v-model="registerPassword" required></b-form-input>
        </b-form-group>
        <b-form-group v-bind:label="textPasswordRepeat">
          <b-form-input v-model="registerPasswordRepeat" required></b-form-input>
        </b-form-group>
        <div style="display:flex;flex-direction:row;justify-content: center;align-items: center;">
          <b-form-group v-bind:label="textCaptchaCode" style="flex:1">
            <b-form-input v-model="registerCaptcha" required></b-form-input>
          </b-form-group>
          <img src="~@/assets/broken_image.png" style="margin:0rem 1rem;height:4rem"/>
        </div>
        <b-button variant="success" block>{{textRegister}}</b-button>
        <b-button variant="outline-success" block v-on:click="changeLoginMode('login')">{{textGoLogin}}</b-button>
      </form>
    </b-modal>
  </div>
</template>

<script>
import ClipboardMessage from './ClipboardMessage'
import { ipcRenderer } from 'electron'
import Language from '../utils/Language'

export default {
  name: 'clipboard-page',
  components: {
    ClipboardMessage
  },
  data: function () {
    return {
      textKeywordInputHint: Language.getLanguageText('keyword_input_hint'),
      textContentSync: Language.getLanguageText('content_sync'),
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

      msgList: [],
      keyword: '',
      syncState: false,
      deviceNum: 0,
      userID: '',

      loginMode: 'login',

      loginAccount: '',
      loginAccountError: '',
      loginPassword: '',
      loginPasswordError: '',

      registerAccount: '',
      registerAccountError: '',
      registerPassword: '',
      registerPasswordError: '',
      registerPasswordRepeat: '',
      registerPasswordRepeatError: '',
      registerCaptcha: '',
      registerCaptchaError: ''
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
      this.$refs['login-modal'].show()
    },
    changeLoginMode: function (mode) {
      this.loginMode = mode
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
      ipcRenderer.send('clipboard-sync-state-toggle', {state: newValue})
    }
  },
  mounted: function () {
    ipcRenderer.send('clipboard-message-connect', '')
    ipcRenderer.on('clipboard-message-add', this.onAddMessage.bind(this))
    ipcRenderer.on('clipboard-message-delete', this.onDeleteMessage.bind(this))
    this.syncState = ipcRenderer.sendSync('clipboard-sync-state')
    ipcRenderer.on('clipboard-sync-state-sync', function (event, arg) {
      this.syncState = arg.state
    }.bind(this))
    ipcRenderer.on('clipboard-sync-state-device', function (event, arg) {
      this.deviceNum = arg.deviceNum
    }.bind(this))
  },
  beforeDestroy: function () {
    ipcRenderer.removeAllListeners('clipboard-message-add')
    ipcRenderer.removeAllListeners('clipboard-message-delete')
    ipcRenderer.removeAllListeners('clipboard-sync-state-sync')
  }
}
</script>

<style scoped>
.scb-clipboard {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.scb-clipboard-topbar {
  height: 4.0rem;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  border-bottom: 1px solid #e4e4e4;
  background-image: linear-gradient(120deg, #8fd3f4 0%, #84fab0 100%);
}

.scb-clipboard-keyword-form {
  box-sizing: border-box;
  padding: 0rem;
  margin: 0rem 0.5rem 0rem 0rem;
  flex: 1;
  padding: 0rem 1rem;
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

.scb-cliboard-list-item {
  box-sizing: border-box;
  padding: 0.3rem;
}
</style>