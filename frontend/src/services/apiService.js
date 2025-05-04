import axios from 'axios';
import apiConfig from '@/config/api.config';

// 创建axios实例
const apiClient = axios.create({
  baseURL: apiConfig.BASE_URL, // 使用配置中的API基础URL
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: false // 禁用跨域cookie，使用Authorization头代替
});

// 启用调试，在开发环境中查看完整的请求/响应
apiClient.interceptors.request.use(request => {
  console.log('Starting Request', {
    url: request.url,
    method: request.method,
    data: request.data,
    headers: request.headers
  });
  return request;
});

apiClient.interceptors.response.use(
  response => {
    console.log('Response:', response);
    return response;
  }, 
  error => {
    // 增强错误日志，帮助调试
    if (error.response) {
      console.error('API Error Response:', {
        status: error.response.status,
        data: error.response.data,
        headers: error.response.headers,
        config: error.config
      });
    } else if (error.request) {
      console.error('API Error Request:', error.request);
    } else {
      console.error('API Error Setup:', error.message);
    }
    console.error('Full error:', error);
    return Promise.reject(error);
  }
);

// 请求拦截器 - 添加认证Token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    console.log('发送请求到', config.url, '方法:', config.method);
    console.log('认证头:', token ? 'Bearer Token已添加' : '无Token');
    
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    } else {
      // 请求需要认证的端点但没有token时，给出明确的日志
      if (config.url.includes('/posts') && 
          (config.method === 'post' || config.method === 'put' || config.method === 'delete')) {
        console.warn('警告: 尝试访问需要认证的资源，但未找到Token');
      }
    }
    return config;
  },
  error => {
    console.error('请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器 - 处理常见错误
apiClient.interceptors.response.use(
  response => response,
  error => {
    // 安全地记录错误
    console.error('API 请求失败:', {
      url: error?.config?.url || 'unknown',
      method: error?.config?.method || 'unknown',
      statusCode: error?.response?.status || 'unknown',
      statusText: error?.response?.statusText || 'unknown',
      data: error?.response?.data || {}
    });

    // 401错误 - 未授权/Token过期
    if (error?.response?.status === 401) {
      console.error('认证失败：401 Unauthorized', error?.response?.data);
      localStorage.removeItem('token');
      // 如果需要，这里可以触发重定向到登录页面
    }
    
    // 处理500错误
    if (error?.response?.status === 500) {
      console.error('服务器错误:', error?.response?.data);
      console.error('请求URL:', error?.config?.url);
      console.error('请求方法:', error?.config?.method);
      console.error('请求数据:', error?.config?.data);
      
      // 特定处理帖子删除错误
      if (error?.config?.url?.includes('/posts/') && error?.config?.method === 'delete') {
        console.error('删除帖子失败:', {
          url: error?.config?.url,
          userId: localStorage.getItem('userId'),
          token: localStorage.getItem('token') ? '已设置' : '未设置'
        });
      }
    }
    
    // 确保返回一个格式化的错误，而不是原始错误对象
    // 这样可以防止未定义属性的问题
    const formattedError = new Error(
      error?.response?.data?.message || 
      error?.message || 
      '服务器连接错误'
    );
    
    // 附加额外信息以便调试
    formattedError.status = error?.response?.status;
    formattedError.data = error?.response?.data;
    formattedError.originalError = error;
    
    return Promise.reject(formattedError);
  }
);

// API服务
const apiService = {
  // 获取基础URL，用于调试
  getBaseURL() {
    return apiConfig.BASE_URL;
  },
  
  // 检查后端服务可用性
  checkBackendStatus() {
    return apiClient.get('/api/test/all')
      .then(response => {
        console.log('后端服务正常运行:', response.status);
        return { status: 'ok' };
      })
      .catch(error => {
        // 如果收到401或任何响应，说明服务器在运行
        if (error.response) {
          console.log('后端服务运行中但返回错误:', error.response.status);
          return { status: 'running', code: error.response.status };
        }
        // 否则可能是连接问题
        console.error('后端连接错误:', error.message);
        return { status: 'error', message: error.message };
      });
  },
  
  // 认证相关
  auth: {
    login(credentials) {
      // 使用正确的API路径 - 已确认/api/auth/login是工作的
      console.log('发送登录请求到 /api/auth/login:', credentials);
      return apiClient.post('/api/auth/login', {
        username: credentials.username || credentials.email,
        password: credentials.password
      });
    },
    register(userData) {
      // 确保发送所有必要的注册信息，但不发送角色信息
      console.log('发送注册请求到 /api/auth/register:', {
        username: userData.username,
        email: userData.email,
        password: '***隐藏***'
      });
      return apiClient.post('/api/auth/register', {
        username: userData.username,
        email: userData.email,
        password: userData.password
        // 不发送角色信息，让后端使用默认角色
      });
    },
    // 添加验证token的方法
    validateToken() {
      return apiClient.get('/api/auth/validate');
    }
  },
  
  // 用户相关
  users: {
    getProfile(userId, config = {}) {
      // 设置默认的 headers 禁用缓存
      const defaultConfig = {
        headers: {
          'Cache-Control': 'no-cache, no-store, must-revalidate',
          'Pragma': 'no-cache',
          'Expires': '0'
        }
      };
      
      // 合并配置
      const mergedConfig = { ...defaultConfig, ...config };
      
      return apiClient.get(`/api/users/${userId}`, mergedConfig);
    },
    updateProfile(userId, profileData) {
      return apiClient.put(`/api/users/${userId}`, profileData);
    },
    // 关注用户
    follow(userId) {
      console.log(`请求关注用户: ${userId}`);
      return apiClient.post(`/api/users/${userId}/follow`);
    },
    // 取消关注
    unfollow(userId) {
      console.log(`请求取消关注用户: ${userId}`);
      return apiClient.delete(`/api/users/${userId}/follow`);
    },
    // 获取用户关注列表
    getFollowing(userId, page = 0, size = 10) {
      return apiClient.get(`/api/users/${userId}/following`, {
        params: { page, size }
      });
    },
    // 获取用户粉丝列表
    getFollowers(userId, page = 0, size = 10) {
      return apiClient.get(`/api/users/${userId}/followers`, {
        params: { page, size }
      });
    },
    // 检查当前用户是否关注了某个用户
    checkFollowing(userId) {
      return apiClient.get(`/api/users/${userId}/follow/check`);
    },
    // 搜索用户
    search(query) {
      return apiClient.get(`/api/users/search`, {
        params: { query }
      });
    }
  },
  
  // 帖子相关
  posts: {
    // 获取所有帖子
    getAll(page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') {
      return apiClient.get('/api/posts', {
        params: { page, size, sortBy, sortDir }
      });
    },
    // 搜索帖子
    search(query, page = 0, size = 10) {
      return apiClient.get('/api/posts/search', {
        params: { query, page, size }
      });
    },
    // 根据标签获取帖子
    getByTab: (tab, config = {}) => {
      return apiClient.get(`/api/posts/tab/${tab}`, config);
    },
    // 获取单个帖子详情
    getById(id) {
      console.log(`正在请求帖子: /api/posts/${id}`);
      return apiClient.get(`/api/posts/${id}`);
    },
    // 创建新帖子
    create(postData) {
      return apiClient.post('/api/posts', postData);
    },
    // 更新帖子
    update(id, postData) {
      return apiClient.put(`/api/posts/${id}`, postData);
    },
    // 删除帖子
    delete(id) {
      return apiClient.delete(`/api/posts/${id}`);
    },
    // 点赞帖子
    like(id) {
      return apiClient.post(`/api/posts/${id}/like`);
    },
    // 取消点赞
    unlike(id) {
      return apiClient.delete(`/api/posts/${id}/like`);
    },
    // 收藏帖子
    favorite(id) {
      return apiClient.post(`/api/posts/${id}/favorite`);
    },
    // 取消收藏
    unfavorite(id) {
      return apiClient.delete(`/api/posts/${id}/favorite`);
    },
    // 获取用户帖子
    getByUserId(userId, page = 0, size = 10) {
      return apiClient.get(`/api/users/${userId}/posts`, {
        params: { page, size }
      });
    },
    // 获取用户点赞过的帖子
    getLikedByUserId(userId, page = 0, size = 10) {
      return apiClient.get(`/api/users/${userId}/likes`, {
        params: { page, size }
      });
    },
    // 获取用户收藏的帖子
    getFavoritedByUserId(userId, page = 0, size = 10) {
      return apiClient.get(`/api/users/${userId}/favorites`, {
        params: { page, size }
      });
    }
  },
  
  // 评论相关
  comments: {
    getCommentsByPostId(postId) {
      // 增强验证，确保postId有效，添加详细日志
      if (!postId) {
        console.error('无效的帖子ID: ID为空');
        return Promise.reject(new Error('无效的帖子ID'));
      }
    
      if (postId === 'undefined' || postId === 'null') {
        console.error(`无效的帖子ID: ID为字符串"${postId}"`);
        return Promise.reject(new Error('无效的帖子ID'));
      }
    
      console.log(`正在获取帖子 ${postId} 的评论`);
      return apiClient.get(`/api/posts/${postId}/comments`);
    },
    create(postId, commentData) {
      console.log(`发送评论到服务器：/api/posts/${postId}/comments`, commentData);
      
      // 添加详细日志以帮助调试
      console.log('发送详细信息:', {
        url: `/api/posts/${postId}/comments`,
        data: commentData,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': localStorage.getItem('token') ? `Bearer ${localStorage.getItem('token')}` : 'None'
        }
      });
      
      // 确保数据结构符合后端期望
      const formattedData = {
        content: commentData.content,
        parentId: commentData.parentId || null,
        replyToUserId: commentData.replyToUserId || null,
        postId: postId // 确保包含帖子ID
      };
    
      return apiClient.post(`/api/posts/${postId}/comments`, formattedData);
    },
    // 创建对评论的回复 - 使用通用的评论创建接口，只是添加parentId
    createReply(commentId, replyData) {
      console.log(`创建对评论 ${commentId} 的回复`, replyData);
      
      // 确保有帖子ID
      if (!replyData.postId) {
        console.error('回复中缺少帖子ID');
        return Promise.reject(new Error('回复中缺少帖子ID'));
      }
      
      // 使用常规的评论创建API，但增加parentId
      const formattedData = {
        content: replyData.content,
        parentId: commentId,
        replyToUserId: replyData.replyToUserId,
        postId: replyData.postId
      };
      
      return apiClient.post(`/api/posts/${replyData.postId}/comments`, formattedData);
    },
    delete(commentId) {
      return apiClient.delete(`/api/comments/${commentId}`);
    },
    like(commentId) {
      return apiClient.post(`/api/comments/${commentId}/like`);
    },
    unlike(commentId) {
      return apiClient.delete(`/api/comments/${commentId}/like`);
    },
    // 注意URL格式
    addComment(postId, content) {
      return apiClient.post(`/api/posts/${postId}/comments`, { content });
    }
  },
  
  // 添加这些方法
  getCancelToken: () => axios.CancelToken,
  isCancel: (error) => axios.isCancel(error)
};

export default apiService; 
