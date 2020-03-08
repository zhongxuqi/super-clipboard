import Store from 'electron-store'

const StoreUserIDKey = 'user_id'
let schema = {}
schema[StoreUserIDKey] = {
  type: 'string',
  default: ''
}

const StoreClipboardMsgsKey = 'clipboard_msgs'

schema[StoreClipboardMsgsKey] = {
  type: 'string',
  default: ''
}

let storeIns = new Store({schema})
// storeIns.set(StoreUserIDKey, '')

let idIndex = 0
let clipboardMsgs = []

try {
  clipboardMsgs = JSON.parse(storeIns.get(StoreClipboardMsgsKey))
  for (let i = 0; i < clipboardMsgs.length; i++) {
    if (clipboardMsgs[i].id >= idIndex) {
      idIndex = clipboardMsgs[i].id + 1
    }
  }
} catch (e) {
  clipboardMsgs = []
}

export default {
  getUserID: function () {
    return storeIns.get(StoreUserIDKey)
  },
  setUserID: function (userID) {
    return storeIns.set(StoreUserIDKey, userID)
  },
  listAll: function (callback) {
    callback(clipboardMsgs)
  },
  insert: function (clipboardMsg, callback) {
    clipboardMsg.id = idIndex
    idIndex++
    callback(clipboardMsg)
  }
}
