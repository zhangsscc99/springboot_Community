/* eslint-disable */
import { createStore } from 'vuex'
import apiService from '@/services/apiService'
import cacheService, { CACHE_TYPES } from '@/services/cacheService'

// 缓存有效期（毫秒）- 2分钟
const CACHE_EXPIRATION = 2 * 60 * 1000;

export default createStore({
  state: {
    user: null,
    isAuthenticated: false,
    posts: [],
    currentPost: null,
    loading: false,
    actionLoading: false,
    error: null,
    activeTab: '关注', // 默认选中"关注"栏目
    tabPosts: {
      '关注': [],
      '推荐': [],
      '热榜': [],
      '故事': [],
      '情感知识': []
    },
    // 新增帖子缓存
    postCache: {}, // 以帖子ID为键存储帖子数据
    lastCacheUpdate: {}, // 记录每个帖子最后更新时间
    tabCacheUpdate: {}, // 记录每个标签页最后更新时间
    cachedPages: {}, // 用于存储各页面数据
    cachedScrollPositions: {}, // 用于存储各页面滚动位置
    requestCancelTokens: {}, // 用于存储取消标记
    currentComments: [],
    commentError: null,
    // 用户关注相关状态
    userFollowState: {}, // 以用户ID为键存储关注状态 {userId: {isFollowing: true/false, followersCount: 0, followingCount: 0}}
    followLoading: false, // 关注操作加载状态
    followError: null, // 关注操作错误信息
    // 私信相关状态
    unreadMessageCount: 0 // 未读私信数量
  },
  getters: {
    isAuthenticated: state => state.isAuthenticated,
    currentUser: state => state.user,
    allPosts: state => state.posts,
    currentPost: state => state.currentPost,
    isLoading: state => state.loading,
    isActionLoading: state => state.actionLoading,
    error: state => state.error,
    activeTab: state => state.activeTab,
    currentTabPosts: state => state.tabPosts[state.activeTab] || [],
    // 获取缓存中的帖子
    getCachedPost: state => id => state.postCache[id],
    // 检查缓存是否过期
    isCacheExpired: state => id => {
      const lastUpdate = state.lastCacheUpdate[id];
      if (!lastUpdate) return true;
      return Date.now() - lastUpdate > CACHE_EXPIRATION;
    },
    // 检查标签缓存是否过期
    isTabCacheExpired: state => tab => {
      const lastUpdate = state.tabCacheUpdate[tab];
      if (!lastUpdate) return true;
      return Date.now() - lastUpdate > CACHE_EXPIRATION;
    },
    // 获取未读消息数
    unreadMessageCount: state => state.unreadMessageCount
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
      // 更新标签缓存时间
      state.tabCacheUpdate[tab] = Date.now();
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
      // 添加到缓存
      state.postCache[post.id] = post;
      state.lastCacheUpdate[post.id] = Date.now();
    },
    SET_CURRENT_POST(state, post) {
      state.currentPost = post;
      // 更新缓存
      if (post && post.id) {
        state.postCache[post.id] = post;
        state.lastCacheUpdate[post.id] = Date.now();
      }
    },
    SET_LOADING(state, status) {
      state.loading = status;
    },
    SET_ACTION_LOADING(state, status) {
      state.actionLoading = status;
    },
    SET_ERROR(state, error) {
      state.error = error;
    },
    // 新增缓存相关mutation
    UPDATE_POST_CACHE(state, post) {
      if (!post || !post.id) return;
      state.postCache[post.id] = post;
      state.lastCacheUpdate[post.id] = Date.now();
    },
    // 更新帖子中的特定字段（如点赞数）
    UPDATE_POST_FIELD(state, { postId, field, value }) {
      // 更新缓存
      if (state.postCache[postId]) {
        state.postCache[postId][field] = typeof value === 'function' 
          ? value(state.postCache[postId]) 
          : value;
      }
      // 更新当前帖子
      if (state.currentPost && state.currentPost.id === postId) {
        state.currentPost[field] = typeof value === 'function' 
          ? value(state.currentPost) 
          : value;
      }
      // 更新列表中的帖子
      const postInList = state.posts.find(p => p.id === postId);
      if (postInList) {
        postInList[field] = typeof value === 'function' 
          ? value(postInList) 
          : value;
      }
      // 更新标签页中的帖子
      Object.keys(state.tabPosts).forEach(tab => {
        const post = state.tabPosts[tab].find(p => p.id === postId);
        if (post) {
          post[field] = typeof value === 'function' 
            ? value(post) 
            : value;
        }
      });
    },
    // 保存页面数据
    CACHE_PAGE_DATA(state, { routePath, data }) {
      state.cachedPages[routePath] = data;
    },
    // 保存滚动位置
    SAVE_SCROLL_POSITION(state, { routePath, position }) {
      state.cachedScrollPositions[routePath] = position;
    },
    // 取消特定标签的请求
    CANCEL_TAB_REQUEST(state, tab) {
      if (state.requestCancelTokens && state.requestCancelTokens[tab]) {
        state.requestCancelTokens[tab].cancel(`切换到新标签，取消 ${tab} 请求`);
        delete state.requestCancelTokens[tab];
      }
    },
    // 重置评论状态
    RESET_COMMENTS(state) {
      // 清空评论相关状态
      state.currentComments = [];
      state.commentError = null;
    },
    // 关注功能相关的mutations
    SET_FOLLOW_LOADING(state, loading) {
      state.followLoading = loading;
    },
    SET_FOLLOW_ERROR(state, error) {
      state.followError = error;
    },
    SET_USER_FOLLOW_STATE(state, { userId, followState }) {
      state.userFollowState = {
        ...state.userFollowState,
        [userId]: {
          ...(state.userFollowState[userId] || {}),
          ...followState
        }
      };
    },
    // 设置未读消息数量
    setUnreadMessageCount(state, count) {
      state.unreadMessageCount = count;
    },
    // 增加未读消息数量
    incrementUnreadMessageCount(state) {
      state.unreadMessageCount += 1;
    },
    // 清空未读消息数量
    clearUnreadMessageCount(state) {
      state.unreadMessageCount = 0;
    }
  },
  actions: {
    // 初始化应用 - 检查存储的令牌并恢复会话
    initializeApp({ commit }) {
      console.log('初始化应用，检查登录状态...');
      const token = localStorage.getItem('token');
      
      if (token) {
        try {
          console.log('找到本地存储的令牌，尝试恢复会话');
          // 从localStorage恢复用户信息
          const userInfoStr = localStorage.getItem('userInfo');
          
          if (!userInfoStr) {
            console.warn('找到令牌但未找到用户信息，清除令牌');
            localStorage.removeItem('token');
            commit('SET_USER', null);
            return;
          }
          
          try {
            const userInfo = JSON.parse(userInfoStr);
            if (userInfo && userInfo.id && userInfo.username) {
              console.log('成功恢复用户会话:', userInfo.username);
              commit('SET_USER', userInfo);
              
              // 可以添加令牌验证逻辑
              // apiService.auth.validateToken().catch(() => {
              //   console.warn('令牌验证失败，清除会话');
              //   localStorage.removeItem('token');
              //   localStorage.removeItem('userInfo');
              //   commit('SET_USER', null);
              // });
            } else {
              console.warn('用户信息格式不正确，清除会话');
              localStorage.removeItem('token');
              localStorage.removeItem('userInfo');
              commit('SET_USER', null);
            }
          } catch (parseError) {
            console.error('解析用户信息失败:', parseError);
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            commit('SET_USER', null);
          }
        } catch (error) {
          console.error('应用初始化失败:', error);
          localStorage.removeItem('token');
          localStorage.removeItem('userInfo');
          commit('SET_USER', null);
        }
      } else {
        console.log('未找到存储的令牌，用户未登录');
        commit('SET_USER', null);
      }
    },
  
    // User authentication actions
    async login({ commit }, credentials) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      
      try {
        console.log(`尝试登录用户: ${credentials.username || credentials.email}`);
        
        // 调用登录API
        const response = await apiService.auth.login(credentials);
        console.log('登录API响应:', response);
        
        // 从响应中获取数据
        const { data } = response;
        console.log('后端返回数据:', data);
        
        // 获取令牌 - 针对实际的API响应格式调整
        const token = data.token || data.accessToken;
        
        if (!token) {
          throw new Error('登录成功但未获取到令牌');
        }
        
        console.log('成功获取到令牌');
        
        // 构建用户信息对象
        const userInfo = {
          id: data.id,
          username: data.username,
          nickname: data.nickname,
          email: data.email,
          avatar: data.avatar,
          token: token,
          roles: data.roles,
          followingCount: data.followingCount || 0,
          followerCount: data.followerCount || 0
        };
        
        // 存储令牌和用户信息
        localStorage.setItem('token', token);
        localStorage.setItem('userInfo', JSON.stringify(userInfo));
        localStorage.setItem('userId', data.id);
        
        console.log('用户登录成功，ID已保存:', data.id);
        
        // 更新状态
        commit('SET_USER', userInfo);
        commit('SET_LOADING', false);
        
        return userInfo;
      } catch (error) {
        console.error('登录失败:', error);
        commit('SET_ERROR', error.message || '登录失败，请检查您的凭据');
        commit('SET_LOADING', false);
        throw error;
      }
    },
    
    // 用户注册
    async register({ commit }, userData) {
      commit('SET_LOADING', true);
      commit('SET_ERROR', null);
      
      try {
        console.log('准备注册用户:', {
          username: userData.username,
          email: userData.email,
          password: userData.password ? '[已隐藏]' : '空'
        });
        
        // 调用API进行注册
        const response = await apiService.auth.register(userData);
        console.log('注册成功，响应数据:', response.data);
        
        commit('SET_ERROR', null);
        return response.data;
      } catch (error) {
        console.error('注册过程中发生错误:', error);
        
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
          
          console.error('API错误响应:', {
            status,
            data: responseData
          });
        } else if (error.request) {
          errorMessage = '服务器无响应，请检查网络连接';
          console.error('未收到响应:', error.request);
        } else {
          errorMessage = `请求错误: ${error.message}`;
          console.error('请求错误:', error.message);
        }
        
        commit('SET_ERROR', errorMessage);
        throw error;
      } finally {
        // 确保无论成功还是失败都会设置loading为false
        commit('SET_LOADING', false);
      }
    },
    
    logout({ commit }) {
      // 获取当前用户ID（如果有）
      const currentUser = JSON.parse(localStorage.getItem('userInfo') || '{}');
      const userId = currentUser?.id;
      
      // 清除本地存储中的所有用户数据
      localStorage.removeItem('userInfo');
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      
      // 如果有用户ID，清除该用户的缓存
      if (userId) {
        cacheService.clearUserCache(userId);
      }
      
      // 提交mutation
      commit('SET_USER', null);
    },
    
    setActiveTab({ commit, dispatch }, tab) {
      commit('SET_ACTIVE_TAB', tab);
      // 加载选定栏目的帖子
      dispatch('fetchPostsByTab', tab);
    },
    
    // Post related actions - 从后端API获取帖子
    async fetchPosts({ commit, state, getters }) {
      commit('SET_LOADING', true);
      try {
        console.log('从API获取所有帖子');
        const response = await apiService.posts.getAll();
        const posts = response.data.content || response.data;
        
        // 更新所有帖子并写入缓存
        posts.forEach(post => {
          commit('UPDATE_POST_CACHE', post);
        });
        
        commit('SET_POSTS', posts);
        commit('SET_ERROR', null);
        
        // 获取默认栏目的帖子
        await this.dispatch('fetchPostsByTab', '关注');
      } catch (error) {
        console.error('获取帖子失败:', error);
        commit('SET_ERROR', error.message || '获取帖子失败');
        
        // 如果API调用失败，使用空数组
        commit('SET_POSTS', []);
        Object.keys(state.tabPosts).forEach(tab => {
          commit('SET_TAB_POSTS', { tab, posts: [] });
        });
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    // 获取特定栏目的帖子
    async fetchPostsByTab({ commit, state, getters }, tab, options = {}) {
      // 如果标签缓存未过期且有数据，直接使用缓存数据
      if (!getters.isTabCacheExpired(tab) && state.tabPosts[tab] && state.tabPosts[tab].length > 0) {
        console.log(`使用缓存中的${tab}栏目帖子`);
        return state.tabPosts[tab];
      }
      
      // 存储当前正在处理的标签请求
      const currentTabRequest = tab;
      
      commit('SET_LOADING', true);
      try {
        console.log(`从API获取${tab}栏目的帖子`);
        
        // 创建一个可取消的请求
        const CancelToken = apiService.getCancelToken();
        const source = CancelToken.source();
        
        // 存储取消标记，以便在用户切换标签时可以取消此请求
        if (options.cancelToken) {
          state.requestCancelTokens = state.requestCancelTokens || {};
          state.requestCancelTokens[options.cancelToken] = source;
        }
        
        const response = await apiService.posts.getByTab(tab, { cancelToken: source.token });
        const posts = response.data.content || response.data;
        
        // 添加调试信息 - 检查后端返回数据中是否有bio字段
        console.log(`[API Debug] Response received for tab ${tab}:`, response.data);
        if (posts && posts.length > 0) {
          const firstPost = posts[0];
          console.log(`[API Debug] First post:`, firstPost);
          if (firstPost.author) {
            console.log(`[API Debug] First post author:`, firstPost.author);
            console.log(`[API Debug] Bio field exists: ${firstPost.author.bio !== undefined}`);
            if (firstPost.author.bio !== undefined) {
              console.log(`[API Debug] Bio value: "${firstPost.author.bio}"`);
            }
          }
        }
        
        // 检查当前激活标签是否仍然是发起请求时的标签
        if (state.activeTab !== tab) {
          console.log(`标签已切换，丢弃 ${tab} 的响应数据`);
          return [];
        }
        
        // 更新标签帖子并写入缓存
        posts.forEach(post => {
          commit('UPDATE_POST_CACHE', post);
        });
        
        commit('SET_TAB_POSTS', { tab, posts });
        commit('SET_ERROR', null);
        return posts;
      } catch (error) {
        if (apiService.isCancel(error)) {
          console.log(`请求 ${tab} 被取消`);
          return [];
        }
        
        console.error(`获取${tab}栏目帖子失败:`, error);
        commit('SET_ERROR', error.message || `获取${tab}栏目帖子失败`);
        
        // 如果API调用失败，使用空数组
        commit('SET_TAB_POSTS', { tab, posts: [] });
        return [];
      } finally {
        // 只有当当前激活标签仍然是请求发起时的标签时，才将加载状态设为false
        if (state.activeTab === currentTabRequest) {
          commit('SET_LOADING', false);
        }
        
        // 清除取消标记
        if (options.cancelToken && state.requestCancelTokens && state.requestCancelTokens[options.cancelToken]) {
          delete state.requestCancelTokens[options.cancelToken];
        }
      }
    },
    
    async fetchPostById({ commit, state, getters }, postId) {
      // 首先尝试从缓存获取
      const cachedPost = getters.getCachedPost(postId);
      if (cachedPost && !getters.isCacheExpired(postId)) {
        console.log(`从缓存获取帖子ID: ${postId}`);
        commit('SET_CURRENT_POST', cachedPost);
        commit('SET_ERROR', null);
        
        // 在后台异步刷新缓存，但不等待结果
        setTimeout(async () => {
          try {
            const response = await apiService.posts.getById(postId);
            const freshPost = response.data;
            commit('UPDATE_POST_CACHE', freshPost);
          } catch (error) {
            console.log('后台刷新缓存失败:', error);
          }
        }, 0);
        
        return cachedPost;
      }
      
      commit('SET_LOADING', true);
      try {
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
    },
    
    // 点赞帖子
    async likePost({ commit, state }, postId) {
      try {
        commit('SET_ACTION_LOADING', true);
        
        // 乐观更新UI
        commit('UPDATE_POST_FIELD', { postId, field: 'likedByCurrentUser', value: true });
        commit('UPDATE_POST_FIELD', { postId, field: 'likes', value: post => post.likes + 1 });
        
        // 调用API
        await apiService.posts.like(postId);
        console.log('帖子点赞成功');
        
        // 清除用户相关缓存，确保喜欢的帖子列表会更新
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_LIKES, state.user.id);
        }
        
        return true;
      } catch (error) {
        console.error('点赞失败:', error);
        
        // 回滚UI更新
        commit('UPDATE_POST_FIELD', { postId, field: 'likedByCurrentUser', value: false });
        commit('UPDATE_POST_FIELD', { postId, field: 'likes', value: post => Math.max(0, post.likes - 1) });
        
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 取消点赞
    async unlikePost({ commit, state }, postId) {
      try {
        commit('SET_ACTION_LOADING', true);
        
        // 乐观更新UI
        commit('UPDATE_POST_FIELD', { postId, field: 'likedByCurrentUser', value: false });
        commit('UPDATE_POST_FIELD', { postId, field: 'likes', value: post => Math.max(0, post.likes - 1) });
        
        // 调用API
        await apiService.posts.unlike(postId);
        console.log('取消点赞成功');
        
        // 清除用户相关缓存，确保喜欢的帖子列表会更新
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_LIKES, state.user.id);
        }
        
        return true;
      } catch (error) {
        console.error('取消点赞失败:', error);
        
        // 回滚UI更新
        commit('UPDATE_POST_FIELD', { postId, field: 'likedByCurrentUser', value: true });
        commit('UPDATE_POST_FIELD', { postId, field: 'likes', value: post => post.likes + 1 });
        
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 收藏帖子
    async favoritePost({ commit, state }, postId) {
      try {
        commit('SET_ACTION_LOADING', true);
        
        // 乐观更新UI
        commit('UPDATE_POST_FIELD', { postId, field: 'favoritedByCurrentUser', value: true });
        commit('UPDATE_POST_FIELD', { postId, field: 'favorites', value: post => post.favorites + 1 });
        
        // 调用API
        await apiService.posts.favorite(postId);
        console.log('收藏帖子成功');
        
        // 清除用户相关缓存，确保收藏的帖子列表会更新
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_FAVORITES, state.user.id);
        }
        
        return true;
      } catch (error) {
        console.error('收藏帖子失败:', error);
        
        // 回滚UI更新
        commit('UPDATE_POST_FIELD', { postId, field: 'favoritedByCurrentUser', value: false });
        commit('UPDATE_POST_FIELD', { postId, field: 'favorites', value: post => Math.max(0, post.favorites - 1) });
        
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 取消收藏
    async unfavoritePost({ commit, state }, postId) {
      try {
        commit('SET_ACTION_LOADING', true);
        
        // 乐观更新UI
        commit('UPDATE_POST_FIELD', { postId, field: 'favoritedByCurrentUser', value: false });
        commit('UPDATE_POST_FIELD', { postId, field: 'favorites', value: post => Math.max(0, post.favorites - 1) });
        
        // 调用API
        await apiService.posts.unfavorite(postId);
        console.log('取消收藏成功');
        
        // 清除用户相关缓存，确保收藏的帖子列表会更新
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_FAVORITES, state.user.id);
        }
        
        return true;
      } catch (error) {
        console.error('取消收藏失败:', error);
        
        // 回滚UI更新
        commit('UPDATE_POST_FIELD', { postId, field: 'favoritedByCurrentUser', value: true });
        commit('UPDATE_POST_FIELD', { postId, field: 'favorites', value: post => post.favorites + 1 });
        
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 获取评论（支持分页）
    async fetchComments({ commit }, { postId, page = 0, size = 10 }) {
      commit('SET_LOADING', true);
      try {
        console.log(`获取帖子 ${postId} 的评论，页码: ${page}, 大小: ${size}`);
        const response = await apiService.comments.getCommentsByPostId(postId, page, size);
        const comments = response.data;
        commit('SET_ERROR', null);
        return comments;
      } catch (error) {
        console.error(`获取评论失败:`, error);
        commit('SET_ERROR', error.message || '获取评论失败');
        return [];
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    // 创建回复
    async createReply({ commit }, { commentId, replyData }) {
      commit('SET_ACTION_LOADING', true);
      try {
        console.log(`为评论 ${commentId} 创建回复:`, replyData);
        const response = await apiService.comments.createReply(commentId, replyData);
        commit('SET_ERROR', null);
        return response.data;
      } catch (error) {
        console.error(`创建回复失败:`, error);
        commit('SET_ERROR', error.message || '发表回复失败');
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 删除评论
    async deleteComment({ commit }, commentId) {
      commit('SET_ACTION_LOADING', true);
      try {
        console.log(`删除评论 ${commentId}`);
        await apiService.comments.delete(commentId);
        commit('SET_ERROR', null);
        return true;
      } catch (error) {
        console.error(`删除评论失败:`, error);
        commit('SET_ERROR', error.message || '删除评论失败');
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 删除帖子
    async deletePost({ commit, state }, postId) {
      commit('SET_ACTION_LOADING', true);
      try {
        console.log(`删除帖子 ${postId}`);
        const response = await apiService.posts.delete(postId);
        console.log('帖子删除成功:', response);
        
        // 从缓存中移除帖子 - 使用Vue 3兼容的方式
        if (state.postCache[postId]) {
          delete state.postCache[postId];
        }
        
        // 从标签页中移除帖子
        Object.keys(state.tabPosts).forEach(tab => {
          if (state.tabPosts[tab]) {
            commit('SET_TAB_POSTS', { 
              tab, 
              posts: state.tabPosts[tab].filter(post => post.id !== postId) 
            });
          }
        });
        
        commit('SET_ERROR', null);
        return true;
      } catch (err) {
        // 使用可选链操作符安全地访问错误对象属性
        const errorMessage = err?.message || '删除帖子失败';
        console.error(`删除帖子失败:`, {
          error: err,
          postId: postId,
          message: errorMessage,
          status: err?.status,
          responseData: err?.data
        });
        commit('SET_ERROR', errorMessage);
        
        // 即使发生错误，帖子可能已经被删除
        // 有时服务器会返回500错误但仍然执行了删除操作
        if (err?.status === 500) {
          console.warn("服务器返回500错误，但帖子可能已被删除。尝试从本地缓存移除...");
          
          // 尝试从本地缓存中移除 - 使用Vue 3兼容的方式
          if (state.postCache[postId]) {
            delete state.postCache[postId];
          }
          
          // 尝试从标签页中移除
          Object.keys(state.tabPosts).forEach(tab => {
            if (state.tabPosts[tab]) {
              commit('SET_TAB_POSTS', { 
                tab, 
                posts: state.tabPosts[tab].filter(post => post.id !== postId) 
              });
            }
          });
        }
        
        throw err;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 点赞评论
    async likeComment({ commit }, commentId) {
      commit('SET_ACTION_LOADING', true);
      try {
        console.log(`点赞评论 ${commentId}`);
        await apiService.comments.like(commentId);
        commit('SET_ERROR', null);
        return true;
      } catch (error) {
        console.error(`点赞评论失败:`, error);
        commit('SET_ERROR', error.message || '点赞评论失败');
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 取消点赞评论
    async unlikeComment({ commit }, commentId) {
      commit('SET_ACTION_LOADING', true);
      try {
        console.log(`取消点赞评论 ${commentId}`);
        await apiService.comments.unlike(commentId);
        commit('SET_ERROR', null);
        return true;
      } catch (error) {
        console.error(`取消点赞评论失败:`, error);
        commit('SET_ERROR', error.message || '取消点赞评论失败');
        throw error;
      } finally {
        commit('SET_ACTION_LOADING', false);
      }
    },
    
    // 获取页面数据，优先使用缓存
    fetchPageData({ state, commit }, { routePath, fetchFunction }) {
      if (state.cachedPages[routePath]) {
        return Promise.resolve(state.cachedPages[routePath]);
      }
      
      return fetchFunction().then(data => {
        commit('CACHE_PAGE_DATA', { routePath, data });
        return data;
      });
    },
    
    // 获取特定栏目的帖子（预加载专用，不切换激活标签）
    async fetchTabDataWithoutActivating({ commit, state, getters }, tab) {
      try {
        console.log(`预加载 ${tab} 栏目的帖子数据`);
        const response = await apiService.posts.getByTab(tab);
        const posts = response.data.content || response.data;
        
        // 更新标签帖子并写入缓存，但不改变当前激活标签
        posts.forEach(post => {
          commit('UPDATE_POST_CACHE', post);
        });
        
        commit('SET_TAB_POSTS', { tab, posts });
        commit('SET_ERROR', null);
        return posts;
      } catch (error) {
        console.error(`预加载 ${tab} 栏目帖子失败:`, error);
        return [];
      }
    },
    
    // 重置评论状态 - 路由切换时调用
    resetComments({ commit }) {
      commit('RESET_COMMENTS');
      commit('SET_ERROR', null); // 同时清除全局错误状态
    },
    
    // 关注用户
    async followUser({ commit, state }, userId) {
      commit('SET_FOLLOW_LOADING', true);
      commit('SET_FOLLOW_ERROR', null);
      
      try {
        const response = await apiService.users.follow(userId);
        commit('SET_USER_FOLLOW_STATE', { 
          userId, 
          followState: { 
            isFollowing: true,
            followersCount: response.data?.followersCount || 0
          } 
        });
        
        // 清除用户缓存
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, state.user.id);
        }
        cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
        
        return response;
      } catch (error) {
        console.error('Failed to follow user:', error);
        commit('SET_FOLLOW_ERROR', error.message || 'Failed to follow user');
        throw error;
      } finally {
        commit('SET_FOLLOW_LOADING', false);
      }
    },
    
    // 取消关注用户
    async unfollowUser({ commit, state }, userId) {
      commit('SET_FOLLOW_LOADING', true);
      commit('SET_FOLLOW_ERROR', null);
      
      try {
        const response = await apiService.users.unfollow(userId);
        commit('SET_USER_FOLLOW_STATE', { 
          userId, 
          followState: { 
            isFollowing: false,
            followersCount: response.data?.followersCount || 0
          } 
        });
        
        // 清除用户缓存
        if (state.user) {
          cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, state.user.id);
        }
        cacheService.clearTypeCache(CACHE_TYPES.USER_PROFILE, userId);
        
        return response;
      } catch (error) {
        console.error('Failed to unfollow user:', error);
        commit('SET_FOLLOW_ERROR', error.message || 'Failed to unfollow user');
        throw error;
      } finally {
        commit('SET_FOLLOW_LOADING', false);
      }
    },
    
    // 获取用户关注状态
    async getUserFollowState({ commit }, userId) {
      commit('SET_FOLLOW_LOADING', true);
      
      try {
        const response = await apiService.users.checkFollowing(userId);
        commit('SET_USER_FOLLOW_STATE', { 
          userId, 
          followState: { 
            isFollowing: response.data?.isFollowing || false,
            followersCount: response.data?.followersCount || 0,
            followingCount: response.data?.followingCount || 0
          } 
        });
        return response.data;
      } catch (error) {
        console.error('Failed to get user follow state:', error);
        commit('SET_FOLLOW_ERROR', error.message || 'Failed to get user follow state');
        throw error;
      } finally {
        commit('SET_FOLLOW_LOADING', false);
      }
    }
  },
  modules: {
  }
}) 