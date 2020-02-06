import dgram from 'dgram'
import { sha256 } from 'js-sha256'
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

const HeaderUdpServerSync = 0x00
const HeaderUdpDataSync = 0x01
const HeaderUdpDataSyncAck = 0x02

const udpWindowMaxLen = 1000

// 回调函数
let onChangeDeviceNum
let onReceiveMsg

// 内容发送
const syncWorkerMap = {}
const sendBufferMaxLen = 400
function createSyncWorker (remoteAddr) {
  let addrItems = remoteAddr.split(':')
  let ip = addrItems[0]
  let port = parseInt(addrItems[1])
  let isRunning = true

  let sendAcks = Array(udpWindowMaxLen)
  let sendTimes = Array(udpWindowMaxLen)
  let sendRetryTime = 3000
  let sendBuffers = Array(udpWindowMaxLen)
  let sendBufferOffset = 0
  let clipboardMsgs = []
  let currMsg

  let intervalID = setInterval(function () {
    if (!isRunning) {
      clearInterval(intervalID)
      return
    }
    if (currMsg === undefined) {
      if (clipboardMsgs.length > 0) {
        currMsg = {
          originMsg: clipboardMsgs.pop()
        }
        currMsg.originMsg.create_time = undefined
        currMsg.originMsg.update_time = undefined
        currMsg.baseInfoBuffer = Buffer.from(JSON.stringify(currMsg.originMsg), 'utf-8')
        currMsg.key = sha256(currMsg.baseInfoBuffer.toString())
        currMsg.total = Math.ceil(currMsg.baseInfoBuffer.length / sendBufferMaxLen)
        currMsg.index = 0
      } else {
        return
      }
    }
    let i = 0
    for (; sendBuffers[(i + sendBufferOffset) % udpWindowMaxLen] !== undefined && i < udpWindowMaxLen; i++) {
      let realIndex = (i + sendBufferOffset) % udpWindowMaxLen
      if (sendAcks[realIndex] === undefined && sendTimes[realIndex] + sendRetryTime < Date.now()) {
        udpClient.send(sendBuffers[realIndex], 0, sendBuffers[realIndex].length, port, ip)
        sendTimes[realIndex] = Date.now()
      }
    }
    for (; i < udpWindowMaxLen && i < currMsg.total; i++) {
      if (i * sendBufferMaxLen < currMsg.baseInfoBuffer.length) {
        let realIndex = (i + sendBufferOffset) % udpWindowMaxLen
        let metaDataJson = {
          key: currMsg.key,
          total: currMsg.total,
          index: currMsg.index + i
        }
        let metaData = str2UTF8(JSON.stringify(metaDataJson))
        let bufferLen = 0
        let isFirst = 0
        if (metaDataJson.index === 0) isFirst = 1
        if (currMsg.baseInfoBuffer.length > i * sendBufferMaxLen + sendBufferMaxLen) {
          bufferLen = sendBufferMaxLen
        } else {
          bufferLen = currMsg.baseInfoBuffer.length - i * sendBufferMaxLen
        }
        let buffer = Buffer.alloc(2 + metaData.length + isFirst + bufferLen)
        buffer[0] = HeaderUdpServerSync
        buffer[1] = metaData.length
        for (let j = 0; j < metaData.length; j++) {
          buffer[2 + j] = metaData[j]
        }
        if (isFirst === 1) {
          buffer[2 + metaData.length] = currMsg.baseInfoBuffer.length
        }
        for (let j = 0; j < bufferLen; j++) {
          // TODO 需要升级支持文件
          buffer[2 + metaData.length + isFirst + j] = currMsg.baseInfoBuffer.slice(metaDataJson.index * sendBufferMaxLen, metaDataJson.index * sendBufferMaxLen + bufferLen)
        }
        sendBuffers[realIndex] = buffer
        sendTimes[realIndex] = Date.now()
        udpClient.send(buffer, 0, buffer.length, port, ip)
      }
    }
  }, 500)
  return {
    close: function () {
      isRunning = false
    },
    sendClipboardMsg: function (msg) {
      clipboardMsgs.splice(0, 0, msg)
    },
    ack: function (metaDataJson) {
      if (metaDataJson.index >= currMsg.index && metaDataJson.index < currMsg.index + udpWindowMaxLen) {
        sendAcks[(metaDataJson.index - currMsg.index + sendBufferOffset) % udpWindowMaxLen] = metaDataJson
      }

      // check acks
      let i = 0
      for (; sendAcks[(i + sendBufferOffset) % udpWindowMaxLen] !== undefined; i++) {
        let realIndex = (i + sendBufferOffset) % udpWindowMaxLen
        sendAcks[realIndex] = undefined
        sendTimes[realIndex] = undefined
        sendBuffers[realIndex] = undefined
        sendBufferOffset = (sendBufferOffset + 1) % udpWindowMaxLen
        currMsg.index += 1
      }
    }
  }
}

