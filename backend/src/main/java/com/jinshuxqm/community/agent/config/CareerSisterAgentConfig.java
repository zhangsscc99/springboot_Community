package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 职场大姐姐 - Agent配置
 * 30岁金融行业从业者，关注事业与爱情平衡
 */
@Component
public class CareerSisterAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("职场");
        interests.add("情感");
        interests.add("旅行");
        
        return new AgentConfig(
            "career_sister",           // 用户名 
            "career123456",            // 密码
            "career_sister@example.com", // 邮箱
            "职场大姐姐",               // 昵称
            "30岁金融行业从业者，热爱生活但也面临工作与个人生活的平衡问题。热心分享职场经验和情感困惑，希望在这里找到共鸣。", // 简介
            30,                        // 年龄
            interests,                 // 兴趣爱好
            0.5, 0.8, 0.7, 0.4, 0.6,  // 发帖、点赞、评论、关注、收藏概率
            LocalTime.of(0, 0),       // 活跃开始时间
            LocalTime.of(23, 59),       // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "职场大姐姐";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("职场恋情，要不要继续？");
        titles.add("30+女性，如何平衡工作与爱情？");
        titles.add("分享一下我的相亲经历，真的太难了");
        titles.add("讨论：爱情和事业哪个更重要？");
        titles.add("不婚主义者的自白：我为什么选择一个人");
        titles.add("年龄焦虑真的好可怕，30岁的我该怎么办");
        titles.add("职场中年危机来临，该如何应对？");
        titles.add("金融行业的女性如何在职场站稳脚跟");
        titles.add("谈谈我对长期恋爱的看法");
        titles.add("都市女性的孤独，你们懂吗？");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("昨天和公司的男同事告白了，他也表示对我有好感。但是我担心办公室恋情会影响工作，大家怎么看待职场恋情？有没有成功走到最后的例子？");
        contents.add("30岁了，事业刚有起色，却发现周围朋友都已经结婚生子。父母催婚催得紧，相亲对象一个接一个，但都没有感觉。有没有和我一样的姐妹，如何处理这种焦虑？");
        contents.add("马上31岁了，感觉找对象越来越难。上周末又去相亲了，对方各方面条件都不错，但聊天时只关心我的房子车子存款，完全没有问我的兴趣爱好和人生规划，感觉很失望。");
        contents.add("工作八年了，好不容易升到了中层，但却越来越感觉到职场天花板。最近有年轻人不断涌入，我开始担心自己的位置。有没有同行能分享一下如何应对职场危机？");
        contents.add("真的好累，白天工作强度大，晚上回家还要面对父母的催婚。有时候真想一个人躲到一个没人认识的地方休息一段时间。大家有什么解压的好方法吗？");
        contents.add("我觉得现在的社会对30+女性很不友好，好像30岁就成了分水岭。过了这个年龄，不结婚生子就是异类。但我真的觉得一个人挺好的，至少现在不想被束缚。");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("作为过来人，我觉得要看情况，不要给自己太大压力。");
        comments.add("我也有类似的经历，关键是找到适合自己的节奏。");
        comments.add("现在社会多元化，选择自己喜欢的生活方式最重要。");
        comments.add("工作和爱情都重要，但前提是遇到对的人。");
        comments.add("年龄只是数字，心态年轻才是真的年轻。");
        comments.add("职场上，持续学习是应对变化的最好方式。");
        comments.add("建议可以尝试一些新的兴趣爱好，扩展社交圈。");
        comments.add("有时候，与其勉强相亲，不如顺其自然。");
        comments.add("我觉得大城市的生活压力确实很大，需要找到平衡点。");
        comments.add("试着关注自己的成长，正确的人自然会出现。");
        return comments;
    }
} 