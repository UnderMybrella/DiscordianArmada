package org.abimon.dArmada

import org.abimon.imperator.handle.Imperator
import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Scout

class ServerDataScout: Scout {

    override fun addAnnouncements(order: Order) {
        if(order is ServerMessageOrder)
            order.addAnnouncement(ServerDataAnnouncement(order.server))
    }

    override fun getName(): String = "Server Data Supplier"

    override fun setImperator(imperator: Imperator) {}
}