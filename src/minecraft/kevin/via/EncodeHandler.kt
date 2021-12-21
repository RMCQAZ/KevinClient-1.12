package kevin.via

import com.viaversion.viaversion.api.connection.UserConnection
import com.viaversion.viaversion.exception.CancelCodecException
import com.viaversion.viaversion.exception.CancelEncoderException
import com.viaversion.viaversion.util.PipelineUtil
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import kevin.via.CommonTransformer.compress
import kevin.via.CommonTransformer.decompress
import java.lang.reflect.InvocationTargetException

@ChannelHandler.Sharable
class EncodeHandler(private val info: UserConnection) : MessageToMessageEncoder<ByteBuf?>() {
    private var handledCompression = false
    @Throws(Exception::class)
    override fun encode(ctx: ChannelHandlerContext?, p1: ByteBuf?, out: MutableList<Any>?) {
        if (!info.checkOutgoingPacket()) throw CancelEncoderException.generate(null)
        if (!info.shouldTransformPacket()) {
            out!!.add(p1!!.retain())
            return
        }
        val transformedBuf: ByteBuf = ctx!!.alloc().buffer().writeBytes(p1)
        try {
            val needsCompress = handleCompressionOrder(ctx, transformedBuf)
            info.transformOutgoing(
                transformedBuf
            ) { cause: Throwable? ->
                CancelEncoderException.generate(
                    cause
                )
            }
            if (needsCompress) {
                compress(ctx, transformedBuf)
            }
            out!!.add(transformedBuf.retain())
        } finally {
            transformedBuf.release()
        }
    }

    @Throws(InvocationTargetException::class)
    private fun handleCompressionOrder(ctx: ChannelHandlerContext, buf: ByteBuf): Boolean {
        if (handledCompression) return false
        val encoderIndex: Int = ctx.pipeline().names().indexOf("compress")
        if (encoderIndex == -1) return false
        handledCompression = true
        if (encoderIndex > ctx.pipeline().names().indexOf("via-encoder")) {
            decompress(ctx, buf)
            val encoder: ChannelHandler = ctx.pipeline().get("via-encoder")
            val decoder: ChannelHandler = ctx.pipeline().get("via-decoder")
            ctx.pipeline().remove(encoder)
            ctx.pipeline().remove(decoder)
            ctx.pipeline().addAfter("compress", "via-encoder", encoder)
            ctx.pipeline().addAfter("decompress", "via-decoder", decoder)
            return true
        }
        return false
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        if (PipelineUtil.containsCause(cause, CancelCodecException::class.java)) return
        super.exceptionCaught(ctx, cause)
    }
}