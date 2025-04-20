<template>
  <div class="create-post-view">
    <div class="post-container">
      <div class="post-header">
        <button class="back-button" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <h2 class="page-title">{{ isEditMode ? '编辑帖子' : '创建帖子' }}</h2>
        <div class="spacer"></div>
      </div>
      
      <form class="post-form" @submit.prevent="submitPost">
        <div class="form-group">
          <input 
            type="text" 
            v-model="postData.title" 
            placeholder="标题（必填）" 
            class="post-title-input"
            required
            maxlength="100"
            @input="updateTitleCount"
          >
          <div class="character-count">{{ titleCount }}/100</div>
        </div>
        
        <div class="form-group">
          <textarea 
            v-model="postData.content" 
            placeholder="分享你的想法..." 
            class="post-content-textarea"
            required
            rows="10"
          ></textarea>
        </div>
        
        <div class="form-group">
          <label class="form-label">选择栏目</label>
          <div class="tab-selector">
            <button 
              v-for="tab in availableTabs" 
              :key="tab" 
              type="button"
              :class="['tab-button', postData.tab === tab ? 'active' : '']"
              @click="selectTab(tab)"
            >
              {{ tab }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label class="form-label">添加标签（可选）</label>
          <div class="tags-input-container">
            <div class="tags-list">
              <div 
                v-for="(tag, index) in tags" 
                :key="index" 
                class="tag-item"
              >
                {{ tag }}
                <button type="button" class="remove-tag-btn" @click="removeTag(index)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </div>
            <div class="tag-input-wrapper">
              <input 
                type="text" 
                v-model="tagInput" 
                placeholder="输入标签，按Enter添加" 
                class="tag-input"
                @keyup.enter="addTag"
              >
              <button 
                type="button" 
                class="add-tag-btn" 
                @click="addTag"
                :disabled="!tagInput.trim()"
              >
                添加
              </button>
            </div>
          </div>
        </div>
        
        <div class="form-actions">
          <button 
            type="button" 
            class="cancel-btn" 
            @click="goBack"
          >
            取消
          </button>
          <button 
            type="submit" 
            class="submit-btn" 
            :disabled="loading || !isFormValid"
          >
            <span v-if="loading">
              <i class="fas fa-spinner fa-spin"></i> 处理中...
            </span>
            <span v-else>
              {{ isEditMode ? '保存修改' : '发布' }}
            </span>
          </button>
        </div>
        
        <div v-if="error" class="form-error">
          {{ error }}
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import apiService from '@/services/apiService';

export default {
  name: 'CreatePostView',
  data() {
    return {
      isEditMode: false,
      postId: null,
      postData: {
        title: '',
        content: '',
        tab: '关注',
        tags: []
      },
      tagInput: '',
      tags: [],
      availableTabs: ['关注', '推荐', '热榜', '故事', '情感知识'],
      titleCount: 0,
      loading: false,
      error: null
    };
  },
  computed: {
    ...mapGetters({
      isAuthenticated: 'isAuthenticated',
      currentUser: 'currentUser'
    }),
    isFormValid() {
      return this.postData.title.trim() && 
             this.postData.content.trim() && 
             this.postData.tab;
    }
  },
  methods: {
    goBack() {
      this.$router.go(-1);
    },
    selectTab(tab) {
      this.postData.tab = tab;
    },
    updateTitleCount() {
      this.titleCount = this.postData.title.length;
    },
    addTag() {
      const tag = this.tagInput.trim();
      if (tag && !this.tags.includes(tag)) {
        if (this.tags.length < 5) {
          this.tags.push(tag);
          this.tagInput = '';
        } else {
          this.error = '最多添加5个标签';
          setTimeout(() => {
            this.error = null;
          }, 3000);
        }
      }
    },
    removeTag(index) {
      this.tags.splice(index, 1);
    },
    async submitPost() {
      if (!this.isFormValid) return;
      
      // 检查认证状态
      const token = localStorage.getItem('token');
      console.log('发帖前检查认证状态:', !!token);
      
      if (!token) {
        this.error = '需要登录才能发布帖子，请先登录';
        console.error('发帖失败：未找到认证令牌');
        
        // 清除可能无效的认证状态
        localStorage.removeItem('userInfo');
        this.$store.commit('SET_USER', null);
        
        setTimeout(() => {
          this.$router.push({ name: 'login', query: { redirect: '/create-post' } });
        }, 1500);
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.postData.tags = this.tags;
      
      try {
        console.log('准备发送帖子数据:', {
          title: this.postData.title,
          content: this.postData.content.substring(0, 30) + '...',
          tab: this.postData.tab,
          tags: Array.from(this.tags)
        });
        
        console.log('认证头: Bearer Token');
        
        let response;
        if (this.isEditMode) {
          response = await apiService.posts.update(this.postId, this.postData);
          console.log('帖子更新成功:', response.data);
        } else {
          // 直接使用axios发送认证请求
          const baseURL = apiService.getBaseURL ? apiService.getBaseURL() : 'http://communityapi.jinshuqingci.com';
          console.log('使用基础URL:', baseURL);
          
          response = await apiService.posts.create(this.postData);
          console.log('帖子创建成功:', response.data);
        }
        
        // 跳转到帖子详情页
        this.$router.push({
          name: 'post-detail',
          params: { id: response.data.id }
        });
      } catch (error) {
        console.error('提交帖子失败:', error);
        
        if (error.response) {
          const status = error.response.status;
          if (status === 401 || status === 403) {
            this.error = '认证失败，请重新登录';
            // 清除无效的认证信息
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            this.$store.commit('SET_USER', null);
            
            setTimeout(() => {
              this.$router.push({ name: 'login', query: { redirect: '/create-post' } });
            }, 1500);
          } else {
            // 修复：添加安全检查，确保data和message存在
            const responseData = error.response.data || {};
            this.error = responseData.message || '服务器错误，请稍后再试';
          }
        } else if (error.request) {
          this.error = '无法连接到服务器，请检查您的网络连接';
        } else {
          this.error = '发生错误: ' + (error.message || '未知错误');
        }
      } finally {
        this.loading = false;
      }
    },
    async loadPostForEdit() {
      if (!this.postId) return;
      
      this.loading = true;
      
      try {
        const response = await apiService.posts.getById(this.postId);
        const post = response.data;
        
        this.postData.title = post.title;
        this.postData.content = post.content;
        this.postData.tab = post.tab;
        this.tags = post.tags || [];
        
        this.updateTitleCount();
      } catch (error) {
        console.error('加载帖子失败:', error);
        this.error = '无法加载帖子，请稍后再试';
      } finally {
        this.loading = false;
      }
    }
  },
  created() {
    // 检查是否已登录
    const token = localStorage.getItem('token');
    
    if (!this.isAuthenticated || !token) {
      console.warn('用户未登录或令牌失效，重定向到登录页面');
      this.$router.push({ name: 'login', query: { redirect: '/create-post' } });
      return;
    }
    
    console.log('用户已登录，令牌有效');
    
    // 检查是否为编辑模式
    if (this.$route.params.id) {
      this.isEditMode = true;
      this.postId = this.$route.params.id;
      this.loadPostForEdit();
    }
  }
}
</script>

<style scoped>
.create-post-view {
  min-height: 100vh;
  background-color: var(--background-color);
  padding-bottom: 30px;
}

.post-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.back-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 5px;
  color: var(--text-color);
}

.page-title {
  flex: 1;
  text-align: center;
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.spacer {
  width: 24px;
}

.post-form {
  background-color: var(--card-background);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.form-group {
  margin-bottom: 20px;
  position: relative;
}

.post-title-input {
  width: 100%;
  padding: 12px 15px;
  font-size: 18px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background-color: var(--input-background);
}

.post-content-textarea {
  width: 100%;
  padding: 15px;
  font-size: 16px;
  line-height: 1.6;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  resize: vertical;
  min-height: 200px;
  background-color: var(--input-background);
}

.character-count {
  position: absolute;
  right: 10px;
  top: 10px;
  font-size: 12px;
  color: var(--light-text-color);
}

.form-label {
  display: block;
  margin-bottom: 10px;
  font-weight: 500;
  color: var(--text-color);
}

.tab-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.tab-button {
  padding: 8px 15px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background-color: var(--background-color);
  color: var(--text-color);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-button.active {
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border-color: transparent;
}

.tags-input-container {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px;
  background-color: var(--input-background);
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  background-color: var(--tag-background);
  color: var(--text-color);
  padding: 5px 10px;
  border-radius: 16px;
  font-size: 14px;
}

.remove-tag-btn {
  background: none;
  border: none;
  margin-left: 5px;
  cursor: pointer;
  font-size: 12px;
  color: var(--light-text-color);
}

.tag-input-wrapper {
  display: flex;
}

.tag-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-right: none;
  border-radius: 4px 0 0 4px;
  background-color: var(--card-background);
}

.add-tag-btn {
  padding: 8px 12px;
  background-color: var(--secondary-color);
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.add-tag-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
  margin-top: 30px;
}

.cancel-btn {
  padding: 10px 20px;
  background-color: var(--background-color);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  cursor: pointer;
}

.submit-btn {
  padding: 10px 30px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
  transition: transform 0.2s;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.form-error {
  margin-top: 15px;
  color: var(--error-color);
  text-align: center;
}

@media (max-width: 768px) {
  .post-container {
    padding: 15px;
  }
  
  .tab-selector {
    gap: 8px;
  }
  
  .tab-button {
    padding: 6px 12px;
    font-size: 14px;
  }
  
  .form-actions {
    flex-direction: column;
    gap: 10px;
  }
  
  .cancel-btn, .submit-btn {
    width: 100%;
  }
}
</style> 