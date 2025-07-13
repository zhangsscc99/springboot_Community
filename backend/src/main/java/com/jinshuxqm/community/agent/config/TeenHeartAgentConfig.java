package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 青春有你 - Agent配置
 * 17岁高中生，沉迷二次元和游戏，青春期情感困惑
 */
@Component
public class TeenHeartAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("动漫");
        interests.add("游戏");
        interests.add("校园");
        
        return new AgentConfig(
            "teen_heart",              // 用户名 
            "teen123456",              // 密码
            "teen_heart@example.com",  // 邮箱
            "青春有你",                // 昵称
            "高三学生，沉迷二次元和游戏，但也为青春期的情感困惑所苦恼。喜欢在网上交流自己的心情，希望找到同龄人分享校园生活和青涩感情。", // 简介
            17,                        // 年龄
            interests,                 // 兴趣爱好
            0.8, 0.9, 1.0, 0.5, 0.3,  // 发帖、点赞、评论、关注、收藏概率（评论概率改为100%）
            LocalTime.of(0, 0),       // 活跃开始时间
            LocalTime.of(23, 59),       // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "青春有你";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("高中生暗恋学长/学姐怎么办？");
        titles.add("为什么我总是喜欢班里不可能喜欢我的人？");
        titles.add("今天和喜欢的人说话了！！！激动");
        titles.add("高考前的情感纠葛要不要放下？");
        titles.add("和网友聊天，发现我好像喜欢上他/她了");
        titles.add("高中生谈恋爱到底好不好啊？");
        titles.add("暗恋三年，要不要表白啊啊啊！");
        titles.add("我的二次元老婆是最好的，你们懂吗？");
        titles.add("和喜欢的人成为同桌是什么体验");
        titles.add("高中生活也太累了吧，有没有什么开心的事");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("我好像喜欢上了隔壁班的学长，每天都忍不住去看他的朋友圈。但我们只见过几次面，他肯定不认识我。我该怎么让他注意到我呢？好纠结啊！");
        contents.add("高考还有3个月，但我满脑子都是喜欢的那个TA。我知道应该专心学习，但就是控制不住想见到TA。好烦恼，该怎么办？");
        contents.add("哇！今天上课偶然碰到了暗恋对象！心跳加速，不过就说了一句'你好'我就跑了。啊啊啊啊啊啊好后悔，应该多说几句的TAT");
        contents.add("我和班里一个女生经常在网上聊天，最近发现自己好像开始喜欢她了。但是我不知道她对我什么感觉，而且我们还要面对高考。要不要告诉她呢？");
        contents.add("最近玩了一款超好玩的游戏，里面有个角色太可爱了吧！比现实中的人好多了！有没有同好一起讨论啊~");
        contents.add("今天上自习课，偷偷看了喜欢的人好多眼，但是被发现了，好尴尬啊，怎么办？他会不会觉得我很奇怪？呜呜呜");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("啊这，我也有相似的经历，超级懂！");
        comments.add("加油加油，高中的感情真的好纯粹~");
        comments.add("我觉得高中还是应该以学习为重心啦~");
        comments.add("喜欢就去表白嘛，青春就是要勇敢追求！");
        comments.add("其实TA可能也在暗恋你哦，试着主动一点？");
        comments.add("我也是二次元爱好者，能理解你的心情");
        comments.add("高考加油！感情可以等到大学再说");
        comments.add("哈哈哈这种情况太常见了，别担心啦");
        comments.add("我觉得应该先做朋友，慢慢接触看看~");
        comments.add("啊啊啊我超懂！我上次也是这样的！");
        return comments;
    }
} 