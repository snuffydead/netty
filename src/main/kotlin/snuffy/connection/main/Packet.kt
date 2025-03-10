package snuffy.connection.main

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext

abstract class Packet {
    abstract val packetId: Int
    abstract fun handle(ctx: ChannelHandlerContext, handler: Handler)

    companion object {
        private val packetRegistry = mutableMapOf<String, Class<out Packet>>()

        fun registerPacketType(name: String, packetClass: Class<out Packet>) {
            packetRegistry[name] = packetClass
        }

        fun fromByteBuf(buf: ByteBuf): Packet {
            val bytes = ByteArray(buf.readableBytes())
            buf.readBytes(bytes)
            val mapper = ObjectMapper().registerKotlinModule()
            val jsonNode = mapper.readTree(bytes)
            val type = jsonNode.get("type").asText()
            val packetClass = packetRegistry[type] ?: throw IllegalArgumentException("Unknown packet type: $type")
            return mapper.treeToValue(jsonNode, packetClass)
        }
    }

    fun toByteBuf(): ByteBuf {
        val mapper = ObjectMapper().registerKotlinModule()
        val json = mapper.writeValueAsString(this)
        val bytes = json.toByteArray(Charsets.UTF_8)

        val buf = Unpooled.buffer()
        buf.writeBytes(bytes)
        return buf
    }
}