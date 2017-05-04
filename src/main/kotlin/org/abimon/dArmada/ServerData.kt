package org.abimon.dArmada

import org.abimon.imperator.handle.Announcement
import org.abimon.imperator.handle.Imperator
import org.abimon.imperator.handle.Order
import org.abimon.imperator.handle.Scout
import org.abimon.visi.io.DataSource
import org.abimon.visi.io.FileDataSource
import org.abimon.visi.io.check
import org.abimon.visi.io.iterate
import sx.blah.discord.handle.obj.IGuild
import java.io.File
import java.util.*

data class Entry<out K, out V>(override val key: K, override val value: V): Map.Entry<K, V>

class ServerData private constructor(val server: Long): Map<String, DataSource> {
    companion object {
        val INSTANCES = HashMap<Long, ServerData>()

        operator fun invoke(id: Long): ServerData = getInstance(id)
        fun getInstance(id: Long): ServerData {
            if(!INSTANCES.containsKey(id))
                INSTANCES[id] = ServerData(id)
            return INSTANCES[id]!!
        }
    }

    val dir = File("server_data${File.separator}$server")

    override val entries: Set<Map.Entry<String, DataSource>>
        get() {
            val set = HashSet<Map.Entry<String, DataSource>>()
            dir.iterate(false).forEach { file -> set.add(Entry(file.absolutePath.replace("${dir.absolutePath}${File.separator}", ""), FileDataSource(file))) }
            return set
        }
    override val keys: Set<String>
        get() {
            val set = HashSet<String>()
            dir.iterate(false).forEach { file -> set.add(file.absolutePath.replace("${dir.absolutePath}${File.separator}", "")) }
            return set
        }
    override val size: Int
        get() = dir.iterate(false).size
    override val values: Collection<DataSource>
        get() {
            val list = ArrayList<DataSource>()
            dir.iterate(false).forEach { file -> list.add(FileDataSource(file)) }
            return list
        }

    override fun containsKey(key: String): Boolean = File(dir, key).exists()

    override fun containsValue(value: DataSource): Boolean {
        if(value is FileDataSource && value.file.exists())
            return true
        return dir.iterate(false).any { file -> FileDataSource(file).getInputStream().check(value.getInputStream()) }
    }

    override fun get(key: String): DataSource? {
        val file = File(dir, key)
        if(file.exists())
            return FileDataSource(file)
        return null
    }

    operator fun set(key: String, data: ByteArray) {
        val file = File(dir, key)
        file.writeBytes(data)
    }

    override fun isEmpty(): Boolean = dir.iterate(false).isEmpty()

    override fun equals(other: Any?): Boolean = if(other == null) false else other.hashCode() == hashCode()

    override fun hashCode(): Int = server.hashCode()

    override fun toString(): String = "Server Data for ID $server"

    init {
        if(!dir.exists())
            dir.mkdirs()
    }
}

class ServerDataAnnouncement(server: IGuild): Announcement<ServerData> {
    companion object {
        val name = "Server Data"
    }
    val serverData = server.serverData

    override fun getInfo(): ServerData = serverData

    override fun getName(): String = name
}

class ServerDataScout: Scout {

    override fun addAnnouncements(order: Order) {
        if(order is MessageOrder)
            order.addAnnouncement(ServerDataAnnouncement(order.server))
    }

    override fun getName(): String = "Server Data Supplier"

    override fun setImperator(imperator: Imperator) {}
}

val IGuild.serverData: ServerData
    get() = ServerData.getInstance(this.longID)