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
package cn.weforward.common.util;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import cn.weforward.common.execption.AbortException;
import cn.weforward.common.execption.OverloadException;
import cn.weforward.common.execption.TimeoutException;
import cn.weforward.common.sys.ClockTick;

/**
 * 使用LruHashMap及WeakReference支持单例的缓存管理器
 * 
 * @author liangyi
 *
 * @param <K>
 *            键的类型（通常是字串）
 * @param <V>
 *            值的类型
 */
public class LruCache<K, V> extends LruHashMap<K, V> {
	/** 传递GC事件给实现GcCleanable接口的项 */
	public static final int POLICY_GC_FORWARD = 0x1000;
	/** 使用弱引用来确认对象是否可达（还有其它引用），控制单例 */
	public static final int OPTION_REACHABLE = 1 << 29;

	/** 用于生成运行时的时间戳（单位为秒） */
	protected static final ClockTick _clock = ClockTick.getInstance(1);
	/** 用于标识缓存项正在加载中 */
	protected static final WeakReference<?> _pending = new MarkReference("pending");
	/** 用于标识缓存项未加载 */
	protected static final WeakReference<?> _unassigned = new MarkReference("unassigned");
	/** 用于标识缓存项需要刷写 */
	protected static final WeakReference<?> _dirty = new MarkReference("dirty");
	/** 标记无更新的缓存项 */
	protected static final DirtyData<?> _emptyDirtyData = new EmptyDirtyData();

	/** 名称 */
	protected String m_Name;
	/** OPTION_xxx */
	protected int m_Options;
	/** 缓存项的超时时间（秒），默认为60分钟 */
	protected int m_Timeout = 10 * 60;
	/** NULL值重新加载间隔（秒），0为不重加载 */
	protected int m_NullTimeout = 0;
	/** 加载等待超时值（毫秒），默认5000毫秒 */
	protected int m_PendingTimeout = 5 * 1000;
	/** 最大加载并发数 */
	protected int m_MaxLoadConcurrent;
	/** 当前加载中的并发数 */
	protected AtomicInteger m_LoadConcurrent;
	/** 更新链表 */
	protected CacheNode<K, V> m_UpdatedChain;
	/** get,load计数 */
	volatile protected long m_GetCount, m_LoadCount;

	public LruCache(String name) {
		super();
		m_Name = name;
	}

	public LruCache(int maxCapacity, String name) {
		super();
		m_Name = name;
		setMaxCapacity(maxCapacity);
	}

	public String getName() {
		return m_Name;
	}

	public void setName(String name) {
		m_Name = name;
	}

	/**
	 * 设置缓存长时间未访问的超时值
	 * 
	 * @param seconds
	 *            超时值（秒），超过此时间未访问的缓存将可能被清除
	 */
	public void setTimeout(int seconds) {
		m_Timeout = seconds;
	}

	/**
	 * 缓存长时间未访问的超时值
	 */
	public int getTimeout() {
		return m_Timeout;
	}

	/**
	 * NULL值(getHitLoad)重新加载间隔（秒）
	 * 
	 * @param seconds
	 *            重新加载间隔（秒），0为不控制
	 */
	public void setNullTimeout(int seconds) {
		m_NullTimeout = seconds;
	}

	public int getNullTimeout() {
		return m_NullTimeout;
	}

	public int getPendingTimeout() {
		return m_PendingTimeout;
	}

	/**
	 * 加载等待超时值（毫秒），默认5000毫秒
	 * 
	 * @param mills
	 *            超时值（毫秒）
	 */
	public void setPendingTimeout(int mills) {
		m_PendingTimeout = mills;
	}

	public int getMaxLoadConcurrent() {
		return m_MaxLoadConcurrent;
	}

	/**
	 * 最大加载并发数，默认不限制
	 * 
	 * @param max
	 *            最大值（<=0为不限制）
	 */
	public void setMaxLoadConcurrent(int max) {
		m_MaxLoadConcurrent = max;
		if (max <= 0) {
			m_LoadConcurrent = null;
			m_MaxLoadConcurrent = 0;
		} else if (null == m_LoadConcurrent) {
			m_LoadConcurrent = new AtomicInteger();
		}
	}

