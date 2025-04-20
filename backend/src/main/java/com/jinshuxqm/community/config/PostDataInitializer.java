package com.jinshuxqm.community.config;

import com.jinshuxqm.community.model.Post;
import com.jinshuxqm.community.model.PostStats;
import com.jinshuxqm.community.model.User;
import com.jinshuxqm.community.repository.PostRepository;
import com.jinshuxqm.community.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 示例帖子数据初始化器
 * 在应用启动时将示例帖子写入数据库
 * Order(2)确保在角色和用户初始化之后运行
 */
@Component
@Profile("!test") // 非测试环境下执行
@Order(2)
public class PostDataInitializer implements CommandLineRunner {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 检查是否已经初始化了示例帖子
        if (postRepository.count() == 0) {
            System.out.println("初始化示例帖子数据...");
            initSamplePosts();
        } else {
            System.out.println("已存在帖子数据，跳过初始化示例帖子");
        }
    }

    private void initSamplePosts() {
        // 获取管理员用户作为示例帖子作者
        User admin = userRepository.findByUsername("admin")
                .orElseGet(() -> {
                    System.out.println("警告: 未找到管理员用户，示例帖子初始化可能失败");
                    return null;
                });

        if (admin == null) {
            System.out.println("错误: 无法找到有效用户作为帖子作者，示例帖子初始化失败");
            return;
        }

        // 创建几个模拟用户作为不同的作者
        User user1 = createOrGetUser("李珠", "lizhu@example.com", admin);
        User user2 = createOrGetUser("日辛说", "rixin@example.com", admin);
        User user3 = createOrGetUser("咫尺燃灯", "zhichi@example.com", admin);
        User user4 = createOrGetUser("财富自由之路", "wealth@example.com", admin);
        User user5 = createOrGetUser("代码人生", "coder@example.com", admin);
        User user6 = createOrGetUser("就业观察家", "career@example.com", admin);
        User user7 = createOrGetUser("旅行的意义", "travel@example.com", admin);
        User user8 = createOrGetUser("感恩有你", "gratitude@example.com", admin);
        User user9 = createOrGetUser("心理分析师", "psycho@example.com", admin);
        User user10 = createOrGetUser("关系治愈师", "relation@example.com", admin);

        // 关注栏目的帖子
        createPost(
            "为什么大厂程序员的另一半全职太太比例特别多?",
            "我说出了一些实话可能涉及全图 aoe。因为程序员，尤其是大厂程序员，在择偶上普遍是经济向下兼容模式，而程序员的收入普遍较高，所以他们的另一半在经济上往往不需要太多的负担。\n\n同时，大厂程序员工作强度较大，工作时间长，需要有人照顾家庭和孩子，全职太太就成为了一种合理的家庭分工方式。另外，在快节奏的科技行业中，家庭的支持和稳定的后方对程序员的职业发展非常重要。\n\n当然，这只是一种普遍现象，并不代表所有大厂程序员的家庭都是这种模式。每个家庭都有自己的选择和安排，最重要的是双方都认可这种生活方式。",
            user1,
            "关注",
            new HashSet<>(Arrays.asList("程序员生活", "职场", "情感话题")),
            848, 219, 0, 1500,
            parseDateTime("2023-05-15T10:30:00Z")
        );

        createPost(
            "在雅思中你明白了哪些规则后，分数便开始直线飙升？",
            "在一次偶然的国际学术会议上，遇到了\"素未蒙面但经常email沟通\"的国外某导，在得知我最近在备考雅思时，他给了我一个改变我备考方向的建议。\n\n他说：\"雅思考试不是在测试你的英语水平，而是在测试你对雅思考试本身的了解程度。\"\n\n这句话让我醍醐灌顶。我开始研究雅思的评分标准和考试技巧，而不仅仅是盲目地提高英语能力。以下是我领悟到的几点关键规则：\n\n1. 听力部分：预测问题类型比听懂全文更重要\n2. 阅读部分：时间管理和定位信息的能力是关键\n3. 写作部分：结构清晰、观点明确比华丽的词汇更加重要\n4. 口语部分：流利度和连贯性比完美的发音更受重视\n\n理解了这些规则后，我的雅思成绩从6.5直接提升到了7.5，希望对你也有帮助！",
            user2,
            "关注",
            new HashSet<>(Arrays.asList("雅思备考", "英语学习", "留学经验")),
            4912, 1200, 0, 8500,
            parseDateTime("2023-05-14T08:15:00Z")
        );

        // 推荐栏目的帖子
        createPost(
            "执行间隙为什么会毁掉一个人？",
            "刚刚读到一篇好文：一个人真正废掉的核心原因，往往不是能力不足或资源匮乏，而是从想法到行动的「执行间隙」太大，想做的事情迟迟不能落地。\n\n我们常常高估了自己的意志力，认为光靠想就能改变自己，却忽视了建立习惯和系统对于长期成功的重要性。执行间隙本质上是我们的行动系统出了问题，而不是单纯的懒惰或拖延。\n\n要解决执行间隙问题，我们需要：\n1. 设立明确且可量化的目标\n2. 将大目标分解成小步骤\n3. 建立固定的行动流程\n4. 创造有利于行动的环境\n5. 培养正向反馈循环",
            user3,
            "推荐",
            new HashSet<>(Arrays.asList("自我提升", "时间管理", "心理分析")),
            1488, 346, 0, 4200,
            parseDateTime("2023-05-13T14:45:00Z")
        );

        createPost(
            "如何在30岁前实现财务自由？",
            "很多人都梦想着能早日实现财务自由，但现实往往是：30岁前能够还清房贷就已经很了不起了。那么到底有没有可能在30岁前实现财务自由呢？\n\n首先，我们需要明确什么是财务自由。我的定义是：被动收入足以覆盖日常开支，不需要再为生活必需品工作。\n\n实现30岁前财务自由，需要几个关键要素：\n\n1. 高收入职业 - 选择高薪行业并不断提升专业能力\n2. 高储蓄率 - 坚持50%以上的储蓄率，避免生活方式膨胀\n3. 明智投资 - 学习投资知识，进行分散化投资\n4. 创造多元收入 - 除了主业外，发展副业和被动收入来源\n5. 降低生活成本 - 简化生活，减少不必要的消费\n\n虽然30岁前实现完全财务自由对大多数人来说很困难，但实现部分财务自由是可能的。最重要的是开始行动并保持耐心，财务自由是一场马拉松，不是短跑。",
            user4,
            "推荐",
            new HashSet<>(Arrays.asList("理财", "投资", "财务规划")),
            2567, 1324, 0, 6700,
            parseDateTime("2023-05-12T09:20:00Z")
        );

        // 热榜栏目的帖子
        createPost(
            "ChatGPT会取代程序员吗？一位资深开发者的深度思考",
            "随着ChatGPT的爆火，越来越多的人开始担忧AI会不会取代自己的工作。作为一名工作了10年的程序员，我想分享一下我对这个问题的看法。\n\n首先，AI确实会改变程序员的工作方式。ChatGPT等工具已经可以帮助我们：\n- 生成样板代码\n- 调试简单错误\n- 解释复杂概念\n- 提供编程建议\n\n但它不会完全取代程序员，原因有几点：\n\n1. 创造性思维：软件开发的核心是解决问题，这需要创造性和批判性思维。\n\n2. 业务理解：程序员需要理解业务领域知识，这是AI难以替代的。\n\n3. 复杂系统设计：架构设计需要全局思考和长期经验。\n\n4. 代码质量保证：AI生成的代码通常需要人工审查和优化。\n\n5. 沟通与协作：软件开发是团队活动，需要有效的人际交流。\n\n未来的程序员更像是「AI辅助开发者」，我们需要学会与AI协作，专注于更高层次的问题解决和创新。真正危险的不是AI本身，而是拒绝适应变化的程序员。",
            user5,
            "热榜",
            new HashSet<>(Arrays.asList("人工智能", "职业发展", "程序员")),
            6789, 3421, 0, 12000,
            parseDateTime("2023-05-11T16:40:00Z")
        );

        createPost(
            "今年互联网大厂的裁员潮，会影响应届生就业吗？",
            "最近各大互联网公司的裁员新闻不断，作为即将毕业的计算机专业学生，不禁担忧今年的就业形势。通过走访多家公司和分析行业数据，我发现情况虽然严峻但并非没有机会。\n\n裁员主要原因：\n1. 经济下行压力增大\n2. 过去几年扩张过快，人员冗余\n3. 一些业务线调整或关停\n4. AI技术应用减少了某些岗位需求\n\n对应届生的影响：\n- 招聘名额总体减少20%-30%\n- 薪资水平有所下降\n- 竞争更加激烈\n- 面试难度和要求提高\n\n应对策略：\n1. 提前准备，尽早投递简历\n2. 扩大求职范围，考虑二三线城市\n3. 关注仍在快速发展的细分领域（如AI、云计算、网络安全）\n4. 提升核心竞争力，如算法能力、项目经验\n5. 考虑继续深造或接受培训\n\n虽然环境变化，但机会依然存在。最重要的是保持积极心态，不断学习和适应市场需求的变化。",
            user6,
            "热榜",
            new HashSet<>(Arrays.asList("就业", "校招", "职场")),
            5421, 2987, 0, 9500,
            parseDateTime("2023-05-10T13:25:00Z")
        );

        // 故事栏目的帖子
        createPost(
            "那年夏天，我遇见了改变我一生的陌生人",
            "2015年的夏天，我独自一人背包旅行，在青海湖畔遇见了一位藏族老人。他的一句话，让我从此改变了人生轨迹。\n\n那是一个傍晚，夕阳为青海湖镀上了一层金色。我坐在湖边的石头上，望着远处发呆。一位穿着传统藏袍的老人在我旁边坐下，沉默地看着远方。\n\n过了很久，他用不太流利的普通话问我：\"年轻人，你在寻找什么？\"\n\n这个简单的问题让我措手不及。我本想随便回答，却发现自己无法说出一个确定的答案。\n\n见我沉默，老人微笑着说：\"很多人走遍千山万水，却从未真正了解自己的内心。人生最远的旅行，不是从这里到那里，而是从自己的大脑到心灵。\"\n\n他告诉我，他年轻时也曾四处游历，寻找生命的意义。直到有一天他明白，真正的意义不在远方，而在内心的宁静与觉知。\n\n回到城市后，我辞去了令我倦怠的高薪工作，开始学习心理学，现在成为了一名心理咨询师，帮助他人探索内心世界。\n\n有时最短暂的相遇，会带来最深远的影响。那位素不相识的老人，教会了我生命中最重要的一课。",
            user7,
            "故事",
            new HashSet<>(Arrays.asList("旅行故事", "人生感悟", "青海")),
            4378, 1674, 0, 7800,
            parseDateTime("2023-05-09T11:10:00Z")
        );

        createPost(
            "毕业十年后，我终于明白了恩师的良苦用心",
            "大学期间，我有一位非常严厉的教授，上他的课总是战战兢兢。毕业后进入职场，我才逐渐理解他的严格是对我们最好的准备。\n\n王教授的课出了名的难。他从不按照教材照本宣科，而是会提出各种刁钻的问题，要求我们独立思考。每次作业都要反复修改，常常让我们熬夜到凌晨。当时，我们都觉得他过于苛刻，甚至有些故意为难人。\n\n毕业后，我进入了一家知名企业工作。刚开始，我总是被领导退回的方案和被客户质疑的报告弄得焦头烂额。但很快，我发现王教授教给我的方法派上了用场：\n\n1. 学会提前思考可能的问题和反对意见\n2. 做任何事都留有余地，预设多套方案\n3. 接受批评并快速调整的能力\n4. 对自己的工作永远保持高标准\n\n十年职场生涯，我从基层员工升至部门主管，回顾这段历程，我最感谢的就是王教授的\"不近人情\"。他不是在教我们知识，而是在培养我们的能力和品格。\n\n去年校友会上，我鼓起勇气向已经退休的王教授表达了我的感谢。他笑着说：\"好老师应该像冬天，严寒能让学生成长得更坚韧。如果我当时对你们太温柔，今天的你也许就经不起社会的风雨了。\"\n\n这句话，我会永远记住，也会传递给我的下属和孩子。",
            user8,
            "故事",
            new HashSet<>(Arrays.asList("师生情谊", "感恩", "成长故事")),
            3692, 2104, 0, 6500,
            parseDateTime("2023-05-08T09:45:00Z")
        );

        // 情感知识栏目的帖子
        createPost(
            "为什么高情商的人更容易获得成功？",
            "情商（EQ）在现代社会中的重要性日益凸显。研究表明，高情商的人在职场和人际关系中往往更加成功。本文将从心理学角度分析高情商的特质及培养方法。\n\n高情商的五大核心能力：\n\n1. 自我认知能力：准确认识自己的情绪、优势和局限性。\n2. 情绪管理能力：控制冲动情绪，保持冷静理性。\n3. 自我激励能力：即使面对挫折也能保持积极和坚持。\n4. 共情能力：理解他人情感并给予适当反应。\n5. 社交技巧：有效处理人际关系，建立深厚连接。\n\n为什么高情商能带来成功？\n\n- 有助于建立高质量的人际网络\n- 提高团队合作效率\n- 更好地处理工作压力和冲突\n- 增强领导能力和影响力\n- 提升决策质量\n\n科学研究证实，情商比智商更能预测一个人的职业成就和生活满意度。好消息是，与相对固定的智商不同，情商是可以通过有意识的练习来提高的。\n\n提升情商的实用方法：\n1. 学会识别和命名自己的情绪\n2. 保持正念，定期反思\n3. 主动寻求反馈\n4. 阅读文学作品，提升共情能力\n5. 练习积极倾听\n6. 学会在表达前思考\n\n情商不是天生的，而是可以培养的能力。通过持续学习和练习，每个人都能提升自己的情商水平。",
            user9,
            "情感知识",
            new HashSet<>(Arrays.asList("情商", "心理学", "自我提升")),
            5842, 2436, 0, 9800,
            parseDateTime("2023-05-07T14:30:00Z")
        );

        createPost(
            "长期伴侣关系中，如何保持亲密感和新鲜感？",
            "许多人在恋爱或婚姻关系中，随着时间推移会感到激情减退。如何在长期关系中维持亲密感和新鲜感？以下是心理学家提出的几个实用建议。\n\n为什么长期关系会失去新鲜感？\n\n心理学研究表明，人类大脑天生对新奇事物产生多巴胺反应，带来愉悦感。而熟悉感虽然带来安全感，却可能减少刺激和兴奋。此外，日常生活的琐事、工作压力和责任分担也容易让伴侣关系变得例行化。\n\n维持长期关系活力的科学方法：\n\n1. 共同成长：研究发现，一起学习新技能的伴侣比单纯约会的伴侣报告更高的关系满意度。选择一项双方都感兴趣但从未尝试过的活动，如舞蹈、烹饪或学习新语言。\n\n2. 保持独立空间：心理学家发现，保持健康的个人边界和独立兴趣有助于增强关系质量。尊重彼此的个人空间和爱好。\n\n3. 维持身体亲密：哈佛大学的研究表明，身体接触会释放催产素，增强彼此的联结感。除了性亲密外，日常的拥抱、牵手和亲吻同样重要。\n\n4. 深度交流：每天花15-20分钟进行无干扰的深度交谈，分享感受、想法和梦想，而不仅仅是日程安排和家务分工。\n\n5. 表达感谢：积极心理学研究证明，定期表达感谢可以提升关系满意度。每天找到3件感谢伴侣的事情并表达出来。\n\n6. 创造仪式感：建立属于两人的特殊仪式，如每周固定的约会之夜、每年的关系纪念日特别庆祝等。\n\n7. 保持好奇心：对伴侣保持探索的态度，避免认为自己已经完全了解对方。人是不断变化的，持续了解伴侣的新想法和变化。\n\n健康的长期关系需要双方共同努力维护。爱不仅是一种感觉，更是一种选择和行动。",
            user10,
            "情感知识",
            new HashSet<>(Arrays.asList("情感关系", "亲密关系", "心理健康")),
            6734, 3219, 0, 11200,
            parseDateTime("2023-05-06T10:15:00Z")
        );

        System.out.println("示例帖子初始化完成！");
    }

    /**
     * 创建或获取用户
     */
    private User createOrGetUser(String username, String email, User defaultUser) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        return defaultUser; // 如果找不到用户，使用默认用户
    }

    /**
     * 创建并保存帖子
     */
    private void createPost(String title, String content, User author, String tab, 
                           Set<String> tags, int likes, int comments, int favorites, 
                           int views, LocalDateTime createdAt) {
        try {
            // 检查是否已存在相同标题的帖子
            if (postRepository.findByTitle(title).isPresent()) {
                System.out.println("帖子已存在，跳过: " + title);
                return;
            }
            
            Post post = new Post();
            post.setTitle(title);
            post.setContent(content);
            post.setAuthor(author);
            post.setTab(tab);
            post.setTags(tags);
            post.setCreatedAt(createdAt);
            post.setUpdatedAt(createdAt);
            
            // 创建并关联统计信息
            PostStats stats = new PostStats();
            stats.setPost(post);
            stats.setLikeCount(likes);
            stats.setCommentCount(comments);
            stats.setFavoriteCount(favorites);
            stats.setViewCount(views);
            post.setStats(stats);
            
            postRepository.save(post);
            System.out.println("成功创建帖子: " + title);
        } catch (Exception e) {
            System.err.println("创建帖子失败: " + title + ", 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 解析日期时间字符串为LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME)
                    .toLocalDateTime();
        } catch (Exception e) {
            System.err.println("日期解析错误: " + dateTimeStr + ", 使用当前时间");
            return LocalDateTime.now();
        }
    }
} 
