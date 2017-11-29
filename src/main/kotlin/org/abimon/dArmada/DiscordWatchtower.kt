package org.abimon.dArmada

import org.abimon.imperator.handle.Order
import org.abimon.imperator.impl.InstanceWatchtower
import sx.blah.discord.api.events.Event

open class DiscordWatchtower<T: Event>(val eventClass: Class<T>, name: String = "", val regulations: (T) -> Boolean): InstanceWatchtower<DiscordOrder>(DiscordOrder::class.java, name) {
    companion object {
        inline operator fun <reified T: Event> invoke(name: String = "", noinline regulations: (T) -> Boolean) = DiscordWatchtower(T::class.java, name, regulations)
    }

    @Suppress("UNCHECKED_CAST")
    override fun allow(order: Order): Boolean = order is DiscordOrder && eventClass.isInstance(order.event) && regulations.invoke(order.event as T)
}