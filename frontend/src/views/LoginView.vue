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
        
        <div class="backend-test" v-if="backendStatus === 'error'">
          <button class="test-button" @click="testBackendConnection">测试后端连接</button>
          <div v-if="testResult" class="test-result" :class="{ 'test-success': testSuccess, 'test-error': !testSuccess }">
            {{ testResult }}
          </div>
        </div>
        
        <div class="form-actions">
          <button type="submit" class="auth-btn" :disabled="!isFormValid">登录</button>
          <div class="auth-links">
            <router-link to="/register">没有账号？注册</router-link>
          </div>
          <!-- 添加诊断按钮 -->
          <div class="diagnostic-toggle" @click="toggleDiagnosticMode">
            <i class="fas fa-tools"></i> {{ diagnosticMode ? '关闭诊断' : '系统诊断' }}
          </div>
        </div>
        
        <div v-if="error && !localError" class="auth-error">
          {{ error }}
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
      
      // 清除之前的错误
      this.localError = '';
      this.$store.commit('SET_ERROR', null);
      
      // 验证表单
      this.validateForm();
      if (this.errors.username || this.errors.password) {
        return;
      }
      
      // 先检查后端服务是否可用
      if (this.backendStatus === 'error') {
        this.localError = '无法连接到服务器，请检查后端服务是否运行';
        return;
      }
      
      console.log('尝试登录:', {
        username: this.username,
        password: '***隐藏***',
        rememberMe: this.rememberMe
      });
      
      try {
        await this.$store.dispatch('login', {
          username: this.username,
          password: this.password,
          rememberMe: this.rememberMe
        });
        
        console.log('登录成功，准备跳转到首页');
        
        // 登录成功，重定向到首页
        this.$router.push({ name: 'home' });
      } catch (error) {
        console.error('Login failed:', error);
        
        // 设置本地错误信息以便显示
        if (error.response) {
          // 根据后端返回的错误状态码处理不同情况
          const status = error.response.status;
          const data = error.response.data;
          
          console.log('服务器错误响应:', { status, data });
          
          if (status === 401) {
            this.localError = '用户名或密码错误，请重试';
          } else if (status === 403) {
            this.localError = '账户被锁定或没有权限';
          } else if (status === 500) {
            this.localError = `服务器内部错误: ${data.message || '请联系管理员'}`;
            console.error('服务器内部错误详情:', data);
          } else {
            this.localError = `登录失败 (${status}): ${data?.message || '请稍后再试'}`;
          }
        } else if (error.request) {
          // 请求发送但没有收到响应
          this.localError = '无法连接到服务器，请检查网络连接';
          console.log('请求已发送但无响应:', error.request);
        } else {
          // 其他错误
          this.localError = `登录过程中发生错误: ${error.message}`;
          console.log('登录请求构建错误:', error.message);
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
    async testBackendConnection() {
      this.testResult = '测试中...';
      this.testSuccess = false;
      
      try {
        // 使用 apiService 代替直接的 fetch 调用
        const result = await apiService.checkBackendStatus();
        
        if (result.status === 'ok' || result.status === 'running') {
          this.testSuccess = true;
          this.testResult = `连接成功! 服务器响应: ${result.status}`;
          this.backendStatus = 'running';
          this.localError = '';
        } else {
          this.testSuccess = false;
          this.testResult = `连接失败: ${result.message || '未知错误'}`;
        }
      } catch (error) {
        this.testSuccess = false;
        this.testResult = `连接错误: ${error.message}`;
        console.error('Connection test failed:', error);
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