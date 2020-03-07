// const Domain = '127.0.0.1'
// const Host = `http://${Domain}:8000`

const Domain = 'www.superclipboard.online'
const Host = `https://${Domain}`

export default {
  Domain: Domain,
  MessageType: {
    Unknow: 0,
    Text: 1,
    Image: 2
  },
  Host: Host,
  AppID: 4
}
