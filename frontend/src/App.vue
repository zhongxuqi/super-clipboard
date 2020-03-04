<template>
  <div id="app">
    <div class="scb-top">
      <b-container class="scb-topbar">
        <img class="scb-topbar-logo" alt="Vue logo" src="./assets/logo.png">
        <div class="scb-topbar-title">{{textSuperClipboard}}</div>
        <div style="flex:1"></div>
        <b-button variant="outline-light" size="sm">{{langs[nextLang]}}</b-button>
      </b-container>
      <b-container class="scb-topbody">
        <div class="scb-topbody-left">
          <h1 class="scb-title-text" style="font-weight:bold">{{ textSuperClipboard }}</h1>
          <h3 class="scb-title-text">{{ textSuperClipboardDesc }}</h3>
        </div>
        <div class="scb-topbody-right">
          <swiper :options="swiperOption">
            <swiper-slide><img class="scb-topbody-img" src="./assets/phone_pic_1.png"/></swiper-slide>
            <swiper-slide><img class="scb-topbody-img" src="./assets/phone_pic_2.png"/></swiper-slide>
            <swiper-slide><img class="scb-topbody-img" src="./assets/phone_pic_3.png"/></swiper-slide>
            <div class="swiper-pagination" slot="pagination"></div>
          </swiper>
        </div>
      </b-container>
    </div>
  </div>
</template>

<script>
import Language from './utils/Language'
import { swiper, swiperSlide } from 'vue-awesome-swiper'
import 'swiper/dist/css/swiper.css'

export default {
  name: 'App',
  components: {
    swiper,
    swiperSlide
  },
  data: function() {
    return {
      textSuperClipboard: Language.getLanguageText('super_clipboard'),
      textSuperClipboardDesc: Language.getLanguageText('super_clipboard_desc'),
      
      langs: {
        'en': 'English',
        'zh': '中文',
      },

      lang: Language.getLanguage(),

      swiperOption: {
        spaceBetween: 30,
        centeredSlides: true,
        autoplay: {
          delay: 2500,
          disableOnInteraction: false
        },
        pagination: {
          el: '.swiper-pagination',
          clickable: true
        },
        navigation: {
          nextEl: '.swiper-button-next',
          prevEl: '.swiper-button-prev'
        }
      }
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
    swiper() {
      return this.$refs.mySwiper.swiper
    },
  },
  mounted() {
    this.swiper.slideTo(3, 1000, false)
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

.scb-topbody {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: start;
  padding-top: 2rem;
}

.scb-topbody {
  padding: 2rem 5rem !important;
}

.scb-topbody-left {
  flex: 1;
}

.scb-topbody-right {
  position: relative;
  width: 25rem;
  margin-bottom: -15rem;
  border: 1rem solid #404040;
  background-color: #404040;
  border-radius: 2rem;
  overflow: hidden;
}

.scb-topbody-img {
  display: block;
  width: 23rem;
  border-radius: 1rem;
}

.scb-title-text {
  color: black;
  text-shadow: 0.1rem 0rem white, 0rem 0.1rem white, -0.1rem 0rem white, 0rem -0.1rem white, 
    0.1rem 0.1rem white, -0.1rem 0.1rem white, -0.1rem -0.1rem white, 0.1rem -0.1rem white;
}

</style>
