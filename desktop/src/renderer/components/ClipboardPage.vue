<template>
  <div class="scb-clipboard">
    <div class="scb-clipboard-topbar">
      <img class="scb-user-head" src="~@/assets/default_head.png" alt="electron-vue">
      <b-form class="scb-clipboard-keyword-form">
        <b-form-input
          v-model="keyword"
          v-bind:placeholder="textKeywordInputHint"
        ></b-form-input>
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

      msgList: [],
      keyword: '',
      syncState: false,
      deviceNum: 0
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
}

.scb-user-head {
  display: block;
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 999rem;
  margin: 0rem 0.5rem;
  padding: 0.5rem;
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