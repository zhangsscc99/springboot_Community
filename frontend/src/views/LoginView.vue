<template>
  <div class="login-view">
    <div class="auth-container">
      <div class="auth-header">
        <button class="back-button" @click="goBack">
          <i class="fas fa-arrow-left"></i>
        </button>
        <h2 class="auth-title">登录</h2>
        <div class="spacer"></div>
      </div>
      
      <form class="auth-form" @submit.prevent="login">
        <div v-if="registrationSuccess" class="auth-success">
          注册成功！请使用您的新账号登录。
        </div>
        
        <!-- 添加系统诊断提示 -->
        <div v-if="diagnosticMode" class="auth-diagnostic">
          <h3>系统诊断</h3>
          <div class="diagnostic-item">
            <span>后端连接状态:</span>
            <span :class="{'status-ok': backendStatus === 'running', 'status-error': backendStatus === 'error', 'status-pending': backendStatus === 'checking'}">
              {{ backendStatusText }}
            </span>
          </div>
          <div class="diagnostic-buttons">
            <button type="button" class="diagnostic-btn" @click="checkBackendStatus">
              <i class="fas fa-sync"></i> 检查连接
            </button>
            <button type="button" class="diagnostic-btn" @click="clearLocalData">
              <i class="fas fa-trash"></i> 清除本地数据
            </button>
          </div>
          <div v-if="diagnosticResult" class="diagnostic-result" :class="{'result-success': diagnosticSuccess, 'result-error': !diagnosticSuccess}">
            <pre>{{ diagnosticResult }}</pre>
          </div>
        </div>
        
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="username" 
            placeholder="请输入用户名"
            required
            autocomplete="username"
            :class="{ 'error-input': errors.username }"
          >
          <span class="error" v-if="errors.username">{{ errors.username }}</span>
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <div class="password-input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              id="password" 
              v-model="password" 
              placeholder="请输入密码"
              required
              autocomplete="current-password"
              :class="{ 'error-input': errors.password }"
            >
            <i 
              :class="['password-toggle', showPassword ? 'fas fa-eye-slash' : 'fas fa-eye']" 
              @click="togglePasswordVisibility"
            ></i>
          </div>
          <span class="error" v-if="errors.password">{{ errors.password }}</span>
        </div>
        
        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="rememberMe">
            <span>记住我</span>
          </label>
          
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>
        
        <div v-if="localError" class="auth-error">
          {{ localError }}
        </div>
        
        <div class="form-actions">
          <button 
            type="submit" 
            class="auth-btn"
            :disabled="loading || !isFormValid"
          >
            <span v-if="loading">
              <i class="fas fa-spinner fa-spin"></i> 登录中...
            </span>
            <span v-else>登录</span>
          </button>
          <div class="auth-links">
            <router-link to="/register">没有账号？注册</router-link>
          </div>
          <!-- 添加诊断按钮 -->
          <div class="diagnostic-toggle" @click="toggleDiagnosticMode">
            <i class="fas fa-tools"></i> {{ diagnosticMode ? '关闭诊断' : '系统诊断' }}
          </div>
        </div>
      </form>
      
      <div class="auth-footer">
        <div class="auth-divider">
          <span>其他登录方式</span>
        </div>
        
        <div class="social-logins">
          <button class="social-btn">
            <i class="fab fa-weixin"></i>
          </button>
          <button class="social-btn">
            <i class="fab fa-qq"></i>
          </button>
          <button class="social-btn">
            <i class="fab fa-weibo"></i>
          </button>
        </div>
        
        <div class="register-link">
          还没有账号？ <router-link to="/register">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import apiService from '@/services/apiService';

