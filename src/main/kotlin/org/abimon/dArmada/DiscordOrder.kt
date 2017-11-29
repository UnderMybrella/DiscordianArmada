package org.abimon.dArmada

import org.abimon.imperator.handle.Scout
import org.abimon.imperator.impl.BaseOrder
import sx.blah.discord.api.events.Event

open class DiscordOrder(val event: Event, origin: Scout? = null): BaseOrder("Discord Order", origin)