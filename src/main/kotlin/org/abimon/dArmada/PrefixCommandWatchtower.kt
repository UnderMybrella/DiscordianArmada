package org.abimon.dArmada

import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Watchtower
import org.abimon.visi.lang.splitOutsideGroup

class PrefixCommandWatchtower(val command: String, val getParams: (String) -> Array<String> = { msg -> msg.splitOutsideGroup() }): Watchtower {
    override fun allow(order: Order): Boolean = order is MessageOrder && order.msg.content.startsWith(order.prefix) && order.params[0] == command

    override fun getName(): String = command
}