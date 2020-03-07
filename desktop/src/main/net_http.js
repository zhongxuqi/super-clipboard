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
  },
  register: function (params, callback) {
    const request = net.request({
      method: 'POST',
      url: `${Consts.Host}/openapi/account/register`
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
    request.write(JSON.stringify(params))
    request.end()
  },
  login: function (params, callback) {
    const request = net.request({
      method: 'POST',
      url: `${Consts.Host}/openapi/account/login`
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
    request.write(JSON.stringify(params))
    request.end()
  },
  changePassword: function (params, callback) {
    const request = net.request({
      method: 'POST',
      url: `${Consts.Host}/openapi/account/change_password`
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
    request.write(JSON.stringify(params))
    request.end()
  },
  feedback: function (params, callback) {
    const request = net.request({
      method: 'POST',
      url: `${Consts.Host}/openapi/codeutils`
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
    request.write(JSON.stringify(params))
    request.end()
  }
}