	/**
	 * 是否控制对象单例
	 * 
	 * @param enabled
	 *            是否单例
	 */
	public void setReachable(boolean enabled) {
		if (enabled) {
			m_Options |= OPTION_REACHABLE;
		} else {
			m_Options &= (~OPTION_REACHABLE);
		}
	}

	/**
	 * 是否控制对象单例
	 */
	boolean isReachable() {
		return OPTION_REACHABLE == (OPTION_REACHABLE & m_Options);
	}

	/**
	 * 取缓存项，且当缓存中没有时尝试加载缓存项，不管加载后的值是否为null（在缓存项有存在时）都只加载一次
	 * 
	 * @param key
	 *            缓存项的键
	 * @param loader
	 *            缓存项加载器，若为null则不加载只创建空的缓存项
	 * @return 返回相应缓存项的值，没有则返回null
	 */
	public V getHintLoad(K key, Loader<K, V> loader) {
		if (null == key) {
			return null;
		}
		int hash = hash(key);
		CacheNode<K, V> node = openNode(hash, key);
		V v;
		v = node.getValue();
		if (null != v) {
			// 缓存值有效，返回
			afterNodeAccess(node);
			return v;
		}

		if (node.reloadNull(m_NullTimeout)) {
			// 未到空值重加载时间，继续返回
			afterNodeAccess(node);
			return v;
		}

		// 尝试加载
		v = load(node, loader, false);
		return v;
	}

	/**
	 * 取缓存项，且当缓存中没有时尝试加载缓存项，不管加载后的值是否为null（在缓存项有存在时）都只加载一次
	 * 
	 * @param key
	 *            缓存项的键
	 * @param loader
	 *            缓存项加载器，若为null则不加载只创建空的缓存项
	 * @param expire
	 *            过期值（秒），若不为0则超过此时间重新加载
	 * @return 返回相应缓存项的值，没有则返回null
	 */
	public V getHintLoad(K key, Loader<K, V> loader, int expire) {
		if (null == key) {
			return null;
		}
		int hash = hash(key);
		CacheNode<K, V> node = openNode(hash, key);
		V v;
		v = node.getValue();
		if (null != v) {
			if (node.isReady(v, expire)) {
				// 缓存值有效，返回
				afterNodeAccess(node);
				return v;
			}
			// 缓存值过期，尝试加载它
			node.reinit();
			try {
				v = load(node, loader, false);
			} catch (Throwable e) {
				// 若出错把旧值放回缓存
				if (null != v) {
					_Logger.warn("缓存项过期(" + expire + ")重加载失败，保持旧值" + node, e);
				}
				node.ready(v);
				throw e;
			}
			return v;
		}

		if (node.reloadNull(m_NullTimeout)) {
			// 未到空值重加载时间，继续返回
			afterNodeAccess(node);
			return v;
		}

		// 尝试加载
		v = load(node, loader, false);
		return v;
	}

	/**
	 * 取缓存项，当缓存中没有或为null时总会尝试加载缓存项
	 * 
	 * @param key
	 *            缓存项的键
	 * @param loader
	 *            缓存项加载器，若为null则不加载只创建空的缓存项
	 * @param expire
	 *            过期值（秒），若不为0则超过此时间重新加载
	 * @return 返回相应缓存项的值，没有则返回null
	 */
	public V getAndLoad(K key, Loader<K, V> loader, int expire) {
		if (null == key) {
			return null;
		}
		int hash = hash(key);
		CacheNode<K, V> node = openNode(hash, key);
		V v;
		v = node.getValue();
		if (null != v && node.isReady(v, expire)) {
			// 缓存值有效，返回
			afterNodeAccess(node);
			return v;
		}
		// 缓存值为null或过期，尝试加载它
		node.reinit();
		try {
			v = load(node, loader, false);
		} catch (Throwable e) {
			// 若出错把旧值放回缓存
			// node.recover(v);
			node.ready(v);
			if (null != v) {
				_Logger.warn("缓存项过期(" + expire + ")重加载失败，保持旧值" + node, e);
			}
			throw e;
		}
		// Node<K, V> check = getNode(hash, key);
		// if (node != check) {
		// // 确认一遍node变了？
		// check = getNode(hash, key);
		// return check.getValue();
		// }
		return v;
	}

