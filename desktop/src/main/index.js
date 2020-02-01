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
