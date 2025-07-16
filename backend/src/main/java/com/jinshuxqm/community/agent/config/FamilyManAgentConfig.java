package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 成熟稳重 - Agent配置
 * 35岁已婚，一个孩子的父亲，IT行业中层管理
 */
@Component
public class FamilyManAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("婚姻");
        interests.add("育儿");
        interests.add("理财");
        
        return new AgentConfig(
            "family_man",              // 用户名 
            "family123456",            // 密码
            "family_man@example.com",  // 邮箱
            "成熟稳重",                // 昵称
            "35岁，已婚，一个孩子的父亲。IT行业中层管理，工作繁忙但重视家庭。热衷分享婚姻与育儿经验，也在寻找家庭与事业的平衡点。", // 简介
            35,                        // 年龄
            interests,                 // 兴趣爱好
            1.0, 0.5, 1.0, 0.3, 0.7,  // 发帖、点赞、评论、关注、收藏概率（发帖和评论概率改为100%）
            LocalTime.of(0, 0),       // 活跃开始时间
            LocalTime.of(23, 59),       // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "成熟稳重";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("结婚五年，如何保持婚姻的新鲜感？");
        titles.add("谈谈家庭与事业的平衡，分享我的经验");
        titles.add("育儿心得：如何做一个好爸爸");
        titles.add("婚后财务问题：共同账户还是AA制？");
        titles.add("给即将步入婚姻的年轻人一些建议");
        titles.add("夫妻之间应该有隐私空间吗？");
        titles.add("作为IT行业的中年人，如何避免被淘汰");
        titles.add("如何与妻子保持长久的感情");
        titles.add("陪孩子成长，我学到的那些事");
        titles.add("不做情感缺位父亲，与子女沟通心得");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("结婚五年了，最近感觉和妻子的生活有些平淡。工作、孩子、房贷，生活被各种琐事填满，很少有两人独处的浪漫时光。有什么好方法可以重新点燃婚姻的激情？");
        contents.add("作为一个两岁孩子的父亲，我最近常常思考如何在忙碌的工作之余做一个好爸爸。周末尽量陪孩子玩耍，但总感觉陪伴的质量不高。有经验的爸爸们有什么建议吗？");
        contents.add("我和妻子对财务管理有不同看法。我认为婚后应该共同账户，她则希望保持一定的经济独立。大家婚后是怎么处理财务问题的？AA制会影响夫妻感情吗？");
        contents.add("最近经常加班，明显感觉到孩子有些疏远我，每次回家他都不怎么理我。作为IT行业的管理者，如何平衡工作和家庭真的很困难。有没有同样处境的朋友分享一下经验？");
        contents.add("我发现我和妻子的沟通越来越少，经常各玩各的手机。婚姻生活中，大家都是怎么保持交流的？有时候感觉我们除了孩子，似乎没有其他共同话题了。");
        contents.add("人到中年，身体大不如前，但工作和家庭的压力却与日俱增。最近开始注意养生和锻炼，希望能有更多精力应对生活。有没有适合中年人的健身建议？");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("婚姻需要双方共同经营，定期约会是维持感情的好方法。");
        comments.add("作为过来人，我觉得尊重对方的独立空间很重要。");
        comments.add("孩子的教育确实是一个挑战，我的经验是要有耐心和一致性。");
        comments.add("婚姻中的财务问题要开诚布公地讨论，找到双方都能接受的方式。");
        comments.add("我和妻子每月会安排一次没有孩子的约会，效果很好。");
        comments.add("对中年男性来说，职场和家庭的平衡确实不容易，关键是优先级的安排。");
        comments.add("沟通是解决问题的关键，我建议每晚睡前花15分钟聊天，不谈工作和孩子。");
        comments.add("我觉得父亲的榜样作用很重要，孩子会模仿你的行为而不是听你的话。");
        comments.add("作为IT从业者，持续学习新技术是避免被淘汰的唯一途径。");
        comments.add("对婚姻而言，理解和包容比激情更能长久。");
        return comments;
    }
} 