	/**
	 * 更新缓存项
	 * 
	 * @param key
	 *            缓存项的key
	 * @param updater
	 *            更新器
	 * @return 更新后缓存项当前的值
	 */
	public V update(K key, Updater<K, V> updater) {
		if (null == key) {
			return null;
		}
		int hash = hash(key);
		CacheNode<K, V> node = openNode(hash, key);
		V v;
		if (null != updater) {
			synchronized (node) {
				v = node.getValue();
				v = updater.update(key, v);
				node.setValue(v);
			}
		} else {
			v = node.getValueFast();
		}
		afterNodeUpdate(node);
		return v;
	}

	public boolean markUpdated(K key) {
		if (null == key) {
			return false;
		}
		int hash = hash(key);
		CacheNode<K, V> node = (CacheNode<K, V>) getNode(hash, key);
		if (null != node) {
			node.dirty();
			afterNodeUpdate(node);
			return true;
		}
		return false;
	}

	@Override
	public void onGcCleanup(int policy) {
		if (0 == size()) {
			return;
		}

		if (POLICY_GC_FORWARD == (POLICY_GC_FORWARD & policy)) {
			// // TODO 转发
			// policy = (POLICY_MASK & policy);
			return;
		}

		if (POLICY_LOW == policy && size() > 16) {
			// 内存低分离或清除1/4缓存项
			if (trim(size() >> 2) > 0) {
				pinch();
			}
			return;
		}
		if (POLICY_CRITICAL == policy && size() > 16) {
			// 内存紧张分离或清除全部缓存项
			trim(size());
			return;
		}

		int timeout = getTimeout();
		if (timeout > 0) {
			int tick = _clock.getTicker();
			// 指定超时值时，根据LRU表清理空闲项
			CacheNode<K, V> tail = (CacheNode<K, V>) m_LruTail;
			if ((tail.lastAccess + timeout) < tick) {
				if (idleTrim(tick - timeout) > 0) {
					pinch();
				}
				CacheNode<K, V> last = (CacheNode<K, V>) tail.lruBefore;
				if (last == m_LruHead) {
					tail.lastAccess = tick;
				} else {
					tail.lastAccess = last.lastAccess;
				}
			}
			return;
		}
	}

	/**
	 * 按LRU且确认缓存项是否被引用下清除缓存项
	 * 
	 * @param expect
	 *            期望移除的项数
	 * @return 已移除的项数
	 */
	public int trimClean(int expect) {
		Node<K, V> p;
		CacheNode<K, V> node;
		int over = size();
		int i = 0;
		int unreachables = 0;
		synchronized (lruLock()) {
			// 先分离空闲的项
			p = m_LruTail.lruBefore;
			for (; i < over && p != m_LruHead && null != p && unreachables < expect; i++) {
				node = (CacheNode<K, V>) p;
				p = p.lruBefore;
				if (node.isUnreachable()) {
					// 太好了，有能直接移除的
					removeNode(node.hash, node.key, null, false);
					++unreachables;
				} else {
					// 只好先分离
					node.detach();
				}
			}
			if (unreachables < expect) {
				if (i < over) {
					// LRU表有问题？
					fixLru(i);
					return unreachables;
				}
				// 还没有达到期望移除的项数，再试一次
				over = size();
				Node<K, V> end = p;
				p = m_LruTail.lruBefore;
				for (i = 0; i < over && p != end && p != m_LruHead && null != p && unreachables < expect; i++) {
					node = (CacheNode<K, V>) p;
					p = p.lruBefore;
					if (node.isUnreachable()) {
						// 移除
						removeNode(node.hash, node.key, null, false);
						++unreachables;
					}
				}
			}
		}
		if (unreachables > 0) {
			if (0 == size()) {
				clear();
			}
			_Logger.info("{unreachables:" + unreachables + ",expect:" + expect + "}" + this);
		}
		return unreachables;
	}

