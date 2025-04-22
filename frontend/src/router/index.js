import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/HomeView.vue'),
    meta: { keepAlive: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import(/* webpackChunkName: "login" */ '../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
  {
    path: '/search',
    name: 'search',
    component: () => import(/* webpackChunkName: "search" */ '../views/SearchView.vue')
  },
  {
    path: '/create-post',
    name: 'create-post',
    component: () => import('../views/CreatePostView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/edit-post/:id',
    name: 'edit-post',
    component: () => import('../views/CreatePostView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/post/:id',
    name: 'post-detail',
    component: () => import('../views/PostDetailView.vue')
  },
  {
    path: '/profile/:id',
    name: 'profile',
    component: () => import(/* webpackChunkName: "profile" */ '../views/ProfileView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

// 全局路由守卫 - 处理需要认证的路由
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const isAuthenticated = !!localStorage.getItem('token');
  
  if (requiresAuth && !isAuthenticated) {
    // 如果路由需要认证但用户未登录，重定向到登录页面
    next({
      name: 'login',
      query: { redirect: to.fullPath }
    });
  } else {
    // 检查帖子详情页的ID有效性
    if (to.name === 'post-detail') {
      const postId = to.params.id;
      if (!postId || postId === 'undefined' || isNaN(Number(postId))) {
        console.warn(`导航被拦截：无效的帖子ID [${postId}]`);
        // 帖子ID无效，重定向到首页
        next({ name: 'Home' });
        return;
      }
    }
    // 否则继续
    next();
  }
});

export default router 