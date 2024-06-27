package com.carlos

import java.util.concurrent.atomic.AtomicBoolean

class Spinner(private val message: String) {
    private val isRunning: AtomicBoolean = AtomicBoolean(false)

    fun start() {
        isRunning.set(true)
        Thread {
            val spinner = arrayOf("|", "/", "-", "\\")
            var i = 0
            while (isRunning.get()) {
                print("\r$message ${spinner[i++ % spinner.size]}")
                try {
                    Thread.sleep(200)
                } catch(e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            print("\r")
        }.start()
    }
    fun stop() {
        isRunning.set(false)
    }
}