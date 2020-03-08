import Vue from 'vue'
import App from './App.vue'
import { LayoutPlugin, IconsPlugin, ButtonPlugin } from 'bootstrap-vue'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'

Vue.config.productionTip = false

Vue.use(LayoutPlugin)
Vue.use(ButtonPlugin)
Vue.use(IconsPlugin)

new Vue({
  render: h => h(App),
}).$mount('#app')
