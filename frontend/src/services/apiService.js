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
    // 401错误 - 未授权/Token过期
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      // 如果需要，这里可以触发重定向到登录页面
    }
    return Promise.reject(error);
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
        console.log('Backend server is running:', response);
        return { status: 'ok' };
      })
      .catch(error => {
        // 如果收到401或任何响应，说明服务器在运行
        if (error.response) {
          console.log('Backend is running but returned an error:', error.response.status);
          return { status: 'running', code: error.response.status };
        }
        // 否则可能是连接问题
        console.error('Backend connection error:', error.message);
        return { status: 'error', message: error.message };
      });
  },
  
  // 认证相关
  auth: {
    login(credentials) {
      // 将表单的username值发送给后端
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
    getProfile(userId) {
      return apiClient.get(`/api/users/${userId}`);
    },
    updateProfile(userId, profileData) {
      return apiClient.put(`/api/users/${userId}`, profileData);
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
    getByPostId(postId, page = 0, size = 10) {
      // 确保这个URL路径是正确的，与后端匹配
      console.log(`正在请求评论: /api/posts/${postId}/comments`, { page, size });
      return apiClient.get(`/api/posts/${postId}/comments`, {
        params: { page, size }
      });
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
        // 其他必要的字段
      };
      
      return apiClient.post(`/api/posts/${postId}/comments`, formattedData);
    },
    createReply(commentId, replyData) {
      return apiClient.post(`/api/comments/${commentId}/replies`, replyData);
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
