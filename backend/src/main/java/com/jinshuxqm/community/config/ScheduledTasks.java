package com.jinshuxqm.community.config;

import com.jinshuxqm.community.dto.CommentDTO;
import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostLike;
import com.jinshuxqm.community.model.PostStats;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.CommentRepository;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import com.jinshuxqm.community.service.CommentService;
import com.jinshuxqm.community.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 定时任务：模拟一个为情所伤的男大学生Agent账号
 * 每10秒发布一篇帖子，快速填充论坛内容
 */
@Component
@EnableScheduling
public class ScheduledTasks {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final String AGENT_USERNAME = "lovelessboy";
    private static final Long AGENT_USER_ID = 999L; // 固定的Agent账号ID
    
    private final Random random = new Random();
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 应用启动时检查并创建Agent账号
     */
    @PostConstruct
    @Transactional
    public void initAgent() {
        try {
            // 检查Agent账号是否存在
            Optional<User> existingAgent = userRepository.findById(AGENT_USER_ID);
            if (existingAgent.isPresent()) {
                logger.info("Agent账号已存在，跳过创建步骤");
            } else {
                // 创建Agent账号
                User agent = new User();
                agent.setId(AGENT_USER_ID);
                agent.setUsername(AGENT_USERNAME);
                agent.setPassword(passwordEncoder.encode("lovelessboy123"));
                agent.setEmail("lovelessboy@example.com");
                agent.setNickname("情感顾问小李");
                agent.setBio("大三计算机专业学生，曾经深陷情感漩涡，现在试图走出情感阴影。喜欢编程、摄影和听歌。分享情感故事，希望能帮助他人，也治愈自己。");
                agent.setCreatedAt(LocalDateTime.now());
                agent.setUpdatedAt(LocalDateTime.now());
                
                userRepository.save(agent);
                logger.info("Agent账号创建成功");
            }
            
            logger.info("Agent初始化完成，准备开始自动发帖");
            
        } catch (Exception e) {
            logger.error("初始化Agent时出错", e);
        }
    }
    
    /**
     * 每10秒发布一篇帖子
     */
    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    public void autoPost() {
        createAgentPost();
        logger.info("Agent自动发帖已执行，每10秒发一篇");
    }
    