export default {
  name: 'LoginView',
  data() {
    return {
      username: '',
      password: '',
      rememberMe: false,
      showPassword: false,
      localError: '',
      backendStatus: 'checking', // 'checking', 'running', 'error'
      errors: {
        username: '',
        password: ''
      },
      registrationSuccess: false,
      testResult: '',
      testSuccess: false,
      diagnosticMode: false,
      diagnosticResult: '',
      diagnosticSuccess: false
    };
  },
  computed: {
    ...mapGetters({
      loading: 'isLoading',
      error: 'error'
    }),
    isFormValid() {
      return this.username.trim() && this.password.trim();
    },
    backendStatusText() {
      switch(this.backendStatus) {
        case 'running': return '正常';
        case 'error': return '连接失败';
        case 'checking': return '检查中...';
        default: return '未知';
      }
    }
  },
  mounted() {
    this.checkBackendStatus();
    
    // 检查是否有注册成功的参数
    if (this.$route.params.registrationSuccess) {
      this.registrationSuccess = true;
      this.username = this.$route.params.username || '';
    }
  },
  methods: {
    async checkBackendStatus() {
      this.backendStatus = 'checking';
      
      try {
        // 使用 apiService 替代直接的 fetch 调用
        const result = await apiService.checkBackendStatus();
        
        if (result.status === 'ok' || result.status === 'running') {
          this.backendStatus = 'running';
        } else {
          this.backendStatus = 'error';
        }
      } catch (error) {
        console.error('Backend check failed:', error);
        this.backendStatus = 'error';
      }
    },
    togglePasswordVisibility() {
      this.showPassword = !this.showPassword;
    },
    goBack() {
      this.$router.go(-1);
    },
    async login() {
      if (!this.isFormValid) return;
      
      try {
        this.localError = '';
        
        const credentials = {
          username: this.username,
          password: this.password
        };
        
        console.log('准备登录用户:', this.username);
        
        // 使用store的login方法
        await this.$store.dispatch('login', credentials);
        
        console.log('登录成功');
        
        // 登录成功，根据重定向信息进行页面跳转
        const redirectPath = this.$route.query.redirect || '/';
        this.$router.push(redirectPath);
        
      } catch (error) {
        console.error('登录失败:', error);
        
        // 设置本地错误信息
        if (error.response) {
          const status = error.response.status;
          if (status === 401) {
            this.localError = '用户名或密码错误，请重试';
          } else if (status === 403) {
            this.localError = '账户被锁定或没有权限';
          } else {
            this.localError = `登录失败: ${error.response.data?.message || '请稍后再试'}`;
          }
        } else {
          this.localError = error.message || '登录失败，请稍后再试';
        }
      }
    },
    validateForm() {
      // 重置错误
      this.errors = {
        username: '',
        password: ''
      };
      
      // 验证用户名
      if (!this.username.trim()) {
        this.errors.username = '用户名不能为空';
      }
      
      // 验证密码
      if (!this.password.trim()) {
        this.errors.password = '密码不能为空';
      } else if (this.password.length < 6) {
        this.errors.password = '密码长度至少为6个字符';
      }
    },
    toggleDiagnosticMode() {
      this.diagnosticMode = !this.diagnosticMode;
      if (this.diagnosticMode) {
        this.checkBackendStatus();
      }
    },
    clearLocalData() {
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
      this.diagnosticResult = '本地存储的令牌和用户信息已清除';
      this.diagnosticSuccess = true;
    }
  }
}
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
}

.auth-container {
  flex: 1;
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  width: 100%;
}

.auth-header {
  display: flex;
  align-items: center;
  margin-bottom: 30px;
}

.back-button {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 5px;
  color: var(--text-color);
}

.auth-title {
  flex: 1;
  text-align: center;
  font-size: 20px;
  font-weight: 500;
  margin: 0;
}

.spacer {
  width: 24px;
}

.auth-form {
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text-color);
}

