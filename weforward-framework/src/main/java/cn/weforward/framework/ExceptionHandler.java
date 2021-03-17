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
package cn.weforward.framework;

/**
 * 异常处理对象
 * 
 * @author daibo
 *
 */
public interface ExceptionHandler {
	/**
	 * 处理异常的方法 如：
	 * 
	 * <code>
	 *		public Throwable exception(Throwable error) {
	 *			if (error instanceof PayException) {
	 *				return new ApiException(ExceptionUtil.getCode((PayException) error), error.getMessage());
	 *			} else if (error instanceof AccountException) {
	 *				return new ApiException(ExceptionUtil.getCode((AccountException) error), error.getMessage());
	 *			}
	 *			return error;
	 *		}
	 *</code>
	 * 
	 * @param error 异常
	 * @return 处理后的异常
	 */
	Throwable exception(Throwable error);
}
