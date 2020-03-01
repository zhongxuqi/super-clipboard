<template>
  <div class="scb-clipboard-message">
    <i class="iconfont scb-clipboard-message-icon icon-ok1" style="color: #28a745" v-if="copying"></i>
    <div class="scb-clipboard-message-content" v-b-popover.hover.top="content" v-if="!copying">
      <pre>{{content}}</pre>
    </div>
    <div class="scb-clipboard-message-content" v-if="copying">{{textCopied}}</div>
    <b-dropdown ref="dropdown" text="" variant="light" size="sm" style="margin-right:0.5rem">
      <b-dropdown-item v-if="syncState" v-on:click="clickSync">
        <i class="iconfont icon-refresh scb-action-icon"></i>
        {{textSync}}
      </b-dropdown-item>
      <b-dropdown-item v-on:click="clickDelete">
        <i class="iconfont icon-delete scb-action-icon"></i>
        {{textDelete}}
      </b-dropdown-item>
    </b-dropdown>
    <b-button variant="success" size="sm" v-on:click="clickCopy" v-bind:pressed="copying"><i class="iconfont" v-bind:class="{'icon-copy':!copying,'icon-ok':copying}"></i></b-button>
  </div>
</template>

<script>
import Consts from '../../common/Consts'
import Language from '../utils/Language'

export default {
  name: 'clipboard-message',
  props: {
    type: Number,
    syncState: Boolean,
    content: String
  },
  data: function () {
    return {
      textExpand: Language.getLanguageText('expand'),
      textFold: Language.getLanguageText('fold'),
      textDelete: Language.getLanguageText('delete'),
      textSync: Language.getLanguageText('sync'),
      textCopied: Language.getLanguageText('copied'),

      copying: false
    }
  },
  methods: {
    clickSync: function () {
      this.$refs.dropdown.hide(true)
      this.$emit('onsync')
    },
    clickDelete: function () {
      this.$refs.dropdown.hide(true)
      this.$emit('ondelete')
    },
    clickCopy: function () {
      if (this.copying) return
      this.copying = true
      this.$emit('oncopy')
      setTimeout(function () {
        this.copying = false
      }.bind(this), 1000)
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

.scb-action-icon {
  display: inline-block;
  font-size: 1.2rem;
  margin-right: 0.5rem;
}
</style>