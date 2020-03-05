import dgram from 'dgram'
import os from 'os'
import md5 from 'js-md5'

const UdpAddrSeparator = ','
let localUdpAddrsJoin = ''
let udpClient = dgram.createSocket('udp4')
let localPort = 0
udpClient.on('listening', function () {
  let address = udpClient.address()
  localPort = address.port
})

udpClient.on('error', function (e) {
  console.log(e)
})

function sendUdpPackage (buffer, offset, length, port, ip) {
  try {
    udpClient.send(buffer, offset, length, port, ip)
  } catch (e) {
    console.log(e)
  }
}

const ServerHost = 'www.easypass.tech'
// const ServerHost = '192.168.100.107'

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
    num = num * 256 + bytes[i]
    if (bytes[i] < 0) num += 256
  }
  return num
}

const HeaderUdpServerSync = 0x00
const HeaderUdpClientSync = 0x01
const HeaderUdpDataSync = 0x02
const HeaderUdpDataSyncAck = 0x03

const UdpWindowMaxLen = 1000

// 回调函数
let onChangeDeviceNum
let onReceiveMsg

// 内容发送
const syncWorkerMap = {}
const sendBufferMaxLen = 400
const SyncWorkerTimeout = 3000
function createSyncWorker (remoteAddr) {
  let udpClientSyncKey = `${Date.now()}`
  let remoteAddrs = []
  let remoteAddrList = remoteAddr.split(UdpAddrSeparator)
  for (let i = 0; i < remoteAddrList.length; i++) {
    let addrItems = remoteAddrList[i].split(':')
    if (addrItems.length < 2) continue
    remoteAddrs.push({
      ip: addrItems[0],
      port: parseInt(addrItems[1])
    })
  }
  let activeRemoteAddr = {}
  let isRunning = true
  let lastSyncTime = 0

  let sendAcks = Array(UdpWindowMaxLen)
  let sendTimes = Array(UdpWindowMaxLen)
  let sendRetryTime = 3000
  let sendBuffers = Array(UdpWindowMaxLen)
  let clipboardMsgs = []
  let currMsg

  function sendData () {
    // console.log(`${lastSyncTime} ${SyncWorkerTimeout} ${Date.now()}`)
    if (lastSyncTime + SyncWorkerTimeout < Date.now() || (currMsg === undefined && clipboardMsgs.length <= 0)) {
      let metaData = str2UTF8(JSON.stringify({
        key: udpClientSyncKey
      }))
      let buffer = Buffer.alloc(2 + metaData.length)
      buffer[0] = HeaderUdpClientSync
      buffer[1] = metaData.length
      for (let i = 0; i < metaData.length; i++) {
        buffer[2 + i] = metaData[i]
      }
      for (let i = 0; i < remoteAddrs.length; i++) {
        let remoteAddrItem = remoteAddrs[i]
        sendUdpPackage(buffer, 0, buffer.length, remoteAddrItem.port, remoteAddrItem.ip)
      }
      return
    } else if (currMsg === undefined && clipboardMsgs.length > 0) {
      currMsg = {
        originMsg: clipboardMsgs.pop()
      }
      currMsg.originMsg.create_time = undefined
      currMsg.originMsg.update_time = undefined
      currMsg.baseInfoBuffer = Buffer.from(JSON.stringify(currMsg.originMsg), 'utf-8')
      currMsg.key = md5(currMsg.baseInfoBuffer.toString())
      currMsg.total = Math.ceil(currMsg.baseInfoBuffer.length / sendBufferMaxLen)
      currMsg.index = 0
    }
    let i = 0
    for (; sendBuffers[(i + currMsg.index) % UdpWindowMaxLen] !== undefined && i < UdpWindowMaxLen; i++) {
      let realIndex = (i + currMsg.index) % UdpWindowMaxLen
      if (sendAcks[realIndex] === undefined && sendTimes[realIndex] + sendRetryTime < Date.now()) {
        // console.log(`${sendBuffers[realIndex].toString()} ${activeRemoteAddr.port} ${activeRemoteAddr.ip}`)
        sendUdpPackage(sendBuffers[realIndex], 0, sendBuffers[realIndex].length, activeRemoteAddr.port, activeRemoteAddr.ip)
        sendTimes[realIndex] = Date.now()
      }
    }
    for (; (i + currMsg.index) < UdpWindowMaxLen && (i + currMsg.index) < currMsg.total; i++) {
      if ((i + currMsg.index) * sendBufferMaxLen < currMsg.baseInfoBuffer.length) {
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
        if (currMsg.baseInfoBuffer.length > (metaDataJson.index + 1) * sendBufferMaxLen) {
          bufferLen = sendBufferMaxLen
        } else {
          bufferLen = currMsg.baseInfoBuffer.length - metaDataJson.index * sendBufferMaxLen
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
          buffer[2 + metaData.length + isFirst + j] = currMsg.baseInfoBuffer[metaDataJson.index * sendBufferMaxLen + j]
        }
        sendBuffers[realIndex] = buffer
        sendTimes[realIndex] = Date.now()
        // console.log(`${buffer.toString()} ${port} ${ip}`)

        sendUdpPackage(buffer, 0, buffer.length, activeRemoteAddr.port, activeRemoteAddr.ip)
      }
    }
    // TODO 需要升级支持文件
  }

  sendData()
  let intervalID = setInterval(function () {
    if (!isRunning) {
      clearInterval(intervalID)
      return
    }
    sendData()
  }, 500)
  return {
    close: function () {
      isRunning = false
    },
    isActive: function () {
      return lastSyncTime + SyncWorkerTimeout > Date.now()
    },
    sendClipboardMsg: function (msg) {
      msg.id = undefined
      msg.create_time = undefined
      msg.update_time = undefined
      clipboardMsgs.splice(0, 0, msg)
    },
    feed: function (metaDataJson, syncUdpAddr) {
      // if (metaDataJson.key !== udpClientSyncKey) return
      if (remoteAddr.search(syncUdpAddr) < 0) return
      let addrItems = syncUdpAddr.split(':')
      activeRemoteAddr = {
        ip: addrItems[0],
        port: parseInt(addrItems[1])
      }
      lastSyncTime = Date.now()
    },
    ack: function (metaDataJson) {
      lastSyncTime = Date.now()
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
        sendTimes = Array(UdpWindowMaxLen)
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
    if (syncWorkerMap[addr] === undefined) continue
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
let receiveIndexMap = {}
let receiveMap = {}
let resultMap = {}
let isFinishMap = {}
function clearKeyTask (msgKey) {
  return function () {
    receiveIndexMap[msgKey] = undefined
    receiveMap[msgKey] = undefined
    resultMap[msgKey] = undefined
    isFinishMap[msgKey] = undefined
  }
}

function ackBuf (metaBuffer, remoteInfo) {
  var buf = Buffer.alloc(metaBuffer.length + 2)
  buf[0] = HeaderUdpDataSyncAck
  buf[1] = metaBuffer.length
  for (var i = 0; i < metaBuffer.length; i++) {
    buf[i + 2] = metaBuffer[i]
  }
  sendUdpPackage(buf, 0, buf.length, remoteInfo.port, remoteInfo.address)
}

function parseResult (msgKey) {
  if (receiveMap[msgKey][receiveIndexMap[msgKey].index % UdpWindowMaxLen] === undefined) return
  if (resultMap[msgKey] === undefined) {
    let msgBuf = receiveMap[msgKey][receiveIndexMap[msgKey].index % UdpWindowMaxLen]
    let baseInfoLen = bytes2Uint(msgBuf.slice(0, 4))
    // console.log('===>>> baseInfoLen', `${baseInfoLen}`)
    let baseInfoByte = []
    let i = 0
    let includeLast = 0
    while (receiveMap[msgKey][(receiveIndexMap[msgKey].index + i) % UdpWindowMaxLen] !== undefined) {
      let realIndex = (receiveIndexMap[msgKey].index + i) % UdpWindowMaxLen
      msgBuf = receiveMap[msgKey][realIndex]
      if (i === 0) {
        msgBuf = msgBuf.slice(4)
      }
      // console.log('===>>>', msgBuf.slice(0, 10).toString(), realIndex)
      if (baseInfoByte.length + msgBuf.length < baseInfoLen) {
        for (let j = 0; j < msgBuf.length; j++) {
          baseInfoByte.push(msgBuf[j])
        }
        i++
        continue
      }
      if (baseInfoByte.length + msgBuf.length > baseInfoLen) {
        receiveMap[msgKey][realIndex] = msgBuf.slice(baseInfoLen - baseInfoByte.length).toString()
        includeLast = 0
      } else {
        includeLast = 1
      }
      let validMsgBuf = msgBuf.slice(0, baseInfoLen - baseInfoByte.length)
      for (let j = 0; j < validMsgBuf.length; j++) {
        baseInfoByte.push(validMsgBuf[j])
      }
      // console.log('===>>> baseInfoByte.length', `${baseInfoByte.length}`)
      try {
        let baseInfoStr = Buffer.from(baseInfoByte).toString()
        // console.log('===>>>', baseInfoStr)
        resultMap[msgKey] = JSON.parse(baseInfoStr)
      } catch (e) {
        console.log(e)
      }
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
  if (resultMap[msgKey] === undefined || resultMap[msgKey] == null) return
  if (receiveIndexMap[msgKey].index >= receiveIndexMap[msgKey].total) {
    let msg = resultMap[msgKey]
    let now = Date.now()
    msg.create_time = now
    msg.update_time = now
    onReceiveMsg(msg)
    receiveMap[msgKey] = null
    isFinishMap[msgKey] = true
    setTimeout(clearKeyTask(msgKey), 3 * 60 * 1000)
  }
}

udpClient.on('message', function (buf, remoteInfo) {
  if (buf.length < 2) return
  let metaDataLen = buf[1]
  if (buf.length < 2 + metaDataLen) return
  let metaDataJson = JSON.parse(buf.slice(2, 2 + metaDataLen).toString())
  if (buf[0] === HeaderUdpServerSync) {
    // console.log(`receive server sync from ${remoteInfo.address}:${remoteInfo.port}：${buf.slice(2, 2 + metaDataLen).toString()}`)
    let validUdpAddrs = []
    if (metaDataJson.udp_addrs !== undefined && metaDataJson.udp_addrs != null) {
      for (let i = 0; i < metaDataJson.udp_addrs.length; i++) {
        if (localUdpAddrsJoin === metaDataJson.udp_addrs[i]) continue
        validUdpAddrs.push(metaDataJson.udp_addrs[i])
      }
    }
    let deviceNum = 0
    for (let udpAddrkey in syncWorkerMap) {
      if (syncWorkerMap[udpAddrkey] === undefined) continue
      if (syncWorkerMap[udpAddrkey] !== undefined && syncWorkerMap[udpAddrkey].isActive()) {
        deviceNum++
      }
    }
    if (typeof onChangeDeviceNum === 'function') onChangeDeviceNum(deviceNum)
    refreshSyncWork(validUdpAddrs)
  } else if (buf[0] === HeaderUdpClientSync) {
    // console.log(`${buf.toString()}`)
    let syncUdpAddr = `${remoteInfo.address}:${remoteInfo.port}`
    for (let udpAddrkey in syncWorkerMap) {
      if (syncWorkerMap[udpAddrkey] === undefined) continue
      if (udpAddrkey.search(syncUdpAddr) < 0) continue
      syncWorkerMap[udpAddrkey].feed(metaDataJson, syncUdpAddr)
    }
  } else if (buf[0] === HeaderUdpDataSync) {
    // console.log(`${buf.toString()}`)
    if (metaDataJson.key === undefined || metaDataJson.key === null) return
    if (isFinishMap[metaDataJson.key] !== undefined) {
      ackBuf(buf.slice(2, 2 + metaDataLen), remoteInfo)
      if (metaDataJson.index === 0 && resultMap[metaDataJson.key] !== undefined) {
        onReceiveMsg(resultMap[metaDataJson.key])
      }
      return
    }
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
    let currUdpAddr = `${remoteInfo.address}:${remoteInfo.port}`
    for (let udpAddrkey in syncWorkerMap) {
      if (syncWorkerMap[udpAddrkey] === undefined) continue
      if (udpAddrkey.search(currUdpAddr) < 0) continue
      syncWorkerMap[udpAddrkey].ack(metaDataJson, currUdpAddr)
    }
  }
})

let heartBeatIntervalID
function heartBeat () {
  let ifaces = os.networkInterfaces()
  let localUdpAddrs = []
  for (let dev in ifaces) {
    for (let i = 0; i < ifaces[dev].length; i++) {
      if (ifaces[dev][i].family !== 'IPv4' || ifaces[dev][i].address === '127.0.0.1') continue
      localUdpAddrs.push(`${ifaces[dev][i].address}:${localPort}`)
    }
  }
  localUdpAddrs.sort()
  localUdpAddrsJoin = localUdpAddrs.join(UdpAddrSeparator)
  let udpAddrs = []
  if (localUdpAddrsJoin !== '') udpAddrs.push(localUdpAddrsJoin)
  var metaData = str2UTF8(JSON.stringify({
    app_id: 'superclipboard',
    udp_addrs: udpAddrs
  }))
  var buffer = Buffer.alloc(metaData.length + 2)
  buffer[0] = HeaderUdpServerSync
  buffer[1] = metaData.length
  for (var i = 0; i < metaData.length; i++) {
    buffer[2 + i] = metaData[i]
  }
  sendUdpPackage(buffer, 0, buffer.length, 9000, ServerHost)
}
export default {
  isStart: function () {
    return heartBeatIntervalID !== undefined
  },
  start: function () {
    if (heartBeatIntervalID !== undefined) return
    heartBeat()
    heartBeatIntervalID = setInterval(heartBeat, 2000)
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
      if (syncWorkerMap[addr] === undefined) continue
      syncWorkerMap[addr].sendClipboardMsg(msg)
    }
  },
  close: function () {
    if (heartBeatIntervalID === undefined) return
    for (let addr in syncWorkerMap) {
      if (syncWorkerMap[addr] === undefined) continue
      syncWorkerMap[addr].close()
      syncWorkerMap[addr] = undefined
    }
    clearInterval(heartBeatIntervalID)
    heartBeatIntervalID = undefined
  }
}