	@Override
	public int trim(int expect) {
		if (isReachable()) {
			return trimClean(expect);
		}

		int count = 0;
		Node<K, V> p;
		CacheNode<K, V> node;
		int i = 0;
		synchronized (lruLock()) {
			int over = size();
			p = m_LruTail.lruBefore;
			for (; i < over && p != m_LruHead && null != p && count < expect; i++) {
				node = (CacheNode<K, V>) p;
				p = p.lruBefore;
				if (!node.isDirty()) {
					removeNode(node.hash, node.key, null, false);
					++count;
				}
			}
			if (i < over && count < expect) {
				// LRU表有问题？
				fixLru(i);
			}
		}
		if (count > 0) {
			_Logger.info("{trim:" + count + ",expect:" + expect + "}" + this);
			if (0 == size()) {
				clear();
			}
		}
		return count;
	}

	/**
	 * 清理长时间未访问的空闲缓存项
	 * 
	 * @param lastAccess
	 *            清除的时间点（小于它则被清除）
	 * @return 已清除的项数
	 */
	protected int idleTrim(int lastAccess) {
		int i = 0;
		Node<K, V> p;
		CacheNode<K, V> node;
		int detachs = 0;
		int unreachables = 0;
		int over = size();
		long ts = System.currentTimeMillis();
		synchronized (lruLock()) {
			if (null == m_LruTail.lruBefore || m_LruHead == m_LruTail.lruBefore) {
				// 链表空
				return 0;
			}
			if (isReachable()) {
				// 要确认缓存项是否被引用情况下
				p = m_LruTail.lruBefore;
				// 先分离空闲的项
				for (; i < over && p != m_LruHead && null != p; i++) {
					node = (CacheNode<K, V>) p;
					if (node.lastAccess > lastAccess) {
						// 前面的都是早于lastAccess访问过
						break;
					}
					p = p.lruBefore;
					if (node.isUnreachable()) {
						removeNode(node.hash, node.key, null, false);
						++unreachables;
					} else if (null != node.detach()) {
						++detachs;
					}
				}
				// 再移除不可达的项
				for (; p != m_LruTail && null != p;) {
					node = (CacheNode<K, V>) p;
					p = p.lruAfter;
					if (node.isUnreachable()) {
						removeNode(node.hash, node.key, null, false);
						++unreachables;
					}
				}
			} else {
				// 直接按LRU移除
				p = m_LruTail.lruBefore;
				for (; i < over && p != m_LruHead && null != p; i++) {
					node = (CacheNode<K, V>) p;
					if (node.lastAccess > lastAccess) {
						// 前面的都是早于lastAccess访问过
						break;
					}
					p = p.lruBefore;
					if (!node.isDirty()) {
						removeNode(node.hash, node.key, null, false);
						++unreachables;
					}
				}
			}
		}
		if (0 == size()) {
			clear();
		}
		if (((detachs > 0 || unreachables > 0) && _Logger.isInfoEnabled()) || _Logger.isTraceEnabled()) {
			_Logger.info("idleTrim{name:" + getName() + ",detachs:" + detachs + ",size:" + size() + ",unreachables:"
					+ unreachables + ",elapse:" + (System.currentTimeMillis() - ts) + ",t:" + _clock.getTicker()
					+ ",tl:" + ((CacheNode<K, V>) m_LruTail).lastAccess + "}");
		}
		return unreachables;
	}

