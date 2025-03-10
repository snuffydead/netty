package snuffy.connection.main

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import snuffy.connection.packets.s2c.*
import kotlin.system.exitProcess

abstract class Handler