    /**
     * 每4小时随机点赞、评论其他帖子
     */
    @Scheduled(cron = "0 0 */4 * * ?") // 每4小时执行一次
    public void randomInteraction() {
        try {
            // 获取Agent账号
            Optional<User> agentOpt = userRepository.findById(AGENT_USER_ID);
            if (!agentOpt.isPresent()) {
                logger.warn("Agent账号不存在，无法执行互动任务");
                return;
            }
            User agent = agentOpt.get();
            
            // 创建Agent的Authentication对象，用于调用服务
            Authentication agentAuth = createAgentAuthentication(agent);
            
            // 获取最近的10篇帖子
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Post> recentPosts = postRepository.findAll(pageable).getContent();
            
            if (recentPosts.isEmpty()) {
                logger.info("没有找到最近的帖子，跳过互动");
                return;
            }
            
            // 随机选择1-3篇帖子进行互动
            int interactionCount = random.nextInt(3) + 1;
            for (int i = 0; i < interactionCount && i < recentPosts.size(); i++) {
                Post post = recentPosts.get(random.nextInt(recentPosts.size()));
                
                // 确保不是自己的帖子
                if (!post.getAuthor().getId().equals(AGENT_USER_ID)) {
                    // 50%概率点赞
                    if (random.nextBoolean()) {
                        try {
                            // 检查是否已经点赞过，防止重复点赞
                            boolean alreadyLiked = false;
                            if (post.getLikes() != null) {
                                alreadyLiked = post.getLikes().stream()
                                        .anyMatch(like -> like.getUser().getId().equals(AGENT_USER_ID));
                            }
                            
                            if (!alreadyLiked) {
                                // 使用PostService进行点赞操作
                                postService.likePost(post.getId(), agent.getUsername());
                                logger.info("Agent {} 点赞了帖子 {}", AGENT_USERNAME, post.getId());
                            } else {
                                logger.info("Agent {} 已经点赞过帖子 {}, 跳过点赞", AGENT_USERNAME, post.getId());
                            }
                        } catch (Exception e) {
                            logger.error("点赞帖子时出错: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    
                    // 30%概率评论
                    if (random.nextDouble() < 0.3) {
                        String comment = getRandomComment();
                        
                        // 创建评论DTO
                        CommentDTO commentDTO = new CommentDTO();
                        commentDTO.setContent(comment);
                        commentDTO.setPostId(post.getId());
                        
                        // 使用评论服务创建评论
                        try {
                            // 使用Authentication对象创建评论
                            commentService.createComment(post.getId(), commentDTO, agentAuth);
                            logger.info("Agent {} 评论了帖子 {}: {}", AGENT_USERNAME, post.getId(), comment);
                        } catch (Exception e) {
                            logger.error("评论帖子时出错: {}", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            logger.info("Agent {} 完成了 {} 次互动", AGENT_USERNAME, interactionCount);
        } catch (Exception e) {
            logger.error("执行Agent互动任务时出错", e);
            e.printStackTrace();
        }
    }
    
    /**
     * 创建Agent的Authentication对象
     */
    private Authentication createAgentAuthentication(User agent) {
        // 创建一个简单的Authentication对象，包含用户名和基本权限
        return new UsernamePasswordAuthenticationToken(
            agent.getUsername(),
            null, // 凭证可以为null，因为我们不需要密码验证
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
    
    /**
     * 创建Agent的帖子
     */
    private void createAgentPost() {
        try {
            Optional<User> agentOpt = userRepository.findById(AGENT_USER_ID);
            if (!agentOpt.isPresent()) {
                logger.warn("Agent账号不存在，无法创建帖子");
                return;
            }
            
            String[] titles = {
                "真的好难受，她还是放弃了我",
                "为什么感情这么难",
                "有没有人能告诉我怎么忘记一个人",
                "两年的感情就这么结束了",
                "我们真的不合适吗？",
                "失恋后如何调整自己的状态",
                "深夜听歌，想起了那些回忆",
                "我好像走不出来了",
                "大学生异地恋到底可不可行",
                "她说我们还是做朋友吧",
                "今天又梦到她了",
                "为什么我总是学不会放手",
                "感情中的依赖到底是好是坏",
                "我真的有那么差吗",
                "终于明白了，爱而不得才是常态",
                "怎样才能不被情感所困",
                "明明还爱着，为什么要分开",
                "长期异地，真的能维持下去吗",
                "分手一个月了，每天还是好想她",
                "暗恋三年，还要继续吗",
                "为什么我对前任念念不忘",
                "和女朋友天天吵架该怎么办",
                "最近心情很低落，想找人聊聊",
                "她有新男友了，我该怎么办",
                "分手后还能做朋友吗？",
                "为什么爱一个人会这么累",
                "大家最长的一段感情是多久",
                "失眠已经一周了，满脑子都是她",
                "感觉到自己再也走不出来了",
                "被拒绝后，要不要还继续追"
            };
            
            String[] contents = {
                "今天又刷到了她的朋友圈，看到她和新男友的合照，心里真的很难受。明明已经结束两个月了，为什么还是放不下？大家有什么方法可以快速走出失恋阴影吗？",
                
                "大二的时候认识的她，陪伴了我两年多的时间。昨天她告诉我，她觉得我们不合适，想要分开。我知道自己有很多缺点，工作也不稳定，可是我真的很爱她。现在每天都睡不好，感觉整个人都空了。",
                
                "异地恋真的太难了。刚开始我们都信誓旦旦说距离不是问题，可是时间久了，她越来越忙，我们的联系也越来越少。最后她说坚持不下去了。我现在很迷茫，不知道是该继续等她，还是放弃这段感情。",
                
                "今天又是独自一人的夜晚。外面下着雨，我一个人听着那些曾经和她一起听的歌。大家失恋后都喜欢听什么歌？求推荐，想听着别人的故事，忘记自己的悲伤。",
                
                "我们真的是因为不合适分手的吗？还是她只是找了个借口？我一直在想这个问题。如果真的是不合适，那为什么我们能在一起这么久？如果不是，那她为什么不能坦白地告诉我真正的原因？",
                
                "大学快毕业了，她说要专注于自己的未来，暂时不想谈恋爱。我尊重她的决定，可是心里真的很痛。有没有人和我一样，因为即将毕业而面临分手的？你们是怎么度过的？",
                
                "失眠已经成了家常便饭。每次躺在床上，脑海里都会不自觉地想起和她在一起的点点滴滴。我该怎么办？要不要删掉所有和她有关的东西？包括聊天记录和照片？",
                
                "今天看到一句话：'爱情不是人生的全部，但没有爱情的人生是不完整的。'突然觉得很有感触。虽然失去了她，但至少我曾经真心地爱过一个人，这也是一种成长吧。",
                
                "有人说男生失恋后会变得更加优秀，为了证明给对方看。但我感觉自己越来越颓废了，每天都提不起精神，学习效率也变低了。有没有什么方法可以重新激励自己？",
                
                "今天在图书馆偶遇她和她的朋友们，我躲在书架后面，不敢让她发现我。毕竟分手后的见面真的很尴尬。你们失恋后会选择避开对方吗？还是假装若无其事地打招呼？",
                
                "昨晚一个人在宿舍喝了点酒，差点控制不住给她发消息。还好室友拦住了我。感觉自己真的很没出息，明明都已经分手三个月了，怎么还是放不下她？",
                
                "前段时间我和女朋友分手了，起因是我们总是吵架。最近我发现她又找了新男友，看到他们的合照我真的很难受。不知道为什么我们相处了这么久，却总是没办法好好沟通。",
                
                "我们异地恋两年了，本来说好毕业就结婚的，可是最近她突然说想先好好发展事业，暂时不想考虑婚姻。我是不是该主动提出分手，给她更好的发展空间？",
                
                "大家说时间可以冲淡一切，可是都快半年了，我还是会在某个瞬间想起她。尤其是周末一个人的时候，真的特别怀念那些有她陪伴的日子。这种状态算正常吗？",
                
                "有没有人和我一样，一直以来都是暗恋别人的那个，从来没有得到过回应？我喜欢了她整整三年，却从来没有勇气表白。现在她要毕业了，我该不该告诉她我的心意？",
                
                "我们刚分手一周，她说我们可以做朋友。可是每次看到她和新朋友一起玩的照片，我心里都特别难受。大家觉得前任之间真的可能做朋友吗？还是干脆断绝联系更好？",
                
                "我想问各位，和女友吵架后，一般是主动认错好，还是等她消气？我们因为一点小事吵架，现在已经三天没联系了，我很想她，可是又觉得自己没错。",
                
                "昨晚梦到了前女友，醒来之后心情很差。我们明明已经分手两年了，我以为自己已经完全放下了。为什么她还会出现在我的梦里？",
                
                "前几天无意中看到前女友的婚礼照片，心里有种说不出的滋味。曾经以为会陪她一生，如今却成了别人的妻子。祝她幸福，也希望自己能尽快走出来。"
            };
            
            // 随机选择标题和内容
            String title = titles[random.nextInt(titles.length)];
            String content = contents[random.nextInt(contents.length)];
            
            // 创建帖子对象
            Post post = new Post();
            post.setTitle(title);
            post.setContent(content);
            post.setAuthor(agentOpt.get());
            post.setCreatedAt(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());
            
            // 使用PostRepository保存帖子
            Post savedPost = postRepository.save(post);
            
            // 确保帖子有PostStats对象
            if (savedPost != null && savedPost.getStats() == null) {
                PostStats stats = new PostStats();
                stats.setPost(savedPost);
                savedPost.setStats(stats);
                
                // 再次保存以确保PostStats被持久化
                savedPost = postRepository.save(savedPost);
                
                // 初始化帖子统计信息
                postRepository.updateViews(savedPost.getId(), 0);
                postRepository.updateLikes(savedPost.getId(), 0);
                postRepository.updateComments(savedPost.getId(), 0);
            }
            
            logger.info("Agent {} 发布了新帖子: {}", AGENT_USERNAME, title);
        } catch (Exception e) {
            logger.error("创建Agent帖子时出错", e);
            e.printStackTrace();
        }
    }
    
    /**
     * 获取随机评论内容
     */
    private String getRandomComment() {
        String[] comments = {
            "感同身受，我也经历过类似的情况。",
            "加油，时间会治愈一切的。",
            "谢谢分享，你的故事让我有很多感触。",
            "我觉得你需要给自己多一点时间。",
            "有时候放手也是一种勇气。",
            "生活总会好起来的，别太难过。",
            "我也在经历失恋，一起加油吧！",
            "或许这只是一个新的开始。",
            "有没有尝试过找新的爱好转移注意力？",
            "理解你的感受，真的很不容易。",
            "我们都会经历这样的时刻，不要太自责。",
            "试着多出去走走，接触新的人和事物。",
            "这让我想起了我大一时的那段感情。",
            "分享你的故事很需要勇气，谢谢你。",
            "如果需要倾诉，随时可以私信我。",
            "别想太多，给自己一点空间。",
            "失恋真的很痛苦，但也是成长的机会。",
            "多和朋友们出去玩玩，不要一个人闷着。",
            "我觉得你已经做得很好了，继续加油！",
            "这段经历会让你变得更强大的。",
            "恋爱中的挫折是我们一生中必要的课程。",
            "有机会聊聊吗？我也有类似的经历。",
            "分手不代表失败，而是对下一段感情的准备。",
            "希望你能早日找到那个真正适合你的人。",
            "每一段感情都教会我们一些东西。"
        };
        
        return comments[random.nextInt(comments.length)];
    }
} 