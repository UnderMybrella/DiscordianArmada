package org.abimon.dArmada

import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Soldier
import org.abimon.imperator.handle.Watchtower
import org.abimon.imperator.impl.BasicWatchtower
import sx.blah.discord.api.events.Event
import java.util.ArrayList

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