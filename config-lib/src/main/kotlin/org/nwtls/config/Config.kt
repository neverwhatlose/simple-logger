package org.nwtls.config

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.get

// note: maybe it is reasonable to support nullable values as a result of get() or as an argument for put() functions
class Config(
    private val filePath: String,
    yaml: Yaml? = null
) {
    private val yaml: Yaml = yaml ?: Yaml(DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
        indent = 4
    })
    private val head: AtomicReference<Map<String, Any>> =
        AtomicReference(this.loadDataFromFile())
    val headSnapshot: Map<String, Any> get() = head.get()


    inline fun <reified T> get(path: String, default: T? = null): T? {
        val value = path.split(".").fold(headSnapshot as? Any) { acc, key ->
            if (acc is Map<*, *>) return@fold acc[key] else null
        }
        return value as? T ?: default
    }

    @Throws(IllegalStateException::class)
    inline fun <reified T> getOrFail(path: String, default: T? = null): T = get<T>(path, default)
        ?: throw IllegalArgumentException("Value at path '$path' is missing or is not of type ${T::class.simpleName}")

    @Suppress("UNCHECKED_CAST")
    fun put(path: String, value: Any) {
        val keys = path.split(".")

        fun updateNested(map: Map<String, Any>, keys: List<String>): Map<String, Any> {
            if (keys.isEmpty()) return map

            val key = keys.first()
            val rest = keys.drop(1)

            return if (rest.isEmpty()) {
                map + (key to value)
            } else {
                val nested = (map[key] as? Map<String, Any>) ?: emptyMap()
                val updated = updateNested(nested, rest)
                map + (key to updated)
            }
        }

        do {
            val old = headSnapshot
            val new = updateNested(old, keys)
        } while(!head.compareAndSet(old, new))

        persist()
    }

    @Throws(IOException::class)
    private fun loadDataFromFile(): Map<String, Any> {
        val file = File(filePath).also { it.createNewFile() }

        val obj = yaml.load<Map<String, Any>>(file.readText()) ?: emptyMap()
        return obj
    }

    @Throws(IOException::class)
    private fun persist(snapshot: Map<String, Any> = head.get()) {
        val target = Path.of(filePath)
        val tmp = Path.of("$filePath.tmp")

        Files.writeString(tmp, yaml.dump(snapshot))

        // fsync
        FileChannel.open(tmp, StandardOpenOption.WRITE).use { channel ->
            channel.force(true)
        }

        Files.move(
            tmp,
            target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE
        )
    }
}