.form-group input {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.password-input-wrapper {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: 15px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: var(--light-text-color);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
}

.remember-me {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.remember-me input {
  margin-right: 6px;
}

.forgot-password {
  color: var(--primary-color);
  text-decoration: none;
}

.forgot-password:hover {
  text-decoration: underline;
}

.auth-submit-btn {
  width: 100%;
  padding: 12px;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.auth-submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(231, 76, 60, 0.3);
}

.auth-submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-error {
  margin: 15px 0;
  padding: 10px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #f5222d;
  font-size: 14px;
  text-align: center;
}

.auth-success {
  margin: 15px 0;
  padding: 10px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  color: #52c41a;
  font-size: 14px;
  text-align: center;
}

.auth-footer {
  text-align: center;
}

.auth-divider {
  position: relative;
  margin: 30px 0;
  text-align: center;
}

.auth-divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background-color: var(--border-color);
  z-index: 0;
}

.auth-divider span {
  position: relative;
  padding: 0 15px;
  background-color: var(--background-color);
  color: var(--light-text-color);
  font-size: 14px;
  z-index: 1;
}

.social-logins {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.social-btn {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background-color: white;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.social-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  color: white;
  border-color: transparent;
}

.social-btn i {
  color: #333;
}

.social-btn .fa-weixin {
  color: #07C160;
}

.social-btn .fa-qq {
  color: #12B7F5;
}

.social-btn .fa-weibo {
  color: #E6162D;
}

.register-link {
  font-size: 14px;
  color: var(--light-text-color);
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-link a:hover {
  text-decoration: underline;
}

.backend-test {
  margin-bottom: 20px;
  text-align: center;
}

.test-button {
  padding: 10px 20px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.test-button:hover {
  background-color: var(--primary-gradient-end);
}

.test-result {
  margin-top: 10px;
  padding: 10px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #f5222d;
  font-size: 14px;
  text-align: center;
}

.test-success {
  background-color: #f6ffed;
  border-color: #b7eb8f;
  color: #52c41a;
}

.test-error {
  background-color: #fff2f0;
  border-color: #ffccc7;
  color: #f5222d;
}

.auth-diagnostic {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border: 1px solid #e4e9f2;
  border-radius: 8px;
}

.auth-diagnostic h3 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  color: #333;
}

.diagnostic-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 14px;
}

.status-ok {
  color: #52c41a;
  font-weight: bold;
}

.status-error {
  color: #f5222d;
  font-weight: bold;
}

.status-pending {
  color: #faad14;
  font-weight: bold;
}

.diagnostic-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.diagnostic-btn {
  padding: 8px 12px;
  background-color: #f0f2f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
}

.diagnostic-btn:hover {
  background-color: #e6f7ff;
  border-color: #91d5ff;
}

.diagnostic-result {
  padding: 10px;
  max-height: 200px;
  overflow-y: auto;
  background-color: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.result-success {
  background-color: #f6ffed;
  border-color: #b7eb8f;
}

.result-error {
  background-color: #fff2f0;
  border-color: #ffccc7;
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.auth-btn {
  padding: 12px;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.auth-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(231, 76, 60, 0.3);
}

.auth-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-links {
  font-size: 14px;
  color: var(--light-text-color);
}

.auth-links a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  background-image: linear-gradient(to right, var(--primary-gradient-start), var(--primary-gradient-end));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-links a:hover {
  text-decoration: underline;
}

.diagnostic-toggle {
  margin-top: 10px;
  text-align: center;
  color: #1890ff;
  cursor: pointer;
  font-size: 13px;
  user-select: none;
}

.diagnostic-toggle:hover {
  text-decoration: underline;
}

@media (max-width: 576px) {
  .auth-container {
    padding: 15px;
  }
  
  .auth-title {
    font-size: 18px;
  }
  
  .form-group input {
    padding: 10px 12px;
    font-size: 14px;
  }
  
  .auth-submit-btn {
    padding: 10px;
    font-size: 15px;
  }
  
  .social-btn {
    width: 45px;
    height: 45px;
    font-size: 18px;
  }
}
</style> 