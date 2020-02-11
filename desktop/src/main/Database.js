var sqlite3 = require('sqlite3').verbose()

const TABLE_NAME = 'clipboard_history'

var db = new sqlite3.Database('superclipboard.db')
db.run('create table if not exists ' + TABLE_NAME + ' (id INTEGER PRIMARY KEY AUTOINCREMENT, type INT NOT NULL, content TEXT NOT NULL, extra TEXT NOT NULL, create_time BIGINT NOT NULL, update_time BIGINT NOT NULL)')

function deleteMsg (id) {
  db.run('delete from ' + TABLE_NAME + ' where id = ' + id)
}

export default {
  insert: function (clipboardMsg, callback) {
    db.run('insert into ' + TABLE_NAME + ' (type,content,extra,create_time,update_time) values (?,?,?,?,?)', [clipboardMsg.type, clipboardMsg.content, clipboardMsg.extra, clipboardMsg.create_time, clipboardMsg.update_time], function (res, err) {
      db.all('SELECT id,type,content,extra,create_time,update_time FROM ' + TABLE_NAME + ' order by update_time DESC limit 1', function (err, rows) {
        if (err !== undefined && err != null) {
          console.log(err)
          return
        }
        if (typeof callback === 'function') callback(rows[0])
      })
    })
  },
  update: function (clipboardMsg, callback) {
    db.run(`update ${TABLE_NAME} set update_time=? where id=?`, [clipboardMsg.update_time, clipboardMsg.id], function (res, err) {
      db.all('SELECT id,type,content,extra,create_time,update_time FROM ' + TABLE_NAME + ' order by update_time DESC limit 1', function (err, rows) {
        if (err !== undefined && err != null) {
          console.log(err)
          return
        }
        if (typeof callback === 'function') callback(rows[0])
      })
    })
  },
  listAll: function (callback) {
    db.all('SELECT id,type,content,extra,create_time,update_time FROM ' + TABLE_NAME + ' order by update_time ASC', function (err, rows) {
      if (err !== undefined && err != null) {
        console.log(err)
        return
      }
      callback(rows)
    })
  },
  getLast: function (callback) {
    db.all('SELECT id,type,content,extra,create_time,update_time FROM ' + TABLE_NAME + ' order by update_time DESC limit 1', function (err, rows) {
      if (err !== undefined && err != null) {
        console.log(err)
        return
      }
      if (rows.length > 0) {
        callback(rows[0])
      } else {
        callback()
      }
    })
  },
  deleteMsg: deleteMsg,
  close: function () {
    db.close()
  }
}
