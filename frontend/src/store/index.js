/* eslint-disable */
import { createStore } from 'vuex'
import apiService from '@/services/apiService'

export default createStore({
  state: {
    user: null,
    isAuthenticated: false,
    posts: [],
    currentPost: null,
    loading: false,
    error: null,
    activeTab: '关注', // 默认选中"关注"栏目
    tabPosts: {
      '关注': [],
      '推荐': [],
      '热榜': [],
      '故事': [],
      '情感知识': []
    }
  },
  getters: {
    isAuthenticated: state => state.isAuthenticated,
    currentUser: state => state.user,
    allPosts: state => state.posts,
    currentPost: state => state.currentPost,
    isLoading: state => state.loading,
    error: state => state.error,
    activeTab: state => state.activeTab,
    currentTabPosts: state => state.tabPosts[state.activeTab] || []
  },
  mutations: {
    SET_USER(state, user) {
      state.user = user;
      state.isAuthenticated = !!user;
    },
    SET_POSTS(state, posts) {
      state.posts = posts;
    },
    SET_TAB_POSTS(state, { tab, posts }) {
      state.tabPosts[tab] = posts;
    },
    SET_ACTIVE_TAB(state, tab) {
      state.activeTab = tab;
    },
    ADD_POST(state, post) {
      state.posts.unshift(post);
      // 同时添加到当前激活的栏目
      if (state.tabPosts[state.activeTab]) {
        state.tabPosts[state.activeTab].unshift(post);
      }
    },
    SET_CURRENT_POST(state, post) {
      state.currentPost = post;
    },
    SET_LOADING(state, status) {
      state.loading = status;
    },
    SET_ERROR(state, error) {
      state.error = error;
    }
  },
  actions: {
    // 初始化应用 - 检查存储的令牌并恢复会话
    initializeApp({ commit }) {
      const token = localStorage.getItem('token');
      if (token) {
        try {
          // 这里可以添加验证令牌的逻辑
          // 理想情况下，应该调用后端API验证令牌是否有效
          
          // 从localStorage恢复用户信息（如果有的话）
          const userInfo = JSON.parse(localStorage.getItem('userInfo'));
          if (userInfo) {
            commit('SET_USER', userInfo);
          }
        } catch (error) {
          console.error('Failed to initialize app:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('userInfo');
        }
      }
    },
  
    // User authentication actions
    async login({ commit }, credentials) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      
      try {
        // 使用apiService进行登录
        console.log('Attempting login with:', {
          username: credentials.username || credentials.email,
          password: credentials.password ? '[REDACTED]' : 'empty'
        });
        
        const response = await apiService.auth.login({
          username: credentials.username || credentials.email,
          password: credentials.password
        });
        
        console.log('Login response received:', {
          status: response.status,
          headers: response.headers,
          data: response.data
        });
        
        // 检查响应格式
        const data = response.data;
        if (!data || !data.token && !data.accessToken) {
          console.error('API返回的数据缺少token字段:', data);
          throw new Error('服务器返回的数据格式不正确');
        }
        
        console.log('Login successful, response data:', data);
        
        // 获取token - 适应不同的返回格式
        const token = data.token || data.accessToken;
        if (!token) {
          throw new Error('无法获取认证令牌');
        }
        
        // 构建用户信息对象 - 确保字段名与后端返回一致
        const userInfo = {
          id: data.id,
          username: data.username,
          email: data.email,
          avatar: data.avatar || 'https://via.placeholder.com/40',
          token: token,
          roles: data.roles || []
        };
        
        // 保存token和用户信息到localStorage
        localStorage.setItem('token', token);
        localStorage.setItem('userInfo', JSON.stringify(userInfo));
        
        // 更新状态
        commit('SET_USER', userInfo);
        commit('SET_ERROR', null);
        return userInfo;
      } catch (error) {
        console.error('Login error in store:', error);
        
        let errorMessage = '登录失败，请稍后再试';
        
        if (error.response) {
          const status = error.response.status;
          const responseData = error.response.data;
          
          console.error('API error response:', {
            status,
            data: responseData,
            headers: error.response.headers
          });
          
          if (status === 401) {
            errorMessage = '用户名或密码错误，请检查后重试';
          } else if (status === 403) {
            errorMessage = '账号没有登录权限';
          } else if (status === 500) {
            errorMessage = `服务器内部错误: ${responseData.message || '请联系管理员'}`;
            console.error('服务器内部错误详情:', responseData);
          } else if (responseData && responseData.message) {
            errorMessage = responseData.message;
          }
        } else if (error.request) {
          // 请求已发出，但没有收到响应
          errorMessage = '服务器无响应，请检查网络连接';
          console.error('No response received:', error.request);
        } else {
          // 请求设置时出现了错误
          errorMessage = `请求错误: ${error.message}`;
          console.error('Request error:', error.message);
        }
        
        commit('SET_ERROR', errorMessage);
        throw error;
      } finally {
        // 确保无论成功还是失败都会设置loading为false
        commit('SET_LOADING', false);
      }
    },
    
    // 用户注册
    async register({ commit }, userData) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      
      try {
        console.log('Attempting registration with:', {
          username: userData.username,
          email: userData.email,
          password: userData.password ? '[REDACTED]' : 'empty'
        });
        
        // 调用API进行注册
        const response = await apiService.auth.register(userData);
        console.log('Registration successful, response:', response.data);
        
        commit('SET_ERROR', null);
        return response.data;
      } catch (error) {
        console.error('Registration error in store:', error);
        
        let errorMessage = '注册失败，请稍后再试';
        
        if (error.response) {
          const status = error.response.status;
          const responseData = error.response.data;
          
          if (status === 400) {
            if (responseData.message && responseData.message.includes('Username is already taken')) {
              errorMessage = '用户名已被占用';
            } else if (responseData.message && responseData.message.includes('Email is already in use')) {
              errorMessage = '该邮箱已被注册';
            } else if (responseData.message) {
              errorMessage = responseData.message;
            }
          }
          
          console.error('API error response:', {
            status,
            data: responseData
          });
        } else if (error.request) {
          errorMessage = '服务器无响应，请检查网络连接';
          console.error('No response received:', error.request);
        } else {
          errorMessage = `请求错误: ${error.message}`;
          console.error('Request error:', error.message);
        }
        
        commit('SET_ERROR', errorMessage);
        throw error;
      } finally {
        // 确保无论成功还是失败都会设置loading为false
        commit('SET_LOADING', false);
      }
    },
    
    logout({ commit }) {
      // 清除localStorage中的token和用户信息
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      commit('SET_USER', null);
    },
    
    setActiveTab({ commit, dispatch }, tab) {
      commit('SET_ACTIVE_TAB', tab);
      // 加载选定栏目的帖子
      dispatch('fetchPostsByTab', tab);
    },
    
    // Post related actions - 从后端API获取帖子
    async fetchPosts({ commit }) {
      commit('SET_LOADING', true);
      try {
        // 使用API服务获取所有帖子
        console.log('从API获取所有帖子');
        const response = await apiService.posts.getAll();
        const posts = response.data.content || response.data;
        
        commit('SET_POSTS', posts);
        commit('SET_ERROR', null);
        
        // 获取默认栏目的帖子
        await this.dispatch('fetchPostsByTab', '关注');
      } catch (error) {
        console.error('获取帖子失败:', error);
        commit('SET_ERROR', error.message || '获取帖子失败');
        
        // 如果API调用失败，使用缓存或空数组
        commit('SET_POSTS', []);
        Object.keys(state.tabPosts).forEach(tab => {
          commit('SET_TAB_POSTS', { tab, posts: [] });
        });
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    // 获取特定栏目的帖子
    async fetchPostsByTab({ commit, state }, tab) {
      commit('SET_LOADING', true);
      try {
        console.log(`从API获取 ${tab} 栏目的帖子`);
        let posts = [];
        
        // 使用API获取特定栏目的帖子
        if (tab === '关注' || tab === '推荐' || tab === '热榜' || tab === '故事' || tab === '情感知识') {
          const response = await apiService.posts.getByTab(tab);
          posts = response.data.content || response.data || [];
        }
        
        commit('SET_TAB_POSTS', { tab, posts });
        commit('SET_ERROR', null);
      } catch (error) {
        console.error(`获取 ${tab} 栏目帖子失败:`, error);
        commit('SET_ERROR', `获取 ${tab} 栏目内容失败: ${error.message}`);
        
        // 如果API调用失败，使用空数组
        commit('SET_TAB_POSTS', { tab, posts: [] });
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async fetchPostById({ commit }, postId) {
      commit('SET_LOADING', true);
      try {
        // 使用API获取特定帖子
        console.log(`从API获取帖子ID: ${postId}`);
        const response = await apiService.posts.getById(postId);
        const post = response.data;
        
        commit('SET_CURRENT_POST', post);
        commit('SET_ERROR', null);
        return post;
      } catch (error) {
        console.error(`获取帖子ID ${postId} 失败:`, error);
        commit('SET_ERROR', error.message || '获取帖子详情失败');
        return null;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async createPost({ commit, state, dispatch }, postData) {
      commit('SET_LOADING', true);
      try {
        console.log('创建新帖子:', postData);
        // 使用API创建帖子
        const response = await apiService.posts.create(postData);
        const newPost = response.data;
        
        console.log('新帖子创建成功:', newPost);
        
        // 添加到当前状态
        commit('ADD_POST', newPost);
        
        // 重新获取当前栏目的帖子，确保显示最新数据
        await dispatch('fetchPostsByTab', state.activeTab);
        
        commit('SET_ERROR', null);
        return newPost;
      } catch (error) {
        console.error('创建帖子失败:', error);
        
        let errorMessage = '发布失败，请稍后再试';
        if (error.response && error.response.data) {
          errorMessage = error.response.data.message || errorMessage;
        }
        
        commit('SET_ERROR', errorMessage);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    }
  },
  modules: {
  }
}) 