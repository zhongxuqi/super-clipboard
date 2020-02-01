<template>
  <div class="scb-app" style="height: 100vh">
    <div class="scb-sidebar">
      <div class="scb-user-info">
        <img class="scb-user-head" src="~@/assets/default_head.png" alt="electron-vue">
        <div class="scb-user-username md-title">Click to login</div>
      </div>
      <div class="scb-sidebar-sync">
        <div style="flex:1;margin-bottom:0.5rem">{{textAllPlatformSync}}</div>
        <div class="scb-sidebar-sync-switch"><ToggleButton v-model="syncState"></ToggleButton></div>
      </div>
      <div class="scb-sidebar-menu" style="box-sizing:border-box;padding:0.5rem">
        <div style="margin-bottom:0.5rem"><MenuBtn icon="icon-clipboard" v-bind:content="textClipboard" v-bind:active="mode==='clipboard'"></MenuBtn></div>
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
import { ToggleButton } from 'vue-js-toggle-button'
import { ipcRenderer } from 'electron'

export default {
  name: 'superclipboard',
  components: {
    MenuBtn,
    ToggleButton
  },
  data: function () {
    return {
      textClipboard: Language.getLanguageText('clipboard'),
      textAllPlatformSync: Language.getLanguageText('all_platform_sync'),

      mode: 'clipboard',
      syncState: false
    }
  },
  methods: {

  },
  mounted: function () {
    this.syncState = ipcRenderer.sendSync('clipboard-sync-state')
  },
  watch: {
    syncState: function (newValue, oldValue) {
      ipcRenderer.send('clipboard-sync-state-toggle', {state: newValue})
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

.scb-sidebar-sync {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  padding: 0rem 1rem;
  padding-top: 0.5rem;
  border-top: 1px solid #e4e4e4;
  border-bottom: 1px solid #e4e4e4;
}

.scb-sidebar-sync-switch {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
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