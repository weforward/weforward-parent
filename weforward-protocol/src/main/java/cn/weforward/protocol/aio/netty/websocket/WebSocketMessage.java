package cn.weforward.protocol.aio.netty.websocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map.Entry;

import cn.weforward.common.io.OutputStreamStay;
import cn.weforward.common.io.StayException;
import cn.weforward.protocol.aio.Headers;
import cn.weforward.protocol.aio.netty.ByteBufInput;
import cn.weforward.protocol.aio.netty.ByteBufStream;
import cn.weforward.protocol.aio.netty.CompositeByteBufStream;
import cn.weforward.protocol.aio.netty.HeadersParser;
import cn.weforward.protocol.aio.netty.NettyHttpHeaders;
import cn.weforward.protocol.aio.netty.NettyOutputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Websocket下的类似模拟HTTP请求/响应的封装
 * 
 * @author liangyi
 *
 */
public abstract class WebSocketMessage {
	/** 消息头分隔符 */
	final static byte[] HEADER_DELIMITER = { ':', ' ' };

	WebSocketSession m_Session;
	/** 消息头 */
	NettyHttpHeaders m_Headers;
	/** 消息体 */
	ByteBufStream m_Body;
	/** 直接转传（收到即转传） */
	NettyOutputStream m_TransferTo;
	/** 消息输出 */
	Output m_Output;

	public WebSocketMessage(WebSocketSession session, NettyHttpHeaders headers) {
		m_Headers = headers;
		m_Session = session;
	}

	public int readable(ByteBuf payload) throws IOException {
		ByteBufStream body;
		synchronized (this) {
			// 优先转传
			if (forward(payload)) {
				return -1;
			}
			// 不转传则写入缓冲区
			body = m_Body;
			if (null == body) {
				body = new CompositeByteBufStream(m_Session.compositeBuffer());
				m_Body = body;
			}
			body.readable(payload);
		}
		return body.available();
	}

	synchronized public void complete() throws IOException {
		ByteBufStream body = m_Body;
		if (null == body) {
			m_Body = ByteBufInput._completed;
		} else {
			body.completed();
		}
	}

	synchronized public InputStream getStream() throws IOException {
		if (m_Body instanceof ByteBufInput) {
			return (ByteBufInput) m_Body;
		}
		ByteBufInput stream;
		if (null == m_Body) {
			stream = new ByteBufInput(m_Session.compositeBuffer(), false);
		} else {
			CompositeByteBufStream buffers = (CompositeByteBufStream) m_Body;
			stream = new ByteBufInput(buffers.detach(), buffers.isCompleted());
		}
		m_Body = stream;
		return stream;
	}

	synchronized void cleanup() {
		if (null != m_Body) {
			m_Body.abort();
			m_Body = null;
		}
		m_Headers = null;
	}

	public void abort() {
		cleanup();
	}

	synchronized public void setHeader(String name, String value) {
		m_Headers.setHeader(name, value);
	}

	synchronized public Output openWriter() {
		if (null == m_Output) {
			m_Output = new Output();
		}
		return m_Output;
	}

	/**
	 * 直接输出消息
	 * 
	 * @param body
	 *            消息体（可为空）
	 * @throws IOException
	 */
	public void flush(ByteBuf body) throws IOException {
		Output out = openWriter();
		if (null != body) {
			out.write(body);
		}
		out.close();
	}

	public Headers getHeaders() {
		return m_Headers;
	}

	synchronized public void transferTo(OutputStream writer, int skipBytes) throws IOException {
		if (!(m_Body instanceof CompositeByteBufStream)) {
			if (ByteBufInput._completed == m_Body) {
				// 消息体是0长度（无数据的）？
				writer.close();
				return;
			}
			throw new IOException("只能在getStream前调用");
		}
		CompositeByteBufStream bufStream = (CompositeByteBufStream) m_Body;
		int readableBytes = bufStream.available();
		if (skipBytes > 0) {
			if (skipBytes > readableBytes) {
				throw new IOException("超过范围" + skipBytes + ">" + readableBytes);
			}
			bufStream.skipBytes(skipBytes);
		}
		// 包装转传器
		m_TransferTo = NettyOutputStream.wrap(writer);
		// 写入已接收到缓冲区的数据
		ByteBuf buf = bufStream.detach();
		if (null != buf) {
			try {
				m_Body = null;
				forward(buf);
			} finally {
				buf.release();
			}
		}
	}

