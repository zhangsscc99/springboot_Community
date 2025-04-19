/* eslint-disable */
import { createStore } from 'vuex'

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
      // 同时添加到关注栏目
      state.tabPosts['关注'].unshift(post);
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
    // User authentication actions
    async login({ commit }, credentials) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to authenticate
        // 模拟登录成功，返回用户信息
        const user = { 
          id: 123, 
          username: credentials.email,
          avatar: "https://via.placeholder.com/40"
        };
        commit('SET_USER', user);
        commit('SET_ERROR', null);
        return user;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    logout({ commit }) {
      commit('SET_USER', null);
    },
    
    setActiveTab({ commit }, tab) {
      commit('SET_ACTIVE_TAB', tab);
    },
    
    // Post related actions
    async fetchPosts({ commit }) {
      commit('SET_LOADING', true);
      try {
        // 关注栏目的帖子
        const followPosts = [
          {
            id: 1,
            title: "为什么大厂程序员的另一半全职太太比例特别多?",
            content: "我说出了一些实话可能涉及全图 aoe。因为程序员，尤其是大厂程序员，在择偶上普遍是经济向下兼容模...",
            author: {
              id: 1,
              username: "李珠",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 848,
            comments: 219,
            created_at: "2023-05-15T10:30:00Z",
            tags: ["程序员生活", "职场", "情感话题"]
          },
          {
            id: 2,
            title: "在雅思中你明白了哪些规则后，分数便开始直线飙升？",
            content: "在一次偶然的国际学术会议上，遇到了\"素未蒙面但经常email沟通\"的国外某导，在得知我最近在备考雅思...",
            author: {
              id: 2,
              username: "日辛说",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 4912,
            comments: 12000,
            created_at: "2023-05-14T08:15:00Z",
            tags: ["雅思备考", "英语学习", "留学经验"]
          }
        ];
        
        // 推荐栏目的帖子
        const recommendPosts = [
          {
            id: 3,
            title: "执行间隙为什么会毁掉一个人？",
            content: "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。",
            author: {
              id: 3,
              username: "咫尺燃灯",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 1488,
            comments: 3468,
            created_at: "2023-05-13T14:45:00Z",
            tags: ["自我提升", "时间管理", "心理分析"]
          },
          {
            id: 4,
            title: "如何在30岁前实现财务自由？",
            content: "很多人都梦想着能早日实现财务自由，但现实往往是：30岁前能够还清房贷就已经很了不起了。那么到底有没有可能在30岁前实现财务自由呢？",
            author: {
              id: 4,
              username: "财富自由之路",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 2567,
            comments: 1324,
            created_at: "2023-05-12T09:20:00Z",
            tags: ["理财", "投资", "财务规划"]
          }
        ];
        
        // 热榜栏目的帖子
        const hotPosts = [
          {
            id: 5,
            title: "ChatGPT会取代程序员吗？一位资深开发者的深度思考",
            content: "随着ChatGPT的爆火，越来越多的人开始担忧AI会不会取代自己的工作。作为一名工作了10年的程序员，我想分享一下我对这个问题的看法...",
            author: {
              id: 5,
              username: "代码人生",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 6789,
            comments: 3421,
            created_at: "2023-05-11T16:40:00Z",
            tags: ["人工智能", "职业发展", "程序员"]
          },
          {
            id: 6,
            title: "今年互联网大厂的裁员潮，会影响应届生就业吗？",
            content: "最近各大互联网公司的裁员新闻不断，作为即将毕业的计算机专业学生，不禁担忧今年的就业形势。通过走访多家公司和分析行业数据，我发现...",
            author: {
              id: 6,
              username: "就业观察家",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 5421,
            comments: 2987,
            created_at: "2023-05-10T13:25:00Z",
            tags: ["就业", "校招", "职场"]
          }
        ];
        
        // 故事栏目的帖子
        const storyPosts = [
          {
            id: 7,
            title: "那年夏天，我遇见了改变我一生的陌生人",
            content: "2015年的夏天，我独自一人背包旅行，在青海湖畔遇见了一位藏族老人。他的一句话，让我从此改变了人生轨迹...",
            author: {
              id: 7,
              username: "旅行的意义",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 4378,
            comments: 1674,
            created_at: "2023-05-09T11:10:00Z",
            tags: ["旅行故事", "人生感悟", "青海"]
          },
          {
            id: 8,
            title: "毕业十年后，我终于明白了恩师的良苦用心",
            content: "大学期间，我有一位非常严厉的教授，上他的课总是战战兢兢。毕业后进入职场，我才逐渐理解他的严格是对我们最好的准备...",
            author: {
              id: 8,
              username: "感恩有你",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 3692,
            comments: 2104,
            created_at: "2023-05-08T09:45:00Z",
            tags: ["师生情谊", "感恩", "成长故事"]
          }
        ];
        
        // 情感知识栏目的帖子
        const emotionalPosts = [
          {
            id: 9,
            title: "为什么高情商的人更容易获得成功？",
            content: "情商（EQ）在现代社会中的重要性日益凸显。研究表明，高情商的人在职场和人际关系中往往更加成功。本文将从心理学角度分析高情商的特质及培养方法...",
            author: {
              id: 9,
              username: "心理分析师",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 5842,
            comments: 2436,
            created_at: "2023-05-07T14:30:00Z",
            tags: ["情商", "心理学", "自我提升"]
          },
          {
            id: 10,
            title: "长期伴侣关系中，如何保持亲密感和新鲜感？",
            content: "许多人在恋爱或婚姻关系中，随着时间推移会感到激情减退。如何在长期关系中维持亲密感和新鲜感？以下是心理学家提出的几个实用建议...",
            author: {
              id: 10,
              username: "关系治愈师",
              avatar: "https://via.placeholder.com/40"
            },
            likes: 6734,
            comments: 3219,
            created_at: "2023-05-06T10:15:00Z",
            tags: ["情感关系", "亲密关系", "心理健康"]
          }
        ];
        
        // 设置所有栏目的帖子
        commit('SET_POSTS', [...followPosts, ...recommendPosts, ...hotPosts, ...storyPosts, ...emotionalPosts]);
        commit('SET_TAB_POSTS', { tab: '关注', posts: followPosts });
        commit('SET_TAB_POSTS', { tab: '推荐', posts: recommendPosts });
        commit('SET_TAB_POSTS', { tab: '热榜', posts: hotPosts });
        commit('SET_TAB_POSTS', { tab: '故事', posts: storyPosts });
        commit('SET_TAB_POSTS', { tab: '情感知识', posts: emotionalPosts });
        
        commit('SET_ERROR', null);
      } catch (error) {
        commit('SET_ERROR', error.message);
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async fetchPostById({ commit }, postId) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to fetch a specific post
        const post = {
          id: postId,
          title: "执行间隙为什么会毁掉一个人？",
          content: "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。\n\n我们常常高估了自己的意志力，认为光靠想就能改变自己，却忽视了建立习惯和系统对于长期成功的重要性。执行间隙本质上是我们的行动系统出了问题，而不是单纯的懒惰或拖延。\n\n要解决执行间隙问题，我们需要：\n1. 设立明确且可量化的目标\n2. 将大目标分解成小步骤\n3. 建立固定的行动流程\n4. 创造有利于行动的环境\n5. 培养正向反馈循环",
          author: {
            id: 3,
            username: "咫尺燃灯",
            avatar: "https://via.placeholder.com/40"
          },
          likes: 1488,
          comments: 3468,
          created_at: "2023-05-13T14:45:00Z",
          tags: ["自我提升", "时间管理", "心理分析"],
          commentList: [
            {
              id: 1,
              content: "这篇文章说到我心坎里了，我就是有严重的执行间隙问题",
              author: {
                id: 4,
                username: "Reader01",
                avatar: "https://via.placeholder.com/40"
              },
              created_at: "2023-05-13T15:30:00Z"
            },
            {
              id: 2,
              content: "建立习惯真的很重要，我用番茄工作法后效率提高了不少",
              author: {
                id: 5,
                username: "Efficiency99",
                avatar: "https://via.placeholder.com/40"
              },
              created_at: "2023-05-13T16:20:00Z"
            }
          ]
        };
        commit('SET_CURRENT_POST', post);
        commit('SET_ERROR', null);
      } catch (error) {
        commit('SET_ERROR', error.message);
      } finally {
        commit('SET_LOADING', false);
      }
    },
    
    async createPost({ commit }, postData) {
      commit('SET_LOADING', true);
      try {
        // Here would be an API call to create a post
        const newPost = {
          id: Date.now(),
          title: postData.title,
          content: postData.content,
          author: {
            id: 1,
            username: "User123",
            avatar: "https://via.placeholder.com/40"
          },
          likes: 0,
          comments: 0,
          created_at: new Date().toISOString(),
          tags: ["我的动态"]
        };
        commit('ADD_POST', newPost);
        commit('SET_ERROR', null);
        return newPost;
      } catch (error) {
        commit('SET_ERROR', error.message);
        throw error;
      } finally {
        commit('SET_LOADING', false);
      }
    }
  },
  modules: {
  }
}) 