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
package cn.weforward.framework.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import cn.weforward.common.util.StringUtil;

/**
 * 主机工具
 * 
 * @author daibo
 *
 */
public class HostUtil {
	/**
	 * 查找ip
	 * 
	 * @return ip
	 */
	public static String findIp() {
		InetAddress host;
		try {
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new RuntimeException("获取ip信息出错:" + e.getMessage(), e);
		}
		return host.getHostAddress();
	}

	/**
	 * 获取最可能作为服务端点的IP（尽量使用外网地址）
	 * 
	 * @param prefix 前缀
	 * @return ip
	 */
	public static String getServiceIp(String prefix) {
		InetAddress best = null;
		Enumeration<NetworkInterface> ifs;
		try {
			ifs = NetworkInterface.getNetworkInterfaces();
			loop_ifs: while (ifs.hasMoreElements()) {
				NetworkInterface ni = ifs.nextElement();
				Enumeration<InetAddress> ias = ni.getInetAddresses();
				while (ias.hasMoreElements()) {
					InetAddress ip = ias.nextElement();
					// 找到一个非loopback地址
					if (!ip.isLoopbackAddress() && !ip.isLinkLocalAddress()
							&& StringUtil.toString(ip.getHostAddress()).indexOf(":") == -1) {
						if (null != prefix && ip.getHostAddress().startsWith(prefix)) {
							best = ip;
							break loop_ifs;
						}
						byte[] adds = ip.getAddress();
						// 只找IPV4
						if (null == adds || adds.length < 4) {
							continue;
						}
						byte[] bestAddrs = (null == best) ? null : best.getAddress();
						if (-64 == adds[0] && -88 == adds[1]) {
							// 192.168.x.x
							if (null == bestAddrs) {
								best = ip;
							}
						} else if (-84 == adds[0] && adds[1] >= 16 && adds[1] <= 31) {
							// 172.16~31.x.x段，优先于192.168.x.x
							if (null == bestAddrs || (-64 == bestAddrs[0] && -88 == bestAddrs[1])) {
								best = ip;
							}
						} else if (10 == adds[0]) {
							// 10.x.x.x段，优先于172.x.x.x
							if (null == bestAddrs || (-64 == bestAddrs[0] && -88 == bestAddrs[1])
									|| -84 == adds[0] && adds[1] >= 16 && adds[1] <= 31) {
								best = ip;
							}
						} else {
							// 外网IP
							best = ip;
							break loop_ifs;
						}
					}
				}
			}
		} catch (SocketException e) {
			throw new IllegalStateException("获取IP异常", e);
		}
		return (null == best) ? null : best.getHostAddress();
	}
}
