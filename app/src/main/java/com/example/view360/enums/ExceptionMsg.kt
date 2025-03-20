package com.example.view360.enums

enum class ExceptionMsg(val message: String) {
    SocketClose("Socket was closed"),
    ConnectionFailed("Socket is not connected.")
}