	/**
	 * 在一个同步块内（防止并发）加载缓存项
	 * 
	 * @param node
	 *            要加载的缓存项
	 * @param loader
	 *            加载器
	 * @param removeFail
	 *            若加载失败移该缓存项
	 * @return 加载完的缓存值
	 */
	private V load(CacheNode<K, V> node, Loader<K, V> loader, boolean removeFail) {
		V v;
		// 进入加载状态
		long ts = System.currentTimeMillis();
		long timeout;
		synchronized (node) {
			try {
				if (m_PendingTimeout > 0) {
					for (;;) {
						timeout = System.currentTimeMillis() - ts;
						if (timeout < m_PendingTimeout) {
							v = node.pending(m_PendingTimeout - timeout);
							break;
						}
						// 超时了
						throw new TimeoutException(
								"[" + getName() + "]等待加载失败或超时(" + (System.currentTimeMillis() - ts) + ")" + node);
					}
				} else {
					v = node.pending(0);
				}
			} catch (InterruptedException e) {
				// _Logger.warn(exist, e);
				// 恢复线程的中断标志
				Thread.currentThread().interrupt();
				throw new AbortException("[" + getName() + "]等待加载被中断(" + (System.currentTimeMillis() - ts) + ")" + node,
						e);
			}

			if (!node.isPending(v)) {
				if (node.isReady(v, 0)) {
					// 看来有另一个线程已抢先加载好，捡了现成
					return v;
				}
				// 什么情况？
				_Logger.warn("[" + getName() + "]等待后继续加载(" + (System.currentTimeMillis() - ts) + ")" + node);
			}
		}

		// 加载
		boolean succ = false;
		int maxLoadConcurrent = m_MaxLoadConcurrent;
		try {
			if (maxLoadConcurrent > 0) {
				if (m_LoadConcurrent.incrementAndGet() > maxLoadConcurrent) {
					throw new OverloadException("[" + getName() + "]加载并发过多(" + maxLoadConcurrent + ") " + node);
				}
			}
			++m_LoadCount;
			v = loader.load(node.key, node);
			succ = true;
		} finally {
			if (maxLoadConcurrent > 0) {
				m_LoadConcurrent.decrementAndGet();
			}

			synchronized (node) {
				if (succ) {
					// 加载成功
					node.ready(v);
				} else {
					// 加载失败
					// node.fail();
					node.reinit();
				}
				// 无论如何也要通知啊（否则其它并发的线程就真的只能是死等）
				node.notifyAll();
			}
			if (succ) {
				afterNodeLoad(node);
			} else if (removeFail) {
				// 加载失败，移除这项
				removeNode(node.hash, node.key, null, false);
			}
		}
		return v;
	}

	protected CacheNode<K, V> getLruHead() {
		return (CacheNode<K, V>) m_LruHead;
	}

	@Override
	protected CacheNode<K, V> openNode(int hash, K key) {
		return (CacheNode<K, V>) super.openNode(hash, key);
	}

	protected CacheNode<K, V> getNode(int hash, Object key) {
		return (CacheNode<K, V>) super.getNode(hash, key);
	}

	@Override
	protected Node<K, V> newNode(int hash, K key, V value, Node<K, V> next) {
		return new CacheNode<K, V>(hash, key, value, next);
	}

	/**
	 * 缓存项加载后
	 * 
	 * @param node
	 *            加载的项
	 */
	protected void afterNodeLoad(CacheNode<K, V> node) {
		putLru(node);
	}

	@Override
	protected void afterNodeUpdate(Node<K, V> p) {
		super.afterNodeUpdate(p);
		synchronized (updatedLock()) {
			CacheNode<K, V> node = (CacheNode<K, V>) p;
			m_UpdatedChain = node.update(m_UpdatedChain);
		}
	}

	protected void afterNodeRemoval(Node<K, V> p) {
		super.afterNodeRemoval(p);
		CacheNode<K, V> node = (CacheNode<K, V>) p;
		node.discard();
	}

	protected void afterNodeAccess(Node<K, V> p) {
		++m_GetCount;
		super.afterNodeAccess(p);
	}

	protected Object updatedLock() {
		return this;
	}

	/**
	 * 取得已更新待刷写的链表
	 * 
	 * @param copyOn
	 *            是否复制链表（可减少刷写期间过长导致的缓冲访问阻塞）
	 * @param limit
	 *            限制项数
	 * @return 链表
	 */
	@SuppressWarnings("unchecked")
	protected DirtyData<V> getDirtyData(boolean copyOn, int limit) {
		if (null == m_UpdatedChain) {
			return (DirtyData<V>) _emptyDirtyData;
		}
		if (copyOn) {
			return new CopyOn(limit);
		}
		return new Direct(limit);
	}

	/**
	 * 缓存命中率（百分比）
	 */
	public int getHitRate() {
		long hits = (m_GetCount - m_LoadCount);
		if (hits > 0) {
			return (int) ((hits * 100) / m_GetCount);
		}
		return 0;
	}

	@Override
	public String toString() {
		return "{name:" + getName() + ",size:" + size() + ",capacity:" + capacity() + ",threshold:" + m_Threshold
				+ ",mod:" + m_ModCount + ",height:" + m_Height + ",get:" + m_GetCount + ",load:" + m_LoadCount + ",HR:"
				+ getHitRate() + "}";
	}

