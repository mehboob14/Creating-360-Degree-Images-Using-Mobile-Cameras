package com.example.view360.enums

sealed class MsgStatus(var desc: String = "") {
    class Sent(desciption: String = "Sent.") : MsgStatus(desciption)
    class Failed(desciption: String = "Sent.") : MsgStatus(desciption)
    class InProgress(desciption: String = "Sent.") : MsgStatus(desciption)
}
