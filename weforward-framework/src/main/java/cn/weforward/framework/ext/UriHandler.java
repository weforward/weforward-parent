package cn.weforward.framework.ext;

import java.io.IOException;

import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.common.util.UriMatcher;

/**
 * 
 * 当uri匹配，且非weforward请求时，将接管请求
 * 
 * @author zhangpengji
 *
 */
public interface UriHandler {

	/**
	 * URI字串，支持通配符，参考{@linkplain UriMatcher}
	 * 
	 * @return URI字串
	 */
	String getUri();

	/**
	 * 处理请求
	 * 
	 * @param request  请求
	 * @param response 响应
	 * @throws IOException IO异常时抛出
	 */
	void handle(RestfulRequest request, RestfulResponse response) throws IOException;

}
