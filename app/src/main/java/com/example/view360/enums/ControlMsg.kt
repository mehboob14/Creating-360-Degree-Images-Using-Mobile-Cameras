package com.example.view360.enums


enum class ControlMsg(val message: String) {
    ACK("CONTROL_ACK"),
    NAK("CONTROL_NAK"),
    EOF("CONTROL_EOF"),
    FIN("CONTROL_FIN"),
    ERROR("CONTROL_ERROR");

    companion object {
        fun fromMessage(msg: String): ControlMsg {
            return entries.find { it.message == msg } ?: ERROR
        }
    }
}