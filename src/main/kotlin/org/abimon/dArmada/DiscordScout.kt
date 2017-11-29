package org.abimon.dArmada

import org.abimon.imperator.handle.Imperator
import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Scout
import sx.blah.discord.api.events.Event
import sx.blah.discord.api.events.IListener
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