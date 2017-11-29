package org.abimon.dArmada

import org.abimon.imperator.handle.Order
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser

interface MessageOrder: Order {
    val msg: IMessage
    val channel: IChannel
    val author: IUser
    val client: IDiscordClient

    var params: Array<String>
    var prefix: String
    var languageManager: (MessageOrder, MessageRequest) -> MessageRequest

    val content: String
}