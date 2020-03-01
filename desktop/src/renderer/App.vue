<template>
  <div class="scb-app" style="height: 100vh">
    <div class="scb-body">
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
export default {
  name: 'superclipboard',
  components: {

  },
  data: function () {
    return {
      mode: 'clipboard'
    }
  },
  methods: {

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

.scb-body {
  flex: 1;
  height: 100%;
  overflow: hidden;
}
</style>