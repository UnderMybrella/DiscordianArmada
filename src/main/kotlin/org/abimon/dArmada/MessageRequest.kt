package org.abimon.dArmada

import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

class MessageRequest(val content: String, val channel: IChannel, val embed: EmbedObject? = null) : RequestBuffer.IRequest<IMessage> {
    override fun request(): IMessage = channel.sendMessage(content, embed, false)
}