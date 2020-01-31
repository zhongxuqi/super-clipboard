import dgram from 'dgram'
let udpClient = dgram.createSocket('udp4')

udpClient.on('message', function (msg, rinfo) {
  console.log(`receive message from ${rinfo.address}:${rinfo.port}：${msg}`)
})

let intervalID

export default {
  start: function () {
    if (intervalID !== undefined) return
    intervalID = setInterval(function () {
      var SendBuff = JSON.stringify({
        app_id: 'superclipboard'
      })
      var SendLen = SendBuff.length
      udpClient.send(SendBuff, 0, SendLen, 9000, '127.0.0.1')
    }, 3000)
  },
  close: function () {
    if (intervalID === undefined) return
    clearInterval(intervalID)
  }
}
