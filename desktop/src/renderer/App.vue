<template>
  <div class="scb-app" style="height: 100vh">
    <div class="scb-sidebar">
      <div class="scb-user-info">
        <img class="scb-user-head" src="~@/assets/default_head.png" alt="electron-vue">
        <div class="scb-user-username md-title">Click to login</div>
      </div>
      <div class="scb-sidebar-menu" style="margin-top:0.5rem">
        <MenuBtn icon="icon-clipboard" v-bind:content="textClipboard" v-bind:active="mode='clipboard'"></MenuBtn>
      </div>
    </div>
    <div class="scb-body">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
import MenuBtn from './components/MenuBtn'
import Language from './utils/Language'

export default {
  name: 'superclipboard',
  components: {
    MenuBtn
  },
  data: function () {
    return {
      textClipboard: Language.getLanguageText('clipboard'),

      mode: 'clipboard'
    }
  }
}
</script>

<style>
body {
  margin: 0rem;
}

.scb-app {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.scb-sidebar {
  border-right: 1px solid #e4e4e4;
  height: 100%;
  min-width: 15rem;
}

.scb-user-info {
  display: flex;
  flex-direction: row;
  justify-content: start;
  align-items: center;
  padding: 0.5rem 1rem;
  color: black;
}

.scb-user-head {
  display: block;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 999rem;
  margin-right: 1rem;
}

.scb-user-username {

}

.scb-sidebar-menu-item {
  display: block;
  box-sizing: border-box;
  padding: 0rem;
  color: white;
}

.scb-body {
  flex: 1;
  height: 100%;
  overflow: hidden;
}
</style>
