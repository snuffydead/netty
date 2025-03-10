package snuffy.connection.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.MessageToMessageDecoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import snuffy.connection.main.Handler
import snuffy.connection.main.Packet
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

abstract class PacketServer {
    private val bossGroup = NioEventLoopGroup()
    private val workerGroup = NioEventLoopGroup()
    abstract val handler: ServerPacketHandler

    fun run(port: Int) {
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        val pipeline = ch.pipeline()
                        pipeline.addLast("logger", LoggingHandler(LogLevel.DEBUG))
                        pipeline.addLast("frameDecoder", LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4))
                        pipeline.addLast("packetDecoder", PacketDecoder())
                        pipeline.addLast("frameEncoder", LengthFieldPrepender(4))
                        pipeline.addLast("handler", handler)
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val f = b.bind(port).sync()
            println("Server started on port $port")
            f.channel().closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    class PacketDecoder : MessageToMessageDecoder<ByteBuf>() {
        override fun decode(ctx: ChannelHandlerContext, msg: ByteBuf, out: MutableList<Any>) {
            val packet = Packet.fromByteBuf(msg)
            out.add(packet)
        }
    }
}

data class ClientConnection(
    val channel: Channel,
    var ipAddress: String,
    var lastPacketTime: Long = System.currentTimeMillis()
)

data class AuthState(
    val channel: Channel,
    var connectTime: Long = System.currentTimeMillis()
)

abstract class ServerPacketHandler : SimpleChannelInboundHandler<Packet>() {
    abstract val packetHandler: Handler

    override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet) {
        packet.handle(ctx, packetHandler)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}