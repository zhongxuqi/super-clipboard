import dgram from 'dgram'
let udpClient = dgram.createSocket('udp4')

udpClient.on('message', function (msg, remoteInfo) {
  let metaDataLen = msg[1]
  console.log(`receive message from ${remoteInfo.address}:${remoteInfo.port}：${msg.slice(2, 2 + metaDataLen).toString()}`)
})

function str2UTF8 (str) {
  var bytes = []
  var len, c
  len = str.length
  for (var i = 0; i < len; i++) {
    c = str.charCodeAt(i)
    if (c >= 0x010000 && c <= 0x10FFFF) {
      bytes.push(((c >> 18) & 0x07) | 0xF0)
      bytes.push(((c >> 12) & 0x3F) | 0x80)
      bytes.push(((c >> 6) & 0x3F) | 0x80)
      bytes.push((c & 0x3F) | 0x80)
    } else if (c >= 0x000800 && c <= 0x00FFFF) {
      bytes.push(((c >> 12) & 0x0F) | 0xE0)
      bytes.push(((c >> 6) & 0x3F) | 0x80)
      bytes.push((c & 0x3F) | 0x80)
    } else if (c >= 0x000080 && c <= 0x0007FF) {
      bytes.push(((c >> 6) & 0x1F) | 0xC0)
      bytes.push((c & 0x3F) | 0x80)
    } else {
      bytes.push(c & 0xFF)
    }
  }
  return bytes
}

let intervalID

const header = 0x00

export default {
  isStart: function () {
    return intervalID !== undefined
  },
  start: function () {
    if (intervalID !== undefined) return
    intervalID = setInterval(function () {
      var metaData = str2UTF8(JSON.stringify({
        app_id: 'superclipboard'
      }))
      var buffer = Buffer.alloc(metaData.length + 2)
      buffer[0] = header
      buffer[1] = metaData.length
      for (var i = 0; i < metaData.length; i++) {
        buffer[2 + i] = metaData[i]
      }
      udpClient.send(buffer, 0, buffer.length, 9000, '127.0.0.1')
    }, 1000)
  },
  close: function () {
    if (intervalID === undefined) return
    clearInterval(intervalID)
    intervalID = undefined
  }
}
