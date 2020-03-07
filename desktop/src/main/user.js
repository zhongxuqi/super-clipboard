import Store from 'electron-store'

const StoreUserIDKey = 'user_id'
let schema = {}
schema[StoreUserIDKey] = {
  type: 'string',
  default: ''
}
let storeIns = new Store({schema})
storeIns.set(StoreUserIDKey, '')

export default {
  getUserID: function () {
    return storeIns.get(StoreUserIDKey)
  },
  setUserID: function (userID) {
    return storeIns.set(StoreUserIDKey, userID)
  }
}
