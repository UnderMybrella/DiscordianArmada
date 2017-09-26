package org.abimon.dArmada

import org.abimon.imperator.handle.*
import org.abimon.imperator.impl.BaseOrder
import org.abimon.imperator.impl.BasicWatchtower
import org.abimon.imperator.impl.InstanceWatchtower
import org.abimon.visi.lang.splitOutsideGroup
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.Event
import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.RequestBuffer
import java.util.*

open class DiscordScout: Scout, IListener<Event> {
    private var imperator: Optional<Imperator> = Optional.empty()

    override fun handle(event: Event) {
        if(imperator.isPresent)
            imperator.get().dispatch(DiscordOrder(event, this))
    }

    override fun addAnnouncements(order: Order) {}

    override fun getName(): String = "Discord Scout"

    override fun setImperator(imperator: Imperator) {
        this.imperator = Optional.of(imperator)
    }
}

open class DiscordOrder(val event: Event, origin: Scout? = null): BaseOrder("Discord Order", origin)

open class DiscordCommand<T: Event>(val eventClass: Class<T>, private val watchtowers: ArrayList<Watchtower> = arrayListOf(), val command: (T) -> Unit): Soldier {
    companion object {
        inline operator fun <reified T : Event> invoke(additionalWatchtowers: Collection<Watchtower> = arrayListOf(), noinline command: (T) -> Unit) = DiscordCommand(T::class.java, ArrayList(additionalWatchtowers), command)
        inline operator fun <reified T : Event> invoke(additionalWatchtower: Watchtower, noinline command: (T) -> Unit) = DiscordCommand(T::class.java, arrayListOf(additionalWatchtower), command)
    }

    @Suppress("UNCHECKED_CAST")
    override fun command(order: Order) = command.invoke((order as DiscordOrder).event as T)

    override fun getName(): String = ""

    override fun getWatchtowers(): Collection<Watchtower> = watchtowers

    init {
        watchtowers.add(BasicWatchtower("Watching out for certain events", { order -> order is DiscordOrder && eventClass.isInstance(order.event) }))
    }
}

open class DiscordWatchtower<T: Event>(val eventClass: Class<T>, name: String = "", val regulations: (T) -> Boolean): InstanceWatchtower<DiscordOrder>(DiscordOrder::class.java, name) {
    companion object {
        inline operator fun <reified T: Event> invoke(name: String = "", noinline regulations: (T) -> Boolean) = DiscordWatchtower(T::class.java, name, regulations)
    }

    @Suppress("UNCHECKED_CAST")
    override fun allow(order: Order): Boolean = order is DiscordOrder && eventClass.isInstance(order.event) && regulations.invoke(order.event as T)
}

class MessageRequest(val content: String, val channel: IChannel, val embed: EmbedObject? = null) : RequestBuffer.IRequest<IMessage> {

    override fun request(): IMessage = channel.sendMessage(content, embed, false)
}

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

class ServerMessageOrder(event: Event): DiscordOrder(event), MessageOrder {
    override val msg: IMessage
    override val channel: IChannel
    override val author: IUser
    override val client: IDiscordClient = event.client

    override var prefix: String = "-"
    override var params: Array<String> = arrayOf() //Made a var so that it *can* be modified by things like the MikuCommand in v4
    override var languageManager: (MessageOrder, MessageRequest) -> MessageRequest = { _, request -> request }

    override val content: String
        get() = if(msg.content == null) "" else if(msg.content.startsWith(prefix)) msg.content.substring(prefix.length) else msg.content

    val server: IGuild

    init {
        when(event) {
            is MessageReceivedEvent -> msg = event.message
            is MessageUpdateEvent -> msg = event.newMessage
            else -> throw IllegalArgumentException("$event is not a MessageReceived Event")
        }

        server = msg.guild
        channel = msg.channel
        author = msg.author
        prefix = server.prefix
        params = getParams()
    }

    fun getParams(paramSplitter: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Array<String> = paramSplitter(content)
}

class MessageSpy: Spy {
//    val watchtower: Set<Watchtower> = Collections.singleton(InstanceWatchtower<DiscordOrder>("Message Events", message@{ order ->
//        when(order.event) {
//            is MessageReceivedEvent -> return@message !order.event.channel.isPrivate
//            is MessageUpdateEvent -> return@message !order.event.channel.isPrivate
//            else -> return@message false
//        }
//    }))

    val watchtower: Set<Watchtower> = Collections.singleton(InstanceWatchtower<DiscordOrder>("Message Events", message@{ order -> order.event is MessageReceivedEvent || order.event is MessageUpdateEvent }))

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

class PrefixCommandWatchtower(val command: String, val getParams: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Watchtower {
    override fun allow(order: Order): Boolean = order is MessageOrder && order.msg.content.startsWith(order.prefix) && order.params[0] == command

    override fun getName(): String = command
}

val IGuild.prefix: String
    get() = String(serverData["prefix.txt"]?.data ?: "-".toByteArray(Charsets.UTF_8), Charsets.UTF_8)