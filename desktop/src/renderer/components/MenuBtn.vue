<template>
  <div class="scb-menu-btn">
    <b-button class="scb-menu-btn-body" block variant="light" v-bind:pressed="active" v-on:click="onclick">
      <i class="iconfont" v-bind:class="iconClass"></i>
      <div>{{content}}</div>
    </b-button>
  </div>
</template>

<script>
export default {
  name: 'menu-btn',
  props: {
    content: String,
    icon: String,
    active: Boolean
  },
  computed: {
    iconClass: function () {
      var c = {}
      c[this.icon] = true
      return c
    }
  },
  methods: {
    onclick: function () {
      this.$emit('select')
    }
  }
}
</script>

<style scoped>
.scb-menu-btn {
  box-sizing: border-box;
}

.scb-menu-btn-body {
  text-align: left;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.scb-menu-btn-body i {
  font-size: 1.3rem;
  display: block;
  margin-right: 0.5rem;
}

.scb-menu-btn-body div {
  font-size: 1.1rem;
  flex: 1;
}
</style>