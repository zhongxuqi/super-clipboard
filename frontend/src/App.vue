<template>
  <div id="app">
    <div class="scb-top">
      <b-container class="scb-topbar">
        <img class="scb-topbar-logo" alt="Vue logo" src="./assets/logo.png">
        <div class="scb-topbar-title scb-title-text">{{textSuperClipboard}}</div>
        <div style="flex:1"></div>
        <b-button variant="outline-dark" size="sm" v-on:click="toggleLang">{{langs[nextLang].name}}</b-button>
      </b-container>
      <b-container class="scb-topbody">
        <b-row>
          <b-col class="scb-topbody-left" lg="7">
            <h1 class="scb-title-text" style="font-weight:bold">{{ textSuperClipboard }}</h1>
            <h3 class="scb-title-text">{{ textSuperClipboardDesc }}</h3>
            <div style="height:10rem"></div>
          </b-col>
          <b-col class="scb-topbody-right" lg="5">
            <swiper :options="swiperOption">
              <swiper-slide v-if="lang!='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_1.png"/></swiper-slide>
              <swiper-slide v-if="lang!='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_2.png"/></swiper-slide>
              <swiper-slide v-if="lang!='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_3.png"/></swiper-slide>
              <swiper-slide v-if="lang!='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_4.png"/></swiper-slide>
              <swiper-slide v-if="lang=='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_1_en.png"/></swiper-slide>
              <swiper-slide v-if="lang=='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_2_en.png"/></swiper-slide>
              <swiper-slide v-if="lang=='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_3_en.png"/></swiper-slide>
              <swiper-slide v-if="lang=='en'" style="margin:0rem;overflow:hidden;border-radius:1rem;"><img class="scb-topbody-img" src="./assets/phone_pic_4_en.png"/></swiper-slide>
              <div class="swiper-pagination" slot="pagination"></div>
            </swiper>
          </b-col>
        </b-row>
      </b-container>
    </div>
    <div class="scb-feature">
      <b-container>
        <b-row style="text-align: center">
          <h1 class="scb-feature-title scb-title-text" style="margin:4rem auto 2rem">{{textFeatureTitle}}</h1>
        </b-row>
        <b-row>
          <b-col style="text-align: center">
            <div class="scb-feature-item"><b-icon class="scb-feature-icon" icon="window"></b-icon></div>
            <h2 class="scb-feature-item-title scb-title-text">{{textFloatWindowDesign}}</h2>
          </b-col>
          <b-col style="text-align: center">
            <div class="scb-feature-item"><b-icon class="scb-feature-icon" icon="search"></b-icon></div>
            <h2 class="scb-feature-item-title scb-title-text">{{textKeywordSearch}}</h2>
          </b-col>
          <b-col style="text-align: center">
            <div class="scb-feature-item"><b-icon class="scb-feature-icon" icon="arrows-collapse"></b-icon></div>
            <h2 class="scb-feature-item-title scb-title-text">{{textWindowMin}}</h2>
          </b-col>
          <b-col style="text-align: center">
            <div class="scb-feature-item"><b-icon class="scb-feature-icon" icon="wifi"></b-icon></div>
            <h2 class="scb-feature-item-title scb-title-text">{{textClipboardSync}}</h2>
          </b-col>
        </b-row>
      </b-container>
    </div>
    <div class="scb-download">
      <b-container>
        <b-row style="text-align: center">
          <h1 class="scb-feature-title scb-title-text" style="margin:4rem auto 2rem">{{textAppDownload}}</h1>
        </b-row>
        <b-row>
          <b-col>
            <h3 class="scb-title-text" style="text-align:center">{{textMobile}}</h3>
            <div class="scb-download-table">
              <b-row class="scb-download-row">
                <b-col class="scb-download-row-left scb-title-text">Android</b-col>
                <b-col class="scb-download-row-right">
                  <b-button variant="link" v-bind:href="`https://${ossDomain}/app-release-1.0.apk`"><b-icon icon="cloud-download"></b-icon></b-button>
                </b-col>
              </b-row>
            </div>
          </b-col>
          <b-col>
            <h3 class="scb-title-text" style="text-align:center">{{textDesktop}}</h3>
            <div class="scb-download-table">
              <b-row class="scb-download-row">
                <b-col class="scb-download-row-left scb-title-text">Windows</b-col>
                <b-col class="scb-download-row-right">
                  <b-button variant="link" v-bind:href="`https://${ossDomain}/superclipboard%20Setup%201.0.0.exe`"><b-icon icon="cloud-download"></b-icon></b-button>
                </b-col>
              </b-row>
              <b-row class="scb-download-row">
                <b-col class="scb-download-row-left scb-title-text">Mac</b-col>
                <b-col class="scb-download-row-right">
                  <!-- <b-button variant="link"><b-icon icon="cloud-download"></b-icon></b-button> -->
                  {{textComingSoon}}
                </b-col>
              </b-row>
              <b-row class="scb-download-row">
                <b-col class="scb-download-row-left scb-title-text">Linux</b-col>
                <b-col class="scb-download-row-right">
                  <b-button variant="link" v-bind:href="`https://${ossDomain}/superclipboard-1.0.0.AppImage`"><b-icon icon="cloud-download"></b-icon></b-button>
                </b-col>
              </b-row>
            </div>
          </b-col>
        </b-row>
      </b-container>
    </div>
    <div class="scb-footer">
      <b-container>
        <b-row>
          <p class="col-8">Copyright © 2020-2022.EasyPass All rights reserved. <a style="color:white" href="http://www.beian.miit.gov.cn">浙ICP备18002047号-4</a></p>
          <p class="col-4" style="text-align: right">
            <a href="mailto:easypass.inc@gmail.com" style="color: white">
              <i class="far fa-envelope" style="display: inline-block;margin-right: 5px"></i>
              Email官方邮箱
            </a>
          </p>
        </b-row>
      </b-container>
    </div>
  </div>
