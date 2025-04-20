-- 锦书情感社区 - 示例帖子数据
-- 此脚本用于向数据库中添加示例帖子数据

-- 启用安全更新模式
SET SQL_SAFE_UPDATES = 1;

-- 开始事务
START TRANSACTION;

-- 获取第一个用户ID作为帖子作者（优先使用admin用户）
SELECT @author_id := id FROM users WHERE username = 'admin' LIMIT 1;

-- 如果没有admin用户，则使用第一个可用用户
SELECT COUNT(*) INTO @author_count FROM users WHERE id = @author_id;

IF @author_count = 0 THEN
  SELECT @author_id := id FROM users LIMIT 1;
END IF;

-- 检查是否有用户可作为帖子作者
SELECT COUNT(*) INTO @user_exists FROM users;

-- 如果没有用户，则中止脚本
IF @user_exists = 0 THEN
  SELECT '错误：数据库中没有用户，请先创建至少一个用户账号!' AS '执行状态';
  ROLLBACK;
ELSE
  -- 初始化计数器
  SET @posts_inserted = 0;
  
  -- 帖子1：人际关系网络建立
  SET @post_title = '如何建立并维护一个健康的人际关系网络？';
  IF NOT EXISTS (SELECT 1 FROM posts WHERE title = @post_title) THEN
    INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
    VALUES (
      @post_title,
      '在现代社会中，拥有一个健康的人际关系网络至关重要。以下是我总结的几点建议：\n\n1. **真诚对待每一段关系**：无论是家人、朋友还是同事，真诚是维系关系的基础。\n\n2. **主动联系与维护**：定期与重要的人保持联系，不要等到需要帮助时才想起他们。\n\n3. **学会倾听**：在交流中，倾听往往比表达更重要。尝试理解对方的感受和观点。\n\n4. **设定合理边界**：健康的关系需要合理的边界，学会说"不"也是维护关系的一部分。\n\n5. **解决冲突的能力**：每段关系都会有摩擦，关键是如何建设性地解决问题。\n\n你们有哪些建立和维护人际关系的经验可以分享？',
      @author_id,
      '关注',
      NOW() - INTERVAL FLOOR(RAND() * 30) DAY,
      NOW() - INTERVAL FLOOR(RAND() * 5) DAY
    );
    
    -- 获取新插入帖子的ID
    SET @post_id = LAST_INSERT_ID();
    
    -- 创建帖子统计数据
    INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
    VALUES (@post_id, FLOOR(RAND() * 50), FLOOR(RAND() * 20), FLOOR(RAND() * 10), FLOOR(50 + RAND() * 200));
    
    -- 添加标签
    INSERT INTO post_tags (post_id, tag) VALUES 
    (@post_id, '人际关系'), 
    (@post_id, '社交技巧'), 
    (@post_id, '自我提升');
    
    SET @posts_inserted = @posts_inserted + 1;
  END IF;
  
  -- 帖子2：沟通技巧
  SET @post_title = '提升有效沟通能力的五个实用技巧';
  IF NOT EXISTS (SELECT 1 FROM posts WHERE title = @post_title) THEN
    INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
    VALUES (
      @post_title,
      '沟通是人际关系的核心，但很多人都在沟通中遇到困难。以下是我在心理学研究和个人实践中总结的五个提升沟通效果的技巧：\n\n1. **使用"我"陈述句**：用"我感到..."代替"你总是..."，避免对方产生防御心理。\n\n2. **非语言沟通意识**：注意你的肢体语言、眼神接触和面部表情，它们传递的信息往往比言语更重要。\n\n3. **积极倾听技巧**：不只是听对方说话，而是尝试理解背后的情感和需求，并通过提问和复述确认你的理解是否正确。\n\n4. **选择合适的沟通方式**：根据情况选择面对面、电话或文字沟通，不同的话题适合不同的媒介。\n\n5. **情绪管理**：在沟通中保持情绪稳定，避免在愤怒或沮丧时做出重要决定或发表言论。\n\n大家在日常沟通中有哪些困扰？欢迎在评论区分享你的经历和问题。',
      @author_id,
      '推荐',
      NOW() - INTERVAL FLOOR(RAND() * 25) DAY,
      NOW() - INTERVAL FLOOR(RAND() * 3) DAY
    );
    
    SET @post_id = LAST_INSERT_ID();
    
    INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
    VALUES (@post_id, FLOOR(RAND() * 60), FLOOR(RAND() * 25), FLOOR(RAND() * 15), FLOOR(80 + RAND() * 250));
    
    INSERT INTO post_tags (post_id, tag) VALUES 
    (@post_id, '沟通技巧'), 
    (@post_id, '人际关系'), 
    (@post_id, '情感表达');
    
    SET @posts_inserted = @posts_inserted + 1;
  END IF;
  
  -- 帖子3：家庭关系
  SET @post_title = '如何处理家庭关系中的代际冲突';
  IF NOT EXISTS (SELECT 1 FROM posts WHERE title = @post_title) THEN
    INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
    VALUES (
      @post_title,
      '家庭关系是我们生活中最重要也最复杂的关系之一，特别是当代际差异导致观念冲突时。\n\n作为一个经历过多次家庭冲突并最终找到和解方式的过来人，我想分享一些有效的方法：\n\n1. **理解代际差异的根源**：不同时代成长的人有不同的价值观和生活经历，尝试从他们的角度看问题。\n\n2. **建立有效的沟通渠道**：定期的家庭会议或一对一交流，创造一个每个人都能安全表达的环境。\n\n3. **设定明确的界限**：尊重每个家庭成员的个人空间和决定权，特别是成年子女的生活选择。\n\n4. **寻找共同点**：尽管存在差异，试着发现并强化家庭成员间的共同兴趣和价值观。\n\n5. **在必要时寻求专业帮助**：家庭治疗师可以提供中立的视角和专业的调解技巧。\n\n最近我和父母就我的职业选择有了一些分歧，使用了上述方法后，我们达成了一个双方都能接受的妥协。大家有类似的经历吗？',
      @author_id,
      '关注',
      NOW() - INTERVAL FLOOR(RAND() * 20) DAY,
      NOW() - INTERVAL FLOOR(RAND() * 2) DAY
    );
    
    SET @post_id = LAST_INSERT_ID();
    
    INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
    VALUES (@post_id, FLOOR(RAND() * 45), FLOOR(RAND() * 30), FLOOR(RAND() * 12), FLOOR(65 + RAND() * 180));
    
    INSERT INTO post_tags (post_id, tag) VALUES 
    (@post_id, '家庭关系'), 
    (@post_id, '代际冲突'), 
    (@post_id, '沟通');
    
    SET @posts_inserted = @posts_inserted + 1;
  END IF;
  
  -- 帖子4：数字时代的人际连接
  SET @post_title = '数字时代如何保持真实的人际连接';
  IF NOT EXISTS (SELECT 1 FROM posts WHERE title = @post_title) THEN
    INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
    VALUES (
      @post_title,
      '在这个被社交媒体和即时通讯主导的时代，我们似乎比以往任何时候都更"连接"，但实际上，许多人感到比以往更加孤独。\n\n作为一个既喜欢科技又重视真实人际关系的人，我想分享一些在数字时代保持真实连接的方法：\n\n1. **质量胜于数量**：与其在社交媒体上积累数百个"朋友"，不如专注于发展和维护几段深厚的关系。\n\n2. **定期线下见面**：无论多么忙碌，都要安排时间与重要的人面对面相处，共进晚餐，一起散步或进行其他活动。\n\n3. **有意识地使用科技**：将科技作为增强而非替代真实互动的工具。例如，使用视频通话与远方的亲友保持联系。\n\n4. **创造无手机时刻**：在重要的社交场合，尝试将手机放在一边，全身心投入当下的互动。\n\n5. **深度而非广度**：在交流中追求深度，分享真实的想法和感受，而不是停留在表面的状态更新。\n\n最近我试着每周至少安排一次与朋友的线下聚会，完全不看手机，我发现这些时刻变得异常宝贵，我们的交流也更加真实和有意义。\n\n在这个数字化时代，你是如何保持真实连接的？有什么挑战或技巧想分享吗？',
      @author_id,
      '热榜',
      NOW() - INTERVAL FLOOR(RAND() * 10) DAY,
      NOW()
    );
    
    SET @post_id = LAST_INSERT_ID();
    
    INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
    VALUES (@post_id, FLOOR(RAND() * 80), FLOOR(RAND() * 40), FLOOR(RAND() * 25), FLOOR(150 + RAND() * 350));
    
    INSERT INTO post_tags (post_id, tag) VALUES 
    (@post_id, '数字时代'), 
    (@post_id, '社交媒体'), 
    (@post_id, '真实连接');
    
    SET @posts_inserted = @posts_inserted + 1;
  END IF;

  -- 帖子5：依恋理论
  SET @post_title = '依恋理论：它如何影响你的所有关系';
  IF NOT EXISTS (SELECT 1 FROM posts WHERE title = @post_title) THEN
    INSERT INTO posts (title, content, user_id, tab, created_at, updated_at)
    VALUES (
      @post_title,
      '作为一名心理学研究生，我一直对依恋理论（Attachment Theory）着迷。这个由约翰·鲍尔比（John Bowlby）和玛丽·艾因斯沃斯（Mary Ainsworth）开创的理论，不仅解释了婴儿与照顾者的关系，还深刻影响了我们成年后的所有亲密关系。\n\n依恋理论认为，我们早期与主要照顾者的互动形成了"依恋风格"，主要分为：\n\n1. **安全型依恋（Secure Attachment）**：这些人在关系中感到自在，能够既依赖他人又保持独立。他们通常来自反应及时且一致的养育环境。\n\n2. **焦虑型依恋（Anxious Attachment）**：这些人渴望亲密但又担心被抛弃，常表现为需要过度的确认和敏感于拒绝。他们可能来自反应不一致的养育环境。\n\n3. **回避型依恋（Avoidant Attachment）**：这些人倾向于保持情感距离，不舒服于过度亲密，通常来自情感需求被忽视的环境。\n\n4. **混乱型依恋（Disorganized Attachment）**：同时具有焦虑和回避特征，通常源于创伤或虐待经历。\n\n了解自己的依恋风格是改善关系的第一步。识别你的依恋风格的一个简单方法是思考：在关系中感到不安全时，你是倾向于追求更多联系（焦虑型），还是退缩寻求空间（回避型）？\n\n你认为自己是哪种依恋风格？这对你的关系有何影响？',
      @author_id,
      '情感知识',
      NOW() - INTERVAL FLOOR(RAND() * 3) DAY,
      NOW()
    );
    
    SET @post_id = LAST_INSERT_ID();
    
    INSERT INTO post_stats (post_id, like_count, comment_count, favorite_count, view_count)
    VALUES (@post_id, FLOOR(RAND() * 65), FLOOR(RAND() * 40), FLOOR(RAND() * 25), FLOOR(150 + RAND() * 300));
    
    INSERT INTO post_tags (post_id, tag) VALUES 
    (@post_id, '依恋理论'), 
    (@post_id, '心理学'), 
    (@post_id, '亲密关系');
    
    SET @posts_inserted = @posts_inserted + 1;
  END IF;
  
  -- 提交事务
  COMMIT;
  
  -- 显示执行结果
  SELECT CONCAT('成功插入 ', @posts_inserted, ' 个示例帖子') AS '执行状态';
  
  -- 显示最近插入的帖子
  SELECT p.id, p.title, p.tab, ps.view_count, ps.like_count, ps.comment_count
  FROM posts p
  JOIN post_stats ps ON p.id = ps.post_id
  ORDER BY p.id DESC
  LIMIT 10;
END IF;

-- 恢复默认设置
SET SQL_SAFE_UPDATES = DEFAULT; 
