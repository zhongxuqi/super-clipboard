'use strict'

import { app, BrowserWindow, screen, clipboard, ipcMain, Notification } from 'electron'
import Consts from '../common/Consts'
import Database from './Database'
import NetUDP from './net_udp'
import NetHttp from './net_http'
import Language from './language'
import Store from 'electron-store'

const storeIns = new Store({
  user_id: {
    type: 'string'
  }
})
storeIns.get('user_id')

const MAX_LEN = 100

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
  Language.setLanguage(app.getLocale())

  let notifyMsg
  function sendNotification (title, body) {
    if (notifyMsg !== undefined) notifyMsg.close()
    notifyMsg = new Notification({
      title: title,
      body: body
    })
    notifyMsg.show()
  }

  // init ipc
  let renderChannel
  let renderInited = false
  let intervalID
  let msgList = []
  let preValue = clipboard.readText()
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

  function upsertMsg (msg, callback) {
    for (let i = msgList.length - 1; i >= 0; i--) {
      if (msg.content !== msgList[i].content) continue
      msg = msgList[i]
      msg.update_time = Date.now()
      Database.update(msg, function (dbMsg) {
        if (renderChannel !== undefined) {
          renderChannel.send('clipboard-message-delete', dbMsg)
          renderChannel.send('clipboard-message-add', dbMsg)
        }
        msgList.splice(i, 1)
        msgList = clearMsg([...msgList, dbMsg])
        if (typeof callback === 'function') callback(dbMsg)
        NetUDP.sendClipboardMsg(JSON.parse(JSON.stringify(dbMsg)))
      })
      return
    }
    Database.insert(msg, function (dbMsg) {
      if (renderChannel !== undefined) {
        renderChannel.send('clipboard-message-add', dbMsg)
      }
      msgList = clearMsg([...msgList, dbMsg])
      if (typeof callback === 'function') callback(dbMsg)
      NetUDP.sendClipboardMsg(JSON.parse(JSON.stringify(dbMsg)))
    })
  }

  Database.listAll(function (rows) {
    msgList = clearMsg(rows)
    if (renderChannel !== undefined && renderInited === false) {
      renderInited = true
      for (var i = 0; i < msgList.length; i++) {
        renderChannel.send('clipboard-message-add', msgList[i])
      }
    }

    intervalID = setInterval(function () {
      const newValue = clipboard.readText()
      if (newValue === '') return
      if (preValue !== newValue && skipValue !== newValue) {
        preValue = newValue
        skipValue = ''
        let now = Date.now()
        let msg = {
          type: Consts.MessageType.Text,
          content: newValue,
          extra: '',
          create_time: now,
          update_time: now
        }
        upsertMsg(msg, function (dbMsg) {})
      }
    }, 500)
  })

  ipcMain.on('clipboard-message-connect', (event, arg) => {
    renderChannel = event.sender
    renderInited = false
    if (msgList !== undefined) {
      renderInited = true
      try {
        for (var i = 0; i < msgList.length; i++) {
          renderChannel.send('clipboard-message-add', msgList[i])
        }
      } catch (e) {

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

  ipcMain.on('clipboard-message-action-sync', (event, arg) => {
    NetUDP.sendClipboardMsg(JSON.parse(JSON.stringify(arg)))
  })

  NetUDP.setOnChangeDeviceNum(function (deviceNum) {
    renderChannel.send('clipboard-sync-state-device', {deviceNum: deviceNum})
  })

  NetUDP.setOnReceiveMsg(function (msg) {
    upsertMsg(msg, function (dbMsg) {
      skipValue = dbMsg.content
      clipboard.writeText(dbMsg.content)
      sendNotification(Language.getLanguageText('receive_clipboard_content'), dbMsg.content)
    })
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
    renderChannel.send('clipboard-sync-state-sync', {state: arg.state})
  })

  // 网络请求
  ipcMain.on('request-get_captcha_id', (event) => {
    NetHttp.getCaptchaID(function (resp) {
      renderChannel.send('response-get_captcha_id', resp)
    })
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