	/**
	 * 用于加载缓存值项的加载器
	 * 
	 * @author liangyi
	 * 
	 * @param <K>
	 *            键的类型（通常是字串）
	 * @param <V>
	 *            值的类型
	 */
	public static interface Loader<K, V> {
		/**
		 * 加载
		 * 
		 * @param key
		 *            要加载项的key
		 * @param node
		 *            缓存项节点
		 * @return 加载回来的值
		 */
		V load(K key, CacheNode<K, V> node);
	}

	/**
	 * 用于更新缓存值项的更新器
	 * 
	 * @author liangyi
	 * 
	 * @param <K>
	 *            键的类型（通常是字串）
	 * @param <V>
	 *            值的类型
	 */
	public static interface Updater<K, V> {
		/**
		 * 更新
		 * 
		 * @param key
		 *            要更新项的key
		 * @param current
		 *            当前的值
		 * @return 更新后的值
		 */
		V update(K key, V current);
	}

	/**
	 * 
	 * @author liangyi
	 *
	 * @param <K>
	 * @param <V>
	 */
	public static class CacheNode<K, V> extends Node<K, V> {
		/** 以弱引用检查缓存值是否不可达到（用于确认没有其它引用，保证缓存的对象单例化） */
		protected volatile WeakReference<V> reference;
		/** 最后访问的时间戳 */
		protected int lastAccess;
		/** 最后就绪/加载的时间戳 */
		protected int lastReady;
		/** 更新链表下一项 */
		protected CacheNode<K, V> updatedNext;

		@SuppressWarnings("unchecked")
		public CacheNode(int hash, K key, V value, Node<K, V> next) {
			super(hash, key, value, next);
			reference = (WeakReference<V>) _unassigned;
		}

		public V detach() {
			// if ( _dirty == reference || _pending == reference || null==value)
			// {
			// return;
			// }
			synchronized (this) {
				if (_dirty == reference || _pending == reference || null == value) {
					return value;
				}
				reference = new WeakReference<V>(value);
				value = null;
				return reference.get();
			}
		}

		/**
		 * 取得缓存项的值，若处于分离状态则尝试转回正常的（强）引用状态
		 */
		@SuppressWarnings("unchecked")
		public V getValue() {
			V v = value;
			if (null != v || null == reference || _unassigned == reference || _pending == reference
					|| _dirty == reference) {
				// 缓存值不为null（是可达的），或没有弱引用（缓存项的值就是null），或还在加载中（也只好返回null），或已修改
				return v;
			}

			// 进入同步块来切换引用状态
			synchronized (this) {
				v = value;
				if (null != v || null == reference || _pending == reference || isDirty()) {
					return v;
				}
				// 缓存值已清除，有弱引用，尝试恢复
				v = reference.get();
				if (null != v) {
					// 已恢复到可达
					value = v;
					// reference.clear();
					reference = null;
					return v;
				} else {
					// 引用已丢失，重置为未加载
					reference = (WeakReference<V>) _unassigned;
				}
			}
			// 缓存项的值估计已经丢失
			return null;
		}

		/**
		 * 取得缓存项的值，若处于分离状态不恢复强引用只直接返回值
		 */
		public V getValueFast() {
			V v = value;
			if (null != v) {
				return v;
			}
			WeakReference<V> r = reference;
			return (null == r) ? null : r.get();
		}

		@SuppressWarnings("unchecked")
		synchronized public void reinit() {
			this.value = null;
			this.reference = (WeakReference<V>) _unassigned;
			this.lastReady = 0;
			this.updatedNext = null;
		}

		synchronized public void discard() {
			if (isDirty()) {
				return;
			}
			reinit();
		}

		@SuppressWarnings("unchecked")
		@Override
		synchronized public V setValue(V v) {
			V old = this.value;
			this.value = v;
			reference = (WeakReference<V>) _dirty;
			lastReady = _clock.getTicker();
			lastAccess = lastReady;
			return old;
		}

