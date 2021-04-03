package cn.weforward.framework.ext;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.weforward.common.json.JsonOutputStream;
import cn.weforward.common.restful.RestfulRequest;
import cn.weforward.common.restful.RestfulResponse;
import cn.weforward.protocol.serial.JsonSerialEngine;
import cn.weforward.protocol.support.datatype.SimpleDtList;
import cn.weforward.protocol.support.datatype.SimpleDtObject;
import cn.weforward.protocol.support.doc.ServiceDocumentVo;

/**
 * 本地文档
 * 
 * @author zhangpengji
 *
 */
class LocalDocUriHandler implements UriHandler {

	static final String URI = "/__wf_doc/**";

	Supplier<ServiceDocumentVo> m_Doc;

	LocalDocUriHandler(Supplier<ServiceDocumentVo> doc) {
		m_Doc = doc;
	}

	@Override
	public String getUri() {
		return URI;
	}

	@Override
	public void handle(RestfulRequest request, RestfulResponse response) throws IOException {
		ServiceDocumentVo vo = m_Doc.get();
		response.setHeader("Content-Type", "application/json;charset=utf-8");
		if (null == vo) {
			response.setStatus(RestfulResponse.STATUS_NOT_FOUND);
			response.openOutput().close();
		} else {
			response.setStatus(RestfulResponse.STATUS_OK);
			List<ServiceDocumentVo> vos = Collections.singletonList(vo);
			SimpleDtObject result = new SimpleDtObject();
			result.put("docs", SimpleDtList.toDtList(vos, ServiceDocumentVo.MAPPER));
			try (OutputStream out = response.openOutput()) {
				JsonOutputStream jos = new JsonOutputStream(out);
				JsonSerialEngine.formatObject(result, jos);
			}
		}
	}

}
