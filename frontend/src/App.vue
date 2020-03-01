<template>
  <div id="app">
    <div class="scb-top">
      <b-container class="scb-topbar">
        <img class="scb-topbar-logo" alt="Vue logo" src="./assets/logo.png">
        <div class="scb-topbar-title">{{textSuperClipboard}}</div>
        <div style="flex:1"></div>
        <b-button variant="outline-light" size="sm">{{langs[nextLang]}}</b-button>
      </b-container>
    </div>
  </div>
</template>

<script>
import Language from './utils/Language'

export default {
  name: 'App',
  components: {
    
  },
  data: function() {
    return {
      textSuperClipboard: Language.getLanguageText('super_clipboard'),
      
      langs: {
        'en': 'English',
        'zh': '中文',
      },

      lang: Language.getLanguage(),
    }
  },
  computed: {
    nextLang: function() {
      for (var key in this.langs) {
        if (key != this.lang) {
          return key
        }
      }
      return this.lang
    },
  },
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.scb-top {
  background-image: linear-gradient(120deg, #8fd3f4 0%, #84fab0 100%);
}

.scb-topbar {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.scb-topbar-logo {
  height: 2.4rem;
  display: block;
  margin-right: 0.5rem;
}

.scb-topbar-title {
  color: white;
  font-size: 1.1rem;
}
</style>
