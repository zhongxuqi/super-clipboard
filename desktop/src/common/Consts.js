// const Domain = 'superclipboard.online'
const Domain = '127.0.0.1'

export default {
  Domain: Domain,
  MessageType: {
    Unknow: 0,
    Text: 1,
    Image: 2
  },
  // Host: `https://${Domain}:8000`,
  Host: `http://${Domain}:8000`,
  AppID: 4
}