</template>

<script>
import Language from './utils/Language'
import { swiper, swiperSlide } from 'vue-awesome-swiper'
import 'swiper/dist/css/swiper.css'
import { BIcon } from 'bootstrap-vue'

export default {
  name: 'App',
  components: {
    swiper,
    swiperSlide,
    BIcon
  },
  data: function() {
    return {
      textSuperClipboard: Language.getLanguageText('super_clipboard'),
      textSuperClipboardDesc: Language.getLanguageText('super_clipboard_desc'),
      textFloatWindowDesign: Language.getLanguageText('float_window_design'),
      textFeatureTitle: Language.getLanguageText('feature_title'),
      textKeywordSearch: Language.getLanguageText('keyword_search'),
      textWindowMin: Language.getLanguageText('window_min'),
      textAppDownload: Language.getLanguageText('app_download'),
      textMobile: Language.getLanguageText('mobile'),
      textDesktop: Language.getLanguageText('desktop'),
      textClipboardSync: Language.getLanguageText('clipboard_sync'),
      textComingSoon: Language.getLanguageText('coming_soon'),

      ossDomain: 'easypass-tech.oss-cn-hangzhou.aliyuncs.com',
      // ossDomain: 'easypass-tech-us.oss-us-west-1.aliyuncs.com',
      
      langs: {
        'en': {
          name: 'English',
          search: '?lang=en',
        },
        'zh': {
          name: '中文',
          search: '?lang=zh',
        },
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
      },
    }
  },
  methods: {
    toggleLang: function() {
      window.location.search = this.langs[this.nextLang].search
    },
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
  mounted() {
    
  },
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
  background-image: linear-gradient(120deg, #8fd3f4 0%, #84fab0 100%);
  overflow-y: scroll;
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
  padding-top: 2rem;
}

.scb-topbody {
  padding: 2rem 5rem !important;
}

.scb-topbody-left {
  margin-top: 10rem;
}

.scb-topbody-right {
  width: 100%;
  background-color: #404040;
  border-radius: 2rem;
  padding: 1rem;
  box-sizing: border-box;
  overflow: hidden;
}

.scb-topbody-img {
  display: block;
  width: 100%;
  border-radius: 1rem;
  overflow: hidden;
}

.scb-title-text {
  color: black;
  text-shadow: 0.1rem 0rem white, 0rem 0.1rem white, -0.1rem 0rem white, 0rem -0.1rem white, 
    0.1rem 0.1rem white, -0.1rem 0.1rem white, -0.1rem -0.1rem white, 0.1rem -0.1rem white;
}

.scb-feature {
  margin-top: 5rem;
}

.scb-feature-item {
  margin-bottom: 1rem;
}

.scb-feature-icon {
  width: 10rem;
  height: 10rem;
  padding: 1rem;
  border-radius: 2rem;
  background-color: rgba(255, 255, 255, 0.3)
}

.scb-download {
  margin: 5rem 0rem;
}

.scb-download-table {
  margin: 0rem 0rem !important;
  border: 1px solid white;
  border-radius: 0.5rem;
}

.scb-download-row {
  margin: 0rem !important;
  font-size: 1.3rem;
  border-bottom: 1px solid white;
}

.scb-download-row:last-child {
  border: none;
}

.scb-download-row-left,.scb-download-row-right {
  padding: 0.4rem;
}

.scb-download-row-right {
  text-align: right;
}

.scb-footer {
  background-color: rgba(0, 0, 0, 0.2);
  color: white;
  margin: 0rem;
  padding: 1rem;
}

.scb-footer p {
  font-size: 15px;
  margin: 0px;
  word-break: break-all;
}
</style>
