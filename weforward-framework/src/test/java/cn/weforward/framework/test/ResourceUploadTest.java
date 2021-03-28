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
package cn.weforward.framework.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

public class ResourceUploadTest {

	@Test
	public void upload() throws Exception {
		String url = "http://127.0.0.1:5661/__wf_stream/?token=U77CFaSCQU%2F1MVfzDRzdJJ3gRkvbhtb7zMFnnThZ";
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);

		String boundary = "abcabcabcabcabcabcabcabcabcabcabc" + System.currentTimeMillis();
		// 2.Http请求行/头
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Charset", "utf-8");
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		// 3.Http请求体
		DataOutputStream out = new DataOutputStream(conn.getOutputStream());
		out.write(("--" + boundary + "\r\n" + "Content-Disposition: form-data; name=\"file\"; filename=\"filename\"\r\n"
				+ "Content-Type: application/octet-stream; charset=utf-8" + "\r\n\r\n").getBytes("utf-8"));
		InputStream in = new FileInputStream("1.jpg");
		byte[] b = new byte[512];
		int l = 0;
		while ((l = in.read(b)) != -1) {
			out.write(b, 0, l); // 写入文件
		}
		out.write(("\r\n--" + boundary + "--\r\n").getBytes("utf-8"));
		out.flush();
		out.close();
		in.close();

		// 4.Http响应
		in = null;
		try {
			in = conn.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			in = conn.getErrorStream();
		}
		BufferedReader bf = new BufferedReader(new InputStreamReader(in, "utf-8"));
		String line = null;
		while ((line = bf.readLine()) != null) {
			System.out.println(line);
		}
		bf.close();
	}
}