function refreshSyncWork (remoteAddrs) {
  let remoteAddrMap = {}
  for (let i = 0; i < remoteAddrs.length; i++) {
    remoteAddrMap[remoteAddrs[i]] = true
  }

  // 删除无效SyncWorker
  let addrs2Remove = []
  for (let addr in syncWorkerMap) {
    if (remoteAddrMap[addr] === undefined) {
      syncWorkerMap[addr].close()
      addrs2Remove.push(addr)
    }
  }
  for (let i = 0; i < addrs2Remove.length; i++) {
    syncWorkerMap[addrs2Remove[i]] = undefined
  }

  // 创建新的
  for (let addr in remoteAddrMap) {
    if (syncWorkerMap[addr] === undefined) {
      syncWorkerMap[addr] = createSyncWorker(addr)
    }
  }
}

// 内容接收
let deviceNum = 0
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
    while (receiveMap[key][(receiveIndexMap[key].offset + i) % udpWindowMaxLen] !== undefined) {
      let msg = receiveMap[key][(receiveIndexMap[key].offset + i) % udpWindowMaxLen]
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
      receiveIndexMap[key].offset = (receiveIndexMap[key].offset + i) % udpWindowMaxLen
      receiveIndexMap[key].index += i
    }
    // TODO 需要升级支持文件
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
    if (newdeviceNum > 1) refreshSyncWork(metaDataJson.udp_addrs)
    else refreshSyncWork([])
  } else if (buf[0] === HeaderUdpDataSync) {
    let metaDataLen = buf[1]
    let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
    if (metaDataJson.key === undefined || metaDataJson.key === null) return
    if (receiveMap[metaDataJson.key] === null) return
    if (receiveMap[metaDataJson.key] === undefined) {
      receiveMap[metaDataJson.key] = Array(udpWindowMaxLen)
      receiveIndexMap[metaDataJson.key] = {
        total: metaDataJson.total,
        offset: 0,
        index: 0
      }
    }
    if (receiveIndexMap[metaDataJson.key].index + udpWindowMaxLen <= metaDataJson.index) return
    if (receiveIndexMap[metaDataJson.key].index <= metaDataJson.index && receiveIndexMap[metaDataJson.key].index + udpWindowMaxLen > metaDataJson.index) {
      receiveMap[metaDataJson.key][(metaDataJson.index - receiveIndexMap[metaDataJson.key].index + receiveIndexMap[metaDataJson.key].offset) % udpWindowMaxLen] = buf.slice(2 + metaDataLen)
      parseResult(metaDataJson.key)
      checkFinish(metaDataJson.key)
    }
    ackBuf(buf.slice(2, 2 + metaDataLen), remoteInfo)
  } else if (buf[0] === HeaderUdpDataSyncAck) {
    let metaDataLen = buf[1]
    let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
    let worker = syncWorkerMap[`${remoteInfo.address}:${remoteInfo.port}`]
    if (worker !== undefined) worker.ack(metaDataJson)
  }
})

let heartBeatIntervalID
export default {
  isStart: function () {
    return heartBeatIntervalID !== undefined
  },
  start: function () {
    if (heartBeatIntervalID !== undefined) return
    heartBeatIntervalID = setInterval(function () {
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
  setOnChangeDeviceNum: function (f) {
    onChangeDeviceNum = f
  },
  setOnReceiveMsg: function (f) {
    onReceiveMsg = f
  },
  sendClipboardMsg: function (msg) {
    if (heartBeatIntervalID === undefined) return
    for (let addr in syncWorkerMap) {
      syncWorkerMap[addr].sendClipboardMsg(msg)
    }
  },
  close: function () {
    if (heartBeatIntervalID === undefined) return
    clearInterval(heartBeatIntervalID)
    heartBeatIntervalID = undefined
  }
}
