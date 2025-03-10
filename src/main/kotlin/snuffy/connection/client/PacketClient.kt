package snuffy.connection.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import snuffy.connection.main.Handler
import snuffy.connection.main.Packet
import snuffy.connection.main.Packet.Companion.registerPacketType

abstract class PacketClient {
    private val group = NioEventLoopGroup()
    abstract val handler: ClientPacketHandler
    private var channel: Channel? = null

    fun connect(host: String, port: Int) {
        try {
            val b = Bootstrap()
            b.group(group)
                .channel(NioSocketChannel::class.java)
                .handler(object : ChannelInitializer<NioSocketChannel>() {
                    override fun initChannel(ch: NioSocketChannel) {
                        val pipeline = ch.pipeline()
                        pipeline.addLast("logger", LoggingHandler(LogLevel.DEBUG))
                        pipeline.addLast("frameDecoder", LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                        pipeline.addLast("frameEncoder", LengthFieldPrepender(4))
                        pipeline.addLast("handler", handler)
                    }
                })

            val f = b.connect(host, port).sync()
            println("Connected to $host:$port")

            channel = f.channel()
        } finally {
            group.shutdownGracefully()
        }
    }

    fun sendPacket(packet: Packet) {
        channel?.let {
            if (it.isActive) {
                println("Preparing to send packet: ${packet.javaClass.simpleName}")
                val byteBuf = packet.toByteBuf()
                it.writeAndFlush(byteBuf).addListener { future ->
                    if (future.isSuccess) {
                        println("Packet sent successfully: ${packet.javaClass.simpleName}")
                    } else {
                        println("Failed to send packet: ${future.cause()}")
                    }
                }
            } else {
                println("Channel is not active")
            }
        } ?: println("Channel is null")
    }

    fun registerPacket(packet: Array<Packet>) {
        packet.forEach {
            registerPacketType(it::class.java.canonicalName, it::class.java)
        }
    }
}

abstract class ClientPacketHandler : SimpleChannelInboundHandler<Packet>() {
    abstract val packetHandler: Handler

    override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
        packet.handle(ctx, packetHandler)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Connection established")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}