		@SuppressWarnings("unchecked")
		synchronized public void dirty() {
			reference = (WeakReference<V>) _dirty;
			lastReady = _clock.getTicker();
			lastAccess = lastReady;
		}

		/**
		 * 标记缓存项已刷写
		 */
		synchronized public void clean() {
			if (_dirty == reference) {
				reference = null;
			}
			this.updatedNext = null;
		}

		/**
		 * 标记到更新链表
		 * 
		 * @param linked
		 *            更新链表头
		 * @return 链表头
		 */
		synchronized public CacheNode<K, V> update(CacheNode<K, V> linked) {
			if (null == updatedNext && this != linked) {
				updatedNext = linked;
				return this;
			}
			return linked;
		}

		synchronized public void ready(V v) {
			// if (_pending == reference || _unassigned == reference) {
			this.value = v;
			reference = null;
			lastReady = _clock.getTicker();
			lastAccess = lastReady;
			// }
		}

		@SuppressWarnings("unchecked")
		synchronized public V pending(long timeout) throws InterruptedException {
			if (_pending == reference) {
				// 已经是加载中，等吧
				if (timeout > 0) {
					wait(timeout);
				} else {
					wait();
				}
			}
			if (_unassigned == reference) {
				reference = (WeakReference<V>) _pending;
				return null;
			} else if (_pending == reference) {
				return null;
			}
			return getValue();
		}

		public boolean isDirty() {
			return _dirty == reference || null != updatedNext;
		}

		public boolean isReady(V v, int timeout) {
			if (v == value && _unassigned != reference && _pending != reference) {
				if (_dirty == reference) {
					return true;
				}
				if (timeout <= 0 || _clock.getTicker() < (this.lastReady + timeout)) {
					return true;
				}
			}
			return false;
		}

		synchronized public boolean reloadNull(int timeout) {
			if (null != value || _dirty == reference) {
				// 已有值或修改过值，返回true
				return true;
			}
			if (_unassigned == reference || _pending == reference) {
				// 未加载或加载中，返回false;
				return false;
			}

			// if (null == reference) {
			// 已经加载，但值是null
			if (timeout <= 0 || _clock.getTicker() < (this.lastReady + timeout)) {
				// 未到重加载的时间点，继续用吧
				return true;
			}
			// 重初始化回未加载状态
			reinit();
			// }
			return false;
		}

		public boolean isPending(V v) {
			return (_pending == reference && v == value);
		}

		public boolean isUnreachable() {
			return (null == value && (null == reference
					|| (_dirty != reference && _pending != reference && null == reference.get())));
		}

		public CacheNode<K, V> getLruAfter() {
			return (CacheNode<K, V>) this.lruAfter;
		}

		public CacheNode<K, V> getLruBefore() {
			return (CacheNode<K, V>) this.lruBefore;
		}

		public CacheNode<K, V> getUpdatedNext() {
			return this.updatedNext;
		}

		public int getLastAccess() {
			return lastAccess;
		}

		public int getLastReady() {
			return lastReady;
		}

		protected boolean putLru(Node<K, V> chain) {
			if (super.putLru(chain)) {
				lastAccess = _clock.getTicker();
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			int tick = _clock.getTicker();
			return "{k:" + key + ",t:" + (tick - lastAccess) + ",r:" + (tick - lastReady) + ",v:" + value + '}';
		}
	}

	static class MarkReference extends WeakReference<Object> {
		String m_Name;

		public MarkReference(String name) {
			super(null);
		}

		@Override
		public String toString() {
			return m_Name;
		}
	}

	/**
	 * 待保存的缓存项
	 * 
	 * @author liangyi
	 *
	 */
	public interface DirtyData<V> extends Iterator<V> {
		/**
		 * 在遍历待保存项前执行其开启rollback/commit支持
		 */
		void begin();

		/**
		 * 若使用begin开始，则当存储到数据库完成执行其
		 * 
		 * @return 确认的项数
		 */
		int commit();

		/**
		 * 当存储到数据库出错时回滚则调用其把计数项标记回刷写列表，必须在开始时执行了begin
		 */
		void rollback();

		/**
		 * 当存储到数据库只有前面的部分成功时调用其把余下项标记回刷写列表，不需要开始时执行begin
		 */
		void abort();
	}

