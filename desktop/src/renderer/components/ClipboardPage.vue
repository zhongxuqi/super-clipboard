<template>
  <div class="scb-clipboard">
    <b-form class="scb-clipboard-keyword-form">
      <b-form-input
        v-model="keyword"
        v-bind:placeholder="textKeywordInputHint"
      ></b-form-input>
    </b-form>
    <div class="scb-cliboard-list">
      <div class="scb-cliboard-list-item" v-for="item in msgListFilter" v-bind:key="item.id">
        <ClipboardMessage v-bind:type="item.type" v-bind:content="item.content" v-on:ondelete="deleteMsg(item)" v-on:onsync="syncMsg(item)" v-on:oncopy="copyMsg(item)"></ClipboardMessage>
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

      msgList: [],
      keyword: ''
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
    }
  },
  mounted: function () {
    ipcRenderer.send('clipboard-message-connect', '')
    ipcRenderer.on('clipboard-message-add', this.onAddMessage.bind(this))
    ipcRenderer.on('clipboard-message-delete', this.onDeleteMessage.bind(this))
  },
  beforeDestroy: function () {
    ipcRenderer.removeAllListeners('clipboard-message-add')
    ipcRenderer.removeAllListeners('clipboard-message-delete')
  }
}
</script>

<style scoped>
.scb-clipboard {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.scb-clipboard-keyword-form {
  box-sizing: border-box;
  padding: 0rem 0.5rem;
  border-bottom: 1px solid #e4e4e4;
  height: 3.5rem;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.scb-cliboard-list {
  box-sizing: border-box;
  padding: 0.3rem;
  height: calc(100% - 3.5rem);
  overflow-y: scroll;
  overflow-x: hidden;
}

.scb-cliboard-list-item {
  box-sizing: border-box;
  padding: 0.3rem;
}
</style>