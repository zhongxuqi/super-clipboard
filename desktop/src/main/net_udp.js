import dgram from 'dgram'
let udpClient = dgram.createSocket('udp4')

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

const HeaderUdpServerSync = 0x00
const HeaderUdpDataSync = 0x01
const HeaderUdpDataSyncAck = 0x02

// 回调函数
let onChangeDeviceNum
let onReceiveMsg

// 内容接收
let deviceNum = 0
let receiveMaxLen = 1000
let receiveIndexMap = {}
let receiveMap = {}
let resultMap = {}

function ackBuf (metaBuffer, remoteInfo) {
  var buf = Buffer.alloc(metaBuffer.length + 2)
  buf[0] = HeaderUdpDataSyncAck
  buf[1] = metaBuffer.length
  for (var i = 0; i < metaBuffer.length; i++) {
    buf[i] = metaBuffer[i]
  }
  udpClient.send(buf, 0, buf.length, remoteInfo.port, remoteInfo.address)
}

function parseResult (key) {
  if (receiveMap[key][receiveIndexMap[key].offset] !== undefined) {
    let msg = receiveMap[key][receiveIndexMap[key].offset][0]
    let baseInfoLen = msg[0]
    let hasBaseInfo = false
    let baseInfoStr = ''
    let i = 0
    while (receiveMap[key][(receiveIndexMap[key].offset + i) % receiveMaxLen] !== undefined) {
      let msg = receiveMap[key][(receiveIndexMap[key].offset + i) % receiveMaxLen]
      if (i === 0) {
        msg = msg.slice(1)
      }
      if (baseInfoStr.length + msg.length < baseInfoLen) {
        baseInfoStr = baseInfoStr + msg.toString()
        i++
        continue
      }
      baseInfoStr = baseInfoStr + msg.slice(0, baseInfoLen - baseInfoStr.length)
      resultMap[key] = JSON.parse(baseInfoStr)
      hasBaseInfo = true
      if (baseInfoStr.length + msg.length > baseInfoLen) {
        receiveMap[key][receiveIndexMap[key].offset] = msg.slice(baseInfoLen - baseInfoStr.length)
      }
      break
    }
    if (hasBaseInfo) {
      receiveIndexMap[key].offset = (receiveIndexMap[key].offset + i) % receiveMaxLen
      receiveIndexMap[key].index += i
    }
  }
}

function checkFinish (key) {
  if (receiveIndexMap[key].index + 1 >= receiveIndexMap[key].total) {
    let msg = resultMap[key]
    onReceiveMsg(msg)
    receiveMap[key] = null
    resultMap[key] = null
  }
}

udpClient.on('message', function (buf, remoteInfo) {
  if (buf[0] === HeaderUdpServerSync) {
    let metaDataLen = buf[1]
    console.log(`receive message from ${remoteInfo.address}:${remoteInfo.port}：${buf.slice(2, 2 + metaDataLen).toString()}`)
    let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
    let newdeviceNum = 1
    if (metaDataJson.udp_addrs !== undefined && metaDataJson.udp_addrs !== null) {
      newdeviceNum += metaDataJson.udp_addrs.length
    }
    if (deviceNum !== newdeviceNum) {
      deviceNum = newdeviceNum
      if (typeof onChangeDeviceNum === 'function') onChangeDeviceNum(deviceNum)
    }
  } else if (buf[0] === HeaderUdpDataSync) {
    let metaDataLen = buf[1]
    let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
    if (metaDataJson.key === undefined || metaDataJson.key === null) return
    if (receiveMap[metaDataJson.key] === null) return
    if (receiveMap[metaDataJson.key] === undefined) {
      receiveMap[metaDataJson.key] = Array(receiveMaxLen)
      receiveIndexMap[metaDataJson.key] = {
        total: metaDataJson.total,
        offset: 0,
        index: 0
      }
    }
    if (receiveIndexMap[metaDataJson.key].index + receiveMaxLen <= metaDataJson.index) return
    if (receiveIndexMap[metaDataJson.key].index <= metaDataJson.index && receiveIndexMap[metaDataJson.key].index + receiveMaxLen > metaDataJson.index) {
      receiveMap[metaDataJson.key][(metaDataJson.index - receiveIndexMap[metaDataJson.key].index + receiveIndexMap[metaDataJson.key].offset) % receiveMaxLen] = buf.slice(2 + metaDataLen)
      parseResult(metaDataJson.key)
      checkFinish(metaDataJson.key)
    }
    ackBuf(buf.slice(2, 2 + metaDataLen), remoteInfo)
  }
})

// 内容发送
// const syncWorkMap = {}

// function createSyncWork() {

// }

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
      buffer[0] = HeaderUdpServerSync
      buffer[1] = metaData.length
      for (var i = 0; i < metaData.length; i++) {
        buffer[2 + i] = metaData[i]
      }
      udpClient.send(buffer, 0, buffer.length, 9000, '127.0.0.1')
    }, 1000)
  },
  listenMessage: function (callback) {
    udpClient.on('message', callback)
  },
  setOnChangeDeviceNum: function (f) {
    onChangeDeviceNum = f
  },
  setOnReceiveMsg: function (f) {
    onReceiveMsg = f
  },
  close: function () {
    if (intervalID === undefined) return
    clearInterval(intervalID)
    intervalID = undefined
  }
}
