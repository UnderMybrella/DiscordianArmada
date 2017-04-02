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
import sx.blah.discord.handle.obj.*
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

class MessageOrder(event: Event): DiscordOrder(event) {
    val msg: IMessage
    val server: IGuild
    val channel: IChannel
    val author: IUser
    var prefix: String
    val client: IDiscordClient = event.client
    var params: Array<String> = arrayOf() //Made a var so that it *can* be modified by things like the MikuCommand in v4

    var languageManager: (MessageOrder, MessageRequest) -> MessageRequest = { order, request -> request }

    fun sendReply(content: String): IMessage = sendReply(MessageRequest(content, msg.channel))
    fun sendReply(content: String, embed: EmbedObject): IMessage = sendReply(MessageRequest(content, msg.channel, embed))

    fun sendPrivateReply(content: String): IMessage = sendReply(MessageRequest(content, msg.author.privateChannel))
    fun sendPrivateReply(content: String, embed: EmbedObject): IMessage = sendReply(MessageRequest(content, msg.author.privateChannel, embed))

    fun sendReply(message: MessageRequest): IMessage = RequestBuffer.request(languageManager.invoke(this, message)).get()

    fun delete() = RequestBuffer.request { msg.delete() }

    init {
        when(event) {
            is MessageReceivedEvent -> msg = event.message
            is MessageUpdateEvent -> msg = event.newMessage
            else -> throw IllegalArgumentException("$event is not a MessageReceived Event")
        }

        server = msg.guild
        channel = msg.channel
        author = msg.author
        prefix = server.getPrefix()
        params = getParams()
    }

    fun getContent(): String = if(msg.content == null) "" else if(msg.content.startsWith(prefix)) msg.content.substring(prefix.length) else msg.content
    fun getParams(paramSplitter: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Array<String> = paramSplitter.invoke(getContent())
}

class MessageSpy: Spy {
    val watchtower: Set<Watchtower> = Collections.singleton(InstanceWatchtower<DiscordOrder>("Message Events", message@{ order ->
        when(order.event) {
            is MessageReceivedEvent -> return@message !order.event.channel.isPrivate
            is MessageUpdateEvent -> return@message !order.event.channel.isPrivate
            else -> return@message false
        }
    }))

    override fun fiddle(order: Order): Order {
        val msg = MessageOrder((order as DiscordOrder).event)
        if(msg.author.isBot)
            return order
        return msg
    }

    override fun getName(): String = "Message Spy"

    override fun getWatchtowers(): Collection<Watchtower> = watchtower
}

class PrefixCommandWatchtower(val command: String, val getParams: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Watchtower {
    override fun allow(order: Order): Boolean = order is MessageOrder && order.msg.content.startsWith("${order.server.getPrefix()}$command")

    override fun getName(): String = command
}

fun IGuild.getPrefix(): String = String(ServerData(this)["prefix.txt"]?.getData() ?: "-".toByteArray(Charsets.UTF_8), Charsets.UTF_8)
val IUser.privateChannel: IPrivateChannel
    get() = RequestBuffer.request(RequestBuffer.IRequest { orCreatePMChannel }).get()