	public boolean isCompleted() {
		Output out = m_Output;
		if (null != out) {
			return !out.isOpen();
		}
		return (null == m_Body) ? false : m_Body.isCompleted();
	}

	protected CompositeByteBuf compositeBuffer() {
		return m_Session.compositeBuffer();
	}

	/**
	 * 转发消息体
	 * 
	 * @param data
	 *            数据片段
	 */
	boolean forward(ByteBuf data) {
		NettyOutputStream out = m_TransferTo;
		if (null == out) {
			return false;
		}
		try {
			out.write(data);
			return true;
		} catch (IOException e) {
			// 转传失败
			m_Session.errorTransferTo(this, e, data, out);
		}
		return false;
	}

	static final int STATE_INIT = 1;
	static final int STATE_HEADER = 2;
	static final int STATE_PENDING = 6;
	static final int STATE_CLOSED = 0x10;

	/**
	 * 输出流封装
	 */
	class Output extends NettyOutputStream implements OutputStreamStay {
		/** 暂留缓冲区 */
		CompositeByteBuf m_StayBuffers;
		/** 最后的数据块 */
		ByteBuf m_Last;
		/** 状态 */
		int m_State = STATE_INIT;

		private boolean isHeaderSended() {
			return STATE_HEADER == m_State;
		}

		protected boolean sendHeaders(ByteBuf content, boolean finalFragment) {
			if (STATE_INIT != m_State) {
				return false;
			}
			ByteBuf buf = allocBuffer(HeadersParser.HEADER_LENGTH_MAX);
			CompositeByteBuf bufs = null;
			try {
				// 先置入消息ID
				putId(buf);
				// 输出消息头
				putHeaders(buf);
				if (null != content) {
					// 有内容的话，打包内容
					bufs = compositeBuffer();
					bufs.addComponent(buf);
					bufs.addComponent(content);
					buf = bufs;
					bufs = null;
				}
				BinaryWebSocketFrame frame = new BinaryWebSocketFrame(finalFragment, 0, buf);
				writeAndFlush(frame);
				buf = null;
				bufs = null;
			} finally {
				if (null != buf) {
					buf.release();
				}
				if (null != bufs) {
					bufs.release();
				}
			}
			m_State = STATE_HEADER;
			return true;
		}

		protected void putHeaders(ByteBuf buf) {
			io.netty.handler.codec.http.HttpHeaders hs = m_Headers.getHeaders();
			Iterator<Entry<CharSequence, CharSequence>> it = hs.iteratorCharSequence();
			while (it.hasNext()) {
				Entry<CharSequence, CharSequence> h = it.next();
				CharSequence v = h.getValue();
				if (null != v && v.length() > 0) {
					buf.writeCharSequence(h.getKey(), CharsetUtil.UTF_8);
					buf.writeBytes(HEADER_DELIMITER);
					buf.writeCharSequence(h.getValue(), CharsetUtil.UTF_8);
					buf.writeByte(WebSocketSession.PACKET_LF);
				}
			}
			buf.writeByte(WebSocketSession.PACKET_LF);
		}

		protected void putId(ByteBuf buf) {
			buf.writeByte(WebSocketSession.PACKET_PREAMBLE_REQUEST);
			buf.writeCharSequence(m_Session.getId(), CharsetUtil.UTF_8);
			buf.writeByte(WebSocketSession.PACKET_LF);
		}

		synchronized protected void cleanup() {
			ByteBuf buf;
			buf = m_Last;
			if (null != buf) {
				m_Last = null;
				buf.release();
			}
			buf = m_StayBuffers;
			if (null != buf) {
				m_StayBuffers = null;
				buf.release();
			}
			super.cleanup();
		}

