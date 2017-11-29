package org.abimon.dArmada

import org.abimon.visi.lang.splitOutsideGroup
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.Event
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser

class PrivateMessageOrder(event: Event): DiscordOrder(event), MessageOrder {
    override val msg: IMessage
    override val channel: IChannel
    override val author: IUser
    override val client: IDiscordClient = event.client

    override var prefix: String = "-"
    override var params: Array<String> = arrayOf() //Made a var so that it *can* be modified by things like the MikuCommand in v4
    override var languageManager: (MessageOrder, MessageRequest) -> MessageRequest = { _, request -> request }

    override val content: String
        get() = if(msg.content == null) "" else if(msg.content.startsWith(prefix)) msg.content.substring(prefix.length) else msg.content

    init {
        when(event) {
            is MessageReceivedEvent -> msg = event.message
            is MessageUpdateEvent -> msg = event.newMessage
            else -> throw IllegalArgumentException("$event is not a MessageReceived Event")
        }

        channel = msg.channel
        author = msg.author
        params = getParams()
    }

    fun getParams(paramSplitter: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Array<String> = paramSplitter(content)
}