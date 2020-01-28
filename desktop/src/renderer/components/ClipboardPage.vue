<template>
  <div class="scb-clipboard">
    <div class="scb-cliboard-list">
      <div class="scb-cliboard-list-item" v-for="item in msgList" v-bind:key="item.id">
        <ClipboardMessage></ClipboardMessage>
      </div>
    </div>
  </div>
</template>

<script>
import ClipboardMessage from './ClipboardMessage'

export default {
  name: 'clipboard-page',
  components: {
    ClipboardMessage
  },
  data: function () {
    return {
      msgList: [{
        id: 1
      }, {
        id: 2
      }]
    }
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