		//// OutputStreamStay /////
		@Override
		synchronized public void stay() throws StayException {
			if (STATE_INIT != m_State) {
				throw new StayException("已输出头");
			}
			m_StayBuffers = compositeBuffer();
		}

		//// Channel ////
		@Override
		public boolean isOpen() {
			return STATE_CLOSED != m_State;
		}

		//// NettyOutputStream ////
		protected ByteBuf allocBuffer(int len) {
			return allocBuffer(len);
		}

		protected void ensureOpen() throws IOException {
			if (!isOpen()) {
				throw new IOException("closed");
			}
		}

		synchronized public void write(ByteBuf buf) throws IOException {
			ensureOpen();
			if (null != m_StayBuffers) {
				// 写到暂留缓存
				flushBuffer();
				m_StayBuffers.addComponent(true, buf.retain());
				return;
			}
			// 总保留最后一个数据块先不发送
			ByteBuf last = m_Last;
			m_Last = buf.retain();
			try {
				if (null != last) {
					// m_BodyLength += last.readableBytes();
					// // 也许未发送消息头，混合一起发送
					// if (!sendHeaders(last, false)) {
					// // 发送消息体
					// BinaryWebSocketFrame frame = new
					// BinaryWebSocketFrame(false, 0, last);
					// writeAndFlush(frame);
					// last = null;
					// flushBuffer();
					// }
					// 先发送消息头
					sendHeaders(null, false);
					flushBuffer();
					// 发送消息体
					BinaryWebSocketFrame frame = new BinaryWebSocketFrame(false, 0, last);
					writeAndFlush(frame);
					last = null;
				}
			} finally {
				if (null != last) {
					last.release();
				}
			}
		}

		@Override
		synchronized public void flush() throws IOException {
			super.flush();
			ByteBuf buf = m_StayBuffers;
			if (null != buf) {
				// 刷写暂留缓存
				m_StayBuffers = null;
				if (buf.isReadable()) {
					write(buf);
				}
				buf.release();
			}
		}

		@Override
		synchronized public void close() throws IOException {
			ByteBuf buf = null;
			CompositeByteBuf bufs = null;
			try {
				if (!isOpen()) {
					throw new EOFException();
				}
				flush();
				BinaryWebSocketFrame frame;
				if (!isHeaderSended()) {
					// 消息头未发送
					buf = allocBuffer(HeadersParser.HEADER_LENGTH_MAX);
					putId(buf);
					putHeaders(buf);
				} else {
					buf = allocBuffer(WebSocketSession.PACKET_ID_LENGTH + 2);
					putId(buf);
				}
				if (null != m_Last) {
					// 混合内容打包
					bufs = compositeBuffer();
					bufs.addComponent(buf);
					bufs.addComponent(m_Last);
					buf = bufs;
					bufs = null;
				}
				frame = new BinaryWebSocketFrame(true, 0, buf);
				m_State = STATE_PENDING;
				ChannelFuture future = writeAndFlush(frame);
				m_Last = null;
				buf = null;
				future.addListener(new GenericFutureListener<Future<Void>>() {
					@Override
					public void operationComplete(Future<Void> future) throws Exception {
						if (future.isSuccess()) {
							// 已经提交完请求
							success();
						} else {
							// 失败了:(
							fail();
						}
					}
				});
			} finally {
				cleanup();
				if (null != buf) {
					buf.release();
				}
				if (null != bufs) {
					bufs.release();
				}
			}
		}

		@Override
		synchronized public void cancel() throws IOException {
			super.cancel();
			disconnect();
		}

		protected void success() {
			m_Session.messageCompleted(WebSocketMessage.this);
		}

		protected void fail() {
			m_Session.messageAbort(WebSocketMessage.this);
		}
	}

	public ChannelFuture writeAndFlush(Object content) {
		return m_Session.writeAndFlush(content);
	}

	public void disconnect() {
		m_Session.disconnect();
	}
}
