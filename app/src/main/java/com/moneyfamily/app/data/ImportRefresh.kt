package com.moneyfamily.app.data

object ImportRefresh {
    @Volatile private var requested = false
    fun request() { requested = true }
    fun consume(): Boolean = if (requested) { requested = false; true } else false
}
