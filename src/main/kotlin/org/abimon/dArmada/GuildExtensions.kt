package org.abimon.dArmada

import sx.blah.discord.handle.obj.IGuild
import java.util.*

typealias Entry<K, V> = AbstractMap.SimpleEntry<K, V>

val IGuild.prefix: String
    get() = String(serverData["prefix.txt"]?.data ?: "-".toByteArray(Charsets.UTF_8), Charsets.UTF_8)

val IGuild.serverData: ServerData
    get() = ServerData.getInstance(this.longID)