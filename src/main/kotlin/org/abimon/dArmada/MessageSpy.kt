package org.abimon.dArmada

import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Spy
import org.abimon.imperator.handle.Watchtower
import org.abimon.imperator.impl.InstanceWatchtower
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent
import java.util.*

class MessageSpy: Spy {
//    val watchtower: Set<Watchtower> = Collections.singleton(InstanceWatchtower<DiscordOrder>("Message Events", message@{ order ->
//        when(order.event) {
//            is MessageReceivedEvent -> return@message !order.event.channel.isPrivate
//            is MessageUpdateEvent -> return@message !order.event.channel.isPrivate
//            else -> return@message false
//        }
//    }))

    val watchtower: Set<Watchtower> = Collections.singleton(InstanceWatchtower<DiscordOrder>("Message Events", message@ { order -> order.event is MessageReceivedEvent || order.event is MessageUpdateEvent }))

    override fun fiddle(order: Order): Order {
//        val msg = MessageOrder((order as DiscordOrder).event)
//        if(msg.author.isBot)
//            return order
//        return msg

        val event = (order as DiscordOrder).event
        var msg: MessageOrder? = null
        when(event) {
            is MessageReceivedEvent -> {
                if(event.channel.isPrivate || event.guild == null)
                    msg = PrivateMessageOrder(event)
                else
                    msg = ServerMessageOrder(event)
            }
            is MessageUpdateEvent -> {
                if(event.channel.isPrivate || event.guild == null)
                    msg = PrivateMessageOrder(event)
                else
                    msg = ServerMessageOrder(event)
            }
        }

        if(msg == null || msg.author.isBot)
            return order
        return msg
    }

    override fun getName(): String = "That message's a spy!"

    override fun getWatchtowers(): Collection<Watchtower> = watchtower
}