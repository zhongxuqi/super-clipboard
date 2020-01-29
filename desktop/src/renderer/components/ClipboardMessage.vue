<template>
  <div class="scb-clipboard-message">
    <i class="iconfont scb-clipboard-message-icon" v-bind:class="iconClass"></i>
    <div class="scb-clipboard-message-content">{{content}}</div>
    <b-button variant="success" size="sm"><i class="iconfont icon-copy"></i></b-button>
  </div>
</template>

<script>
import Consts from '../../common/Consts'

export default {
  name: 'clipboard-message',
  props: {
    type: Number,
    content: String
  },
  data: function () {
    return {}
  },
  computed: {
    iconClass: function () {
      let c = {}
      switch (this.type) {
        case Consts.MessageType.Text:
          c['icon-text_fields'] = true
          break
        case Consts.MessageType.Image:
          c['icon-tupian2'] = true
          break
      }
      return c
    }
  }
}
</script>

<style scoped>
.scb-clipboard-message {
  width: 100%;
  border: 1px solid #e4e4e4;
  border-radius: 0.3rem;
  display: flex;
  flex-direction: row;
  justify-content: left;
  align-items: center;
  box-sizing: border-box;
  padding: 0.4rem;
  overflow: hidden;
}

.scb-clipboard-message-icon {
  display: block;
  font-size: 1.5rem;
}

.scb-clipboard-message-content {
  width: 0rem;
  flex: 1;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  margin: 0rem 0.5rem;
}
</style>