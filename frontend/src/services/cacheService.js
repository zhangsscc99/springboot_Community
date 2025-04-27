// frontend/src/services/cacheService.js
// 缓存服务 - 处理个人页面和帖子数据的缓存

const CACHE_PREFIX = 'community_cache_';
const CACHE_EXPIRY = 15 * 60 * 1000; // 缓存过期时间: 15分钟

// 缓存键类型
const CACHE_TYPES = {
  USER_POSTS: 'user_posts',
  USER_LIKES: 'user_likes', 
  USER_FAVORITES: 'user_favorites',
  USER_PROFILE: 'user_profile'
};

/**
 * 缓存服务
 */
const cacheService = {
  /**
   * 生成缓存键
   * @param {string} type 缓存类型
   * @param {string|number} userId 用户ID
   * @param {Object} params 附加参数
   * @returns {string} 缓存键
   */
  generateKey(type, userId, params = {}) {
    const paramsStr = Object.keys(params).length > 0 
      ? '_' + JSON.stringify(params)
      : '';
    return `${CACHE_PREFIX}${type}_${userId}${paramsStr}`;
  },

  /**
   * 设置缓存
   * @param {string} key 缓存键
   * @param {any} data 要缓存的数据
   */
  setCache(key, data) {
    try {
      const cacheData = {
        data,
        timestamp: Date.now()
      };
      localStorage.setItem(key, JSON.stringify(cacheData));
      console.log(`缓存数据已保存: ${key}`);
    } catch (error) {
      console.error('缓存数据失败:', error);
      // 如果存储失败（可能是存储空间不足），清除部分旧缓存
      this.clearOldCache();
    }
  },

  /**
   * 获取缓存
   * @param {string} key 缓存键
   * @returns {any|null} 缓存数据或null（如果缓存不存在或已过期）
   */
  getCache(key) {
    try {
      const cachedItem = localStorage.getItem(key);
      if (!cachedItem) return null;
      
      const cache = JSON.parse(cachedItem);
      const now = Date.now();
      
      // 检查缓存是否过期
      if (now - cache.timestamp > CACHE_EXPIRY) {
        console.log(`缓存已过期: ${key}`);
        localStorage.removeItem(key);
        return null;
      }
      
      console.log(`从缓存获取数据: ${key}`);
      return cache.data;
    } catch (error) {
      console.error('读取缓存失败:', error);
      return null;
    }
  },

  /**
   * 清除特定用户的缓存
   * @param {string|number} userId 用户ID
   */
  clearUserCache(userId) {
    try {
      Object.values(CACHE_TYPES).forEach(type => {
        const key = this.generateKey(type, userId);
        localStorage.removeItem(key);
      });
      console.log(`已清除用户 ${userId} 的所有缓存`);
    } catch (error) {
      console.error('清除用户缓存失败:', error);
    }
  },

  /**
   * 清除特定类型的缓存
   * @param {string} type 缓存类型
   * @param {string|number} userId 用户ID
   */
  clearTypeCache(type, userId) {
    try {
      const key = this.generateKey(type, userId);
      localStorage.removeItem(key);
      console.log(`已清除缓存: ${key}`);
    } catch (error) {
      console.error('清除缓存失败:', error);
    }
  },

  /**
   * 清除旧缓存
   * 当存储空间不足时调用
   */
  clearOldCache() {
    try {
      const keysToRemove = [];
      const now = Date.now();
      
      // 查找所有社区相关的缓存
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i);
        if (key && key.startsWith(CACHE_PREFIX)) {
          try {
            const cachedItem = localStorage.getItem(key);
            const cache = JSON.parse(cachedItem);
            
            // 删除过期缓存或超过一天的缓存
            if (now - cache.timestamp > CACHE_EXPIRY || 
                now - cache.timestamp > 24 * 60 * 60 * 1000) {
              keysToRemove.push(key);
            }
          } catch (e) {
            // 如果解析失败，也删除这个键
            keysToRemove.push(key);
          }
        }
      }
      
      // 删除找到的缓存
      keysToRemove.forEach(key => localStorage.removeItem(key));
      console.log(`已清除 ${keysToRemove.length} 个旧缓存项`);
    } catch (error) {
      console.error('清除旧缓存失败:', error);
    }
  }
};

// 导出缓存类型常量和缓存服务
export { CACHE_TYPES };
export default cacheService; 