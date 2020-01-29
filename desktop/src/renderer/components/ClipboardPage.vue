<template>
  <div class="scb-clipboard">
    <div class="scb-cliboard-list">
      <div class="scb-cliboard-list-item" v-for="item in msgList" v-bind:key="item.id">
        <ClipboardMessage v-bind:type="item.type" v-bind:content="item.content"></ClipboardMessage>
      </div>
    </div>
  </div>
</template>

<script>
import ClipboardMessage from './ClipboardMessage'
import { ipcRenderer } from 'electron'

export default {
  name: 'clipboard-page',
  components: {
    ClipboardMessage
  },
  data: function () {
    return {
      connectID: 0,
      msgList: []
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
  overflow-y: scroll;
  overflow-x: hidden;
}

.scb-cliboard-list {
  box-sizing: border-box;
  padding: 0.3rem;
}

.scb-cliboard-list-item {
  box-sizing: border-box;
  padding: 0.3rem;
}
</style>