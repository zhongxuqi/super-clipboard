import { net } from 'electron'
import Consts from '../common/Consts'

export default {
  Host: Consts.Host,
  getCaptchaID: function (callback) {
    const request = net.request({
      method: 'GET',
      url: `${Consts.Host}/openapi/captcha/new`
    })
    request.on('response', (response) => {
      response.on('data', (chunk) => {
        let resp = JSON.parse(chunk)
        if (typeof callback === 'function') callback(resp)
      })
      response.on('error', () => {

      })
    })
    request.on('error', () => {

    })
    request.end()
  }
}