	static class EmptyDirtyData implements DirtyData<Object> {
		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException("empty");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}

		@Override
		public void begin() {
		}

		@Override
		public int commit() {
			return 0;
		}

		@Override
		public void rollback() {
		}

		@Override
		public void abort() {
		}
	}

	/**
	 * 复制模式下
	 */
	protected class CopyOn extends SinglyLinkedLifo<CacheNode<K, V>> implements DirtyData<V> {
		protected SinglyLinkedLifo.Node<CacheNode<K, V>> m_Next;

		public CopyOn(int limit) {
			CacheNode<K, V> p, next;
			synchronized (updatedLock()) {
				if (limit <= 0) {
					limit = size();
				}
				p = m_UpdatedChain;
				int i;
				for (i = 0; i < limit && null != p; i++) {
					next = p.getUpdatedNext();
					if (p.isDirty()) {
						addHead(p);
					}
					p.clean();
					p = next;
				}
				m_UpdatedChain = p;
				if (null != p && i >= size()) {
					// 难道更新链表循环了？
					_Logger.warn("更新链表异常[" + i + "]" + LruCache.this.toString());
				}
			}
		}

		public void reset() {
			m_Next = getHead();
		}

		@Override
		public boolean hasNext() {
			return null != m_Next;
		}

		@Override
		public V next() {
			return nextNode().getValueFast();
		}

		@Override
		public void begin() {
			// 啥也不用做
		}

		@Override
		public int commit() {
			int count = size();
			// 未遍历的部分标记回列表
			abort();
			return count;
		}

		@Override
		public void rollback() {
			// 全部标记回
			reset();
			abort();
		}

		@Override
		public void abort() {
			// 把后面的标记回待刷写列表
			while (hasNext()) {
				afterNodeUpdate(nextNode());
			}
			clear();
		}

		private CacheNode<K, V> nextNode() {
			Node<CacheNode<K, V>> p = m_Next;
			if (null == p) {
				throw new NoSuchElementException("没有啦");
			}
			m_Next = p.getNext();
			return p.value;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}

	/**
	 * 直接模式
	 */
	protected class Direct implements DirtyData<V> {
		protected CacheNode<K, V> m_First, m_Next;
		protected int m_Limit;
		protected int m_Position;

		public Direct(int limit) {
			m_First = m_Next = m_UpdatedChain;
			m_Limit = (limit <= 0) ? size() : limit;
		}

		@Override
		public boolean hasNext() {
			return null != m_Next && m_Position < m_Limit;
		}

		@Override
		public V next() {
			CacheNode<K, V> p = nextNode();
			return p.getValueFast();
		}

		@Override
		public void begin() {
			// 啥也不用做
		}

		@Override
		public void rollback() {
			m_Next = m_First;
		}

		@Override
		public void abort() {
			clean();
		}

		@Override
		public int commit() {
			clean();
			return m_Position;
		}

		/*
		 * 把first->next的项标记为已存储
		 */
		private void clean() {
			CacheNode<K, V> p, next;
			p = next = m_First;
			while (null != next && next != m_Next) {
				p = next;
				next = p.getUpdatedNext();
				p.clean();
			}
			if (m_First == m_UpdatedChain) {
				m_UpdatedChain = m_Next;
			} else {
				// 不应该！难道没有在updatedLock下？
				_Logger.warn("更新链表数据异常 " + LruCache.this);
			}
			m_First = null;
			m_Next = null;
		}

		private CacheNode<K, V> nextNode() {
			CacheNode<K, V> p = m_Next;
			if (null == p) {
				throw new NoSuchElementException("没有啦");
			}
			++m_Position;
			m_Next = p.getUpdatedNext();
			return p;
		}

		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}

	public V putIfAbsent(K key, V value) {
		V old;
		CacheNode<K, V> node = openNode(hash(key), key);
		synchronized (node) {
			old = node.getValue();
			if (null != old) {
				return old;
			}
			try {
				old = node.pending(0);
			} catch (InterruptedException e) {
				throw new AbortException(e);
			}
			if (null != old) {
				return old;
			}
			old = value;
			node.setValue(value);
		}
		afterNodeUpdate(node);
		return old;
	}
}