package org.abimon.dArmada

import org.abimon.imperator.handle.Announcement
import sx.blah.discord.handle.obj.IGuild

class ServerDataAnnouncement(server: IGuild): Announcement<ServerData> {
    companion object {
        val name = "Server Data"
    }
    val serverData = server.serverData

    override fun getInfo(): ServerData = serverData

    override fun getName(): String = name
}