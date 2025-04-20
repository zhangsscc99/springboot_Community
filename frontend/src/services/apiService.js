import axios from 'axios';

// 创建axios实例
const apiClient = axios.create({
  baseURL: 'http://localhost:8083', // 确保这个端口与后端一致
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // 启用跨域cookie
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
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
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
    }
  },
  
  // 帖子相关
  posts: {
    getAll() {
      return apiClient.get('/api/posts');
    },
    getByTab(tab) {
      return apiClient.get(`/api/posts/tab/${tab}`);
    },
    getById(id) {
      return apiClient.get(`/api/posts/${id}`);
    },
    create(postData) {
      return apiClient.post('/api/posts', postData);
    },
    update(id, postData) {
      return apiClient.put(`/api/posts/${id}`, postData);
    },
    delete(id) {
      return apiClient.delete(`/api/posts/${id}`);
    }
  },
  
  // 评论相关
  comments: {
    getByPostId(postId) {
      return apiClient.get(`/api/posts/${postId}/comments`);
    },
    create(postId, commentData) {
      return apiClient.post(`/api/posts/${postId}/comments`, commentData);
    },
    delete(commentId) {
      return apiClient.delete(`/api/comments/${commentId}`);
    }
  }
};

export default apiService; 
