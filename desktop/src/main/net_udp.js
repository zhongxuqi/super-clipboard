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

function uint2Bytes (num) {
  let bytes = Buffer.alloc(4)
  bytes[0] = (num >> 24) & 0xff
  bytes[1] = (num >> 16) & 0xff
  bytes[2] = (num >> 8) & 0xff
  bytes[3] = num & 0xff
  return bytes
}

function bytes2Uint (bytes) {
  let num = 0
  for (let i = 0; i < bytes.length; i++) {
    num = num << 8 + bytes[0]
  }
  return num
}

const HeaderUdpServerSync = 0x00
const HeaderUdpDataSync = 0x01
const HeaderUdpDataSyncAck = 0x02

const UdpWindowMaxLen = 1000

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

  let sendAcks = Array(UdpWindowMaxLen)
  let sendTimes = Array(UdpWindowMaxLen)
  let sendRetryTime = 3000
  let sendBuffers = Array(UdpWindowMaxLen)
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
    for (; sendBuffers[(i + currMsg.index) % UdpWindowMaxLen] !== undefined && i < UdpWindowMaxLen; i++) {
      let realIndex = (i + currMsg.index) % UdpWindowMaxLen
      if (sendAcks[realIndex] === undefined && sendTimes[realIndex] + sendRetryTime < Date.now()) {
        udpClient.send(sendBuffers[realIndex], 0, sendBuffers[realIndex].length, port, ip)
        sendTimes[realIndex] = Date.now()
      }
    }
    for (; i < UdpWindowMaxLen && i < currMsg.total; i++) {
      if (i * sendBufferMaxLen < currMsg.baseInfoBuffer.length) {
        let realIndex = (i + currMsg.index) % UdpWindowMaxLen
        let metaDataJson = {
          key: currMsg.key,
          total: currMsg.total,
          index: currMsg.index + i
        }
        let metaData = str2UTF8(JSON.stringify(metaDataJson))
        let bufferLen = 0
        let isFirst = 0
        if (metaDataJson.index === 0) isFirst = 4
        if (currMsg.baseInfoBuffer.length > (i + 1) * sendBufferMaxLen) {
          bufferLen = sendBufferMaxLen
        } else {
          bufferLen = currMsg.baseInfoBuffer.length - i * sendBufferMaxLen
        }
        let buffer = Buffer.alloc(2 + metaData.length + isFirst + bufferLen)
        buffer[0] = HeaderUdpDataSync
        buffer[1] = metaData.length
        for (let j = 0; j < metaData.length; j++) {
          buffer[2 + j] = metaData[j]
        }
        if (isFirst > 0) {
          let lenBytes = uint2Bytes(currMsg.baseInfoBuffer.length)
          for (let j = 0; j < isFirst; j++) {
            buffer[2 + metaData.length + j] = lenBytes[j]
          }
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
    // TODO 需要升级支持文件
  }, 500)
  return {
    close: function () {
      isRunning = false
    },
    sendClipboardMsg: function (msg) {
      clipboardMsgs.splice(0, 0, msg)
    },
    ack: function (metaDataJson) {
      if (currMsg == null || metaDataJson.key !== currMsg.key) return
      if (metaDataJson.index >= currMsg.index && metaDataJson.index < currMsg.index + UdpWindowMaxLen) {
        sendAcks[metaDataJson.index % UdpWindowMaxLen] = metaDataJson
      }

      // check acks
      let i = 0
      for (; sendAcks[(i + currMsg.index) % UdpWindowMaxLen] !== undefined; i++) {
        let realIndex = (i + currMsg.index) % UdpWindowMaxLen
        sendAcks[realIndex] = undefined
        sendTimes[realIndex] = undefined
        sendBuffers[realIndex] = undefined
        currMsg.index += 1
      }
      if (currMsg.index >= currMsg.total) {
        currMsg = undefined
        sendAcks = Array(UdpWindowMaxLen)
        sendBuffers = Array(UdpWindowMaxLen)
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
let isFinishMap = {}

function ackBuf (metaBuffer, remoteInfo) {
  var buf = Buffer.alloc(metaBuffer.length + 2)
  buf[0] = HeaderUdpDataSyncAck
  buf[1] = metaBuffer.length
  for (var i = 0; i < metaBuffer.length; i++) {
    buf[i] = metaBuffer[i]
  }
  udpClient.send(buf, 0, buf.length, remoteInfo.port, remoteInfo.address)
}

function parseResult (msgKey) {
  if (receiveMap[msgKey][receiveIndexMap[msgKey].index % UdpWindowMaxLen] === undefined) return
  if (resultMap[msgKey] === undefined) {
    let msgBuf = receiveMap[msgKey][receiveIndexMap[msgKey].index % UdpWindowMaxLen]
    let baseInfoLen = bytes2Uint(msgBuf.slice(0, 4))
    let baseInfoStr = ''
    let i = 0
    let includeLast = 0
    while (receiveMap[msgKey][(receiveIndexMap[msgKey].index + i) % UdpWindowMaxLen] !== undefined) {
      let realIndex = (receiveIndexMap[msgKey].index + i) % UdpWindowMaxLen
      msgBuf = receiveMap[msgKey][realIndex]
      if (i === 0) {
        msgBuf = msgBuf.slice(4)
      }
      if (baseInfoStr.length + msgBuf.length < baseInfoLen) {
        baseInfoStr = baseInfoStr + msgBuf.toString()
        i++
        continue
      }
      if (baseInfoStr.length + msgBuf.length > baseInfoLen) {
        receiveMap[msgKey][realIndex] = msgBuf.slice(baseInfoLen - baseInfoStr.length)
        includeLast = 0
      } else {
        includeLast = 1
      }
      baseInfoStr = baseInfoStr + msgBuf.slice(0, baseInfoLen - baseInfoStr.length)
      resultMap[msgKey] = JSON.parse(baseInfoStr)
      break
    }
    if (resultMap[msgKey] !== undefined) {
      for (let j = 0; j < i + includeLast; j++) {
        receiveMap[msgKey][(receiveIndexMap[msgKey].index + j) % UdpWindowMaxLen] = undefined
      }
      receiveIndexMap[msgKey].index += i + includeLast
    }
  }
  // TODO 需要升级支持文件
}

function checkFinish (msgKey) {
  if (receiveIndexMap[msgKey].index + 1 >= receiveIndexMap[msgKey].total) {
    let msg = resultMap[msgKey]
    onReceiveMsg(msg)
    receiveMap[msgKey] = null
    resultMap[msgKey] = null
    isFinishMap[msgKey] = true
  }
}

udpClient.on('message', function (buf, remoteInfo) {
  if (buf.length < 2) return
  let metaDataLen = buf[1]
  if (buf.length < 2 + metaDataLen) return
  let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
  if (buf[0] === HeaderUdpServerSync) {
    console.log(`receive server sync from ${remoteInfo.address}:${remoteInfo.port}：${buf.slice(2, 2 + metaDataLen).toString()}`)
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
    if (metaDataJson.key === undefined || metaDataJson.key === null) return
    if (isFinishMap[metaDataJson.key] !== undefined) return
    if (receiveMap[metaDataJson.key] === undefined) {
      receiveMap[metaDataJson.key] = Array(UdpWindowMaxLen)
      receiveIndexMap[metaDataJson.key] = {
        total: metaDataJson.total,
        index: 0
      }
    }
    if (receiveIndexMap[metaDataJson.key].index + UdpWindowMaxLen <= metaDataJson.index) return
    if (receiveIndexMap[metaDataJson.key].index <= metaDataJson.index && receiveIndexMap[metaDataJson.key].index + UdpWindowMaxLen > metaDataJson.index) {
      receiveMap[metaDataJson.key][metaDataJson.index % UdpWindowMaxLen] = buf.slice(2 + metaDataLen)
      parseResult(metaDataJson.key)
      checkFinish(metaDataJson.key)
    }
    ackBuf(buf.slice(2, 2 + metaDataLen), remoteInfo)
  } else if (buf[0] === HeaderUdpDataSyncAck) {
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
    deviceNum = 0
  }
}
