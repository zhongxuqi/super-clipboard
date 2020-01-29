'use strict'

import { app, BrowserWindow, screen, clipboard, ipcMain } from 'electron'
import Consts from '../common/Consts'

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
  console.log(require('path').join(__dirname, 'static', 'app_icon.png'))

  mainWindow = new BrowserWindow({
    height: screen.height * 0.6,
    useContentSize: true,
    width: screen.height * 0.6,
    icon: require('path').join(__dirname, 'app_icon.png')
  })
  mainWindow.setMenuBarVisibility(false)

  mainWindow.loadURL(winURL)

  let index = 0
  let renderChannel
  let prevValue = ''
  let intervalID = setInterval(function () {
    const newValue = clipboard.readText()
    if (prevValue !== newValue) {
      prevValue = newValue
      if (renderChannel !== undefined) {
        renderChannel.send('clipboard-message-add', {
          id: ++index,
          type: Consts.MessageType.Text,
          content: newValue
        })
      }
    }
  }, 500)

  ipcMain.on('clipboard-message-connect', (event, arg) => {
    renderChannel = event.sender
  })

  mainWindow.on('closed', () => {
    mainWindow = null
    clearInterval(intervalID)
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
