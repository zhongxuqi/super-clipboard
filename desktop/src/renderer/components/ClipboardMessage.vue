<template>
  <div class="scb-clipboard-message">
    <i class="iconfont icon-text_fields scb-clipboard-message-icon"></i>
    <div class="scb-clipboard-message-content">texsdffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffft</div>
    <b-button variant="success" size="sm"><i class="iconfont icon-copy"></i></b-button>
  </div>
</template>

<script>
export default {
  name: 'clipboard-message',
  props: {

  },
  data: function () {
    return {}
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