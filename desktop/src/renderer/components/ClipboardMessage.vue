<template>
  <div class="scb-clipboard-message">
    <i class="iconfont scb-clipboard-message-icon" v-bind:class="iconClass"></i>
    <div class="scb-clipboard-message-content"><pre>{{content}}</pre></div>
    <b-dropdown ref="dropdown" text="" variant="light" size="sm" style="margin-right:0.5rem">
      <b-dropdown-item-button v-on:click="setExpand(!expand)">{{expand?textFold:textExpand}}</b-dropdown-item-button>
      <b-dropdown-item v-on:click="clickDelete">Delete</b-dropdown-item>
    </b-dropdown>
    <b-button variant="success" size="sm" v-on:click="clickCopy"><i class="iconfont icon-copy"></i></b-button>
  </div>
</template>

<script>
import Consts from '../../common/Consts'
import Language from '../utils/Language'

export default {
  name: 'clipboard-message',
  props: {
    type: Number,
    content: String
  },
  data: function () {
    return {
      textExpand: Language.getLanguageText('expand'),
      textFold: Language.getLanguageText('fold')
    }
  },
  methods: {
    clickDelete: function () {
      this.$refs.dropdown.hide(true)
      this.$emit('ondelete')
    },
    clickCopy: function () {
      this.$emit('oncopy')
    }
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
}

.scb-clipboard-message-icon {
  display: block;
  font-size: 1.5rem;
}

.scb-clipboard-message-content {
  width: 0rem;
  flex: 1;
  margin: 0rem 0.5rem;
}

.scb-clipboard-message-content pre {
  margin: 0rem;
  padding: 0rem;
  word-break: keep-all;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}
</style>