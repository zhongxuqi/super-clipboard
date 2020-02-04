'use strict'

import { app, BrowserWindow, screen, clipboard, ipcMain } from 'electron'
import Consts from '../common/Consts'
import Database from './Database'
import NetUDP from './net_udp'

const MAX_LEN = 10

/**
 * Set `__static` path to static files in production
 * https://simulatedgreg.gitbooks.io/electron-vue/content/en/using-static-assets.html
 */
if (process.env.NODE_ENV !== 'development') {
  global.__static = require('path').join(__dirname, '/static').replace(/\\/g, '\\\\')
}

let mainWindow
const winURL = process.env.NODE_ENV === 'development'
  ? `http://localhost:9080`
  : `file://${__dirname}/index.html`

function createWindow () {
  /**
   * Initial window options
   */
  mainWindow = new BrowserWindow({
    titleBarStyle: 'hidden',
    height: screen.height * 0.6,
    useContentSize: true,
    width: screen.height * 0.6,
    icon: require('path').join(__dirname, 'icons', '64x64.png')
  })
  mainWindow.setMenuBarVisibility(false)

  // init ipc
  let renderChannel
  let renderInited = false
  let prevValue
  let intervalID
  let msgList
  let skipValue = ''

  function clearMsg (rows) {
    let end = 0
    if (rows.length > MAX_LEN) {
      end = rows.length - MAX_LEN
    }
    for (var i = 0; i < end; i++) {
      Database.deleteMsg(rows[i].id)
      if (renderChannel !== undefined) renderChannel.send('clipboard-message-delete', rows[i])
    }
    return rows.slice(end)
  }

  Database.listAll(function (rows) {
    msgList = clearMsg(rows)
    if (renderChannel !== undefined && renderInited === false) {
      renderInited = true
      for (var i = 0; i < msgList.length; i++) {
        renderChannel.send('clipboard-message-add', msgList[i])
      }
    }

    if (msgList.length > 0) prevValue = msgList[msgList.length - 1].content
    intervalID = setInterval(function () {
      const newValue = clipboard.readText()
      if (newValue === '') return
      if (prevValue !== newValue && skipValue !== newValue) {
        skipValue = ''
        prevValue = newValue
        let now = Date.now()
        let msg = {
          type: Consts.MessageType.Text,
          content: newValue,
          extra: '',
          create_time: now,
          update_time: now
        }
        Database.insert(msg, function (dbMsg) {
          if (renderChannel !== undefined) {
            renderChannel.send('clipboard-message-add', dbMsg)
          }
          msgList = clearMsg([...msgList, dbMsg])
        })
      }
    }, 500)
  })

  ipcMain.on('clipboard-message-connect', (event, arg) => {
    renderChannel = event.sender
    renderInited = false
    if (msgList !== undefined) {
      renderInited = true
      for (var i = 0; i < msgList.length; i++) {
        renderChannel.send('clipboard-message-add', msgList[i])
      }
    }
  })

  ipcMain.on('clipboard-message-action-delete', (event, arg) => {
    Database.deleteMsg(arg.id)
    if (renderChannel !== undefined) renderChannel.send('clipboard-message-delete', arg)
    msgList = msgList.filter(function (item) {
      return item.id !== arg.id
    })
  })

  ipcMain.on('clipboard-message-action-copy', (event, arg) => {
    skipValue = arg
    clipboard.writeText(arg)
  })

  let deviceNum = 0
  let receiveMaxLen = 1000
  let receiveIndexMap = {}
  let receiveMap = {}
  let resultMap = {}

  function ackMsg (metaBuffer, remoteInfo) {
    var buf = Buffer.alloc(metaBuffer.length + 2)
    buf[0] = NetUDP.HeaderUdpDataSyncAck
    buf[1] = metaBuffer.length
    for (var i = 0; i < metaBuffer.length; i++) {
      buf[i] = metaBuffer[i]
    }
    NetUDP.sendMsg(buf, remoteInfo)
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
      Database.insert(msg, function (dbMsg) {
        if (renderChannel !== undefined) {
          renderChannel.send('clipboard-message-add', dbMsg)
        }
        msgList = clearMsg([...msgList, dbMsg])
      })
      receiveMap[key] = null
      resultMap[key] = null
    }
  }

  NetUDP.listenMessage(function (msg, remoteInfo) {
    if (msg[0] === NetUDP.HeaderUdpServerSync) {
      let metaDataLen = msg[1]
      console.log(`receive message from ${remoteInfo.address}:${remoteInfo.port}：${msg.slice(2, 2 + metaDataLen).toString()}`)
      let metaDataJson = JSON.parse(msg.slice(2, 2 + metaDataLen).toString())
      let newdeviceNum = 1
      if (metaDataJson.udp_addrs !== undefined && metaDataJson.udp_addrs !== null) {
        newdeviceNum += metaDataJson.udp_addrs.length
      }
      if (deviceNum !== newdeviceNum) {
        deviceNum = newdeviceNum
        renderChannel.send('clipboard-sync-state-device', {deviceNum: deviceNum})
      }
    } else if (msg[0] === NetUDP.HeaderUdpDataSync) {
      let metaDataLen = msg[1]
      let metaDataJson = JSON.parse(msg.slice(2, 2 + metaDataLen).toString())
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
        receiveMap[metaDataJson.key][(metaDataJson.index - receiveIndexMap[metaDataJson.key].index + receiveIndexMap[metaDataJson.key].offset) % receiveMaxLen] = msg.slice(2 + metaDataLen)
        parseResult(metaDataJson.key)
        checkFinish(metaDataJson.key)
      }
      ackMsg(msg.slice(2, 2 + metaDataLen), remoteInfo)
    }
  })

  ipcMain.on('clipboard-sync-state', (event, arg) => {
    event.returnValue = NetUDP.isStart()
  })

  ipcMain.on('clipboard-sync-state-toggle', (event, arg) => {
    if (arg.state) {
      NetUDP.start()
    } else {
      NetUDP.close()
    }
    event.returnValue = NetUDP.isStart()
  })

  mainWindow.loadURL(winURL)
  mainWindow.on('closed', () => {
    mainWindow = null
    if (intervalID !== undefined) clearInterval(intervalID)
    Database.close()
  })
}

app.on('ready', createWindow)

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (mainWindow === null) {
    createWindow()
  }
})

/**
 * Auto Updater
 *
 * Uncomment the following code below and install `electron-updater` to
 * support auto updating. Code Signing with a valid certificate is required.
 * https://simulatedgreg.gitbooks.io/electron-vue/content/en/using-electron-builder.html#auto-updating
 */

/*
import { autoUpdater } from 'electron-updater'

autoUpdater.on('update-downloaded', () => {
  autoUpdater.quitAndInstall()
})

app.on('ready', () => {
  if (process.env.NODE_ENV === 'production') autoUpdater.checkForUpdates()
})
 */
