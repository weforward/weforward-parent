/**
 * Copyright (c) 2019,2020 honintech
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package cn.weforward.framework.exception;

import cn.weforward.common.DistributedObject;
import cn.weforward.framework.WeforwardSession;
import cn.weforward.protocol.ResponseConstants;

/**
 * 转发异常
 * 
 * @author daibo
 *
 */
public class ForwardException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** 后备服务器 */
	private static final String FORWARD_TO_BACKUP = ResponseConstants.FORWARD_TO_BACKUP;// "__backup";
	/** 转发的目标 */
	protected String m_ForwardTo;

	public ForwardException(String forwardTo) {
		super("请求转发");
		m_ForwardTo = forwardTo;
	}

	/**
	 * 转发的目标
	 * 
	 * @return 目标
	 */
	public String getForwardTo() {
		return m_ForwardTo;
	}

	/**
	 * 转发请求到后备服务器
	 * 
	 * @throws ForwardException 转发异常
	 */
	public static void forwardBackUp() throws ForwardException {
		forwardTo(FORWARD_TO_BACKUP);
	}

	/**
	 * 转发请求
	 * 
	 * @param object 分布式对象 <code>
	 *  if (object instanceof DistributedObject) {
			forwardToIfNeed((DistributedObject) (object));
		}
	 * </code>
	 * @throws ForwardException 转发异常
	 */
	public static void forwardToIfNeed(Object object) throws ForwardException {
		forwardToIfNeed(object, true);
	}

	/**
	 * 转发请求
	 * 
	 * @param object      分布式对象 <code>
	 *  if (object instanceof DistributedObject) {
			forwardToIfNeed((DistributedObject) (object));
		}
	 * </code>
	 * @param autoDriveIt 是否自动接管实例
	 * @throws ForwardException 转发异常
	 */
	public static void forwardToIfNeed(Object object, boolean autoDriveIt) throws ForwardException {
		if (object instanceof DistributedObject) {
			forwardToIfNeed((DistributedObject) (object), autoDriveIt);
		}
	}

	/**
	 * 转发请求
	 * 
	 * @param object 分布式对象
	 * @throws ForwardException 转发异常
	 */
	public static void forwardToIfNeed(DistributedObject object) throws ForwardException {
		forwardToIfNeed(object, true);
	}

	/**
	 * 转发请求
	 * 
	 * @param object 分布式对象
	 * @throws ForwardException 转发异常
	 */
	public static void forwardToIfNeed(DistributedObject object, boolean autoDriveIt) throws ForwardException {
		if (!WeforwardSession.TLS.isSupportForward()) {
			if (autoDriveIt) {
				object.tryDriveIt();
			} else {
				return;
			}
		}
		if (!object.iDo()) {
			throw new ForwardException(object.getDriveIt());
		}
	}

	/**
	 * 转发请求
	 * 
	 * @param to 转发的地址
	 * @throws ForwardException 转发异常
	 */
	public static void forwardTo(String to) throws ForwardException {
		throw new ForwardException(to);
	}

}
