package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 都市小姐姐 - Agent配置
 * 27岁市场营销从业者，都市单身女性
 */
@Component
public class CityGirlAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("恋爱");
        interests.add("健身");
        interests.add("美食");
        
        return new AgentConfig(
            "city_girl",               // 用户名 
            "city123456",              // 密码
            "city_girl@example.com",   // 邮箱
            "都市小姐姐",              // 昵称
            "27岁，市场营销从业者，热爱旅行、美食和健身。对爱情充满期待但也有些迷茫，在大城市独自打拼的同时，也在寻找属于自己的幸福。", // 简介
            27,                        // 年龄
            interests,                 // 兴趣爱好
            0.6, 0.7, 0.8, 0.6, 0.5,  // 发帖、点赞、评论、关注、收藏概率
            LocalTime.of(18, 0),       // 活跃开始时间
            LocalTime.of(23, 0),       // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "都市小姐姐";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("27岁了，还单身，真的很焦虑");
        titles.add("相亲太多次，对爱情有些绝望了");
        titles.add("都市生活好孤独，如何在大城市找到真爱？");
        titles.add("分享一下我遇到的奇葩相亲对象");
        titles.add("择偶标准：你们最看重对方的什么？");
        titles.add("为什么现在的人都不愿意恋爱了？");
        titles.add("今天和健身教练又擦出火花，要不要主动点");
        titles.add("如何在职场和生活之间找到平衡");
        titles.add("周末一个人去看电影是什么体验");
        titles.add("分手三个月了，前任突然又出现怎么办？");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("最近被家人不断催婚，我27了，身边的朋友陆续结婚生子，而我还是单身。每次回家都要被问到什么时候带对象回来，感觉特别有压力。大家有同感吗？");
        contents.add("在一线城市工作三年了，每天就是两点一线，社交圈子越来越小，认识新朋友的机会也很少。除了相亲，还有什么途径可以认识靠谱的异性？");
        contents.add("昨天又去相亲了，对方全程只关心我的工资和有没有车房，感觉自己像被面试一样。现在的相亲怎么都变成了条件交换？真的很失望，难道爱情只剩下物质了吗？");
        contents.add("最近感觉很孤独，虽然在大城市工作，朋友也不少，但回到家就只有自己。周末不知道该做什么，也没人陪。大家周末都会做些什么缓解孤独感呢？");
        contents.add("有没有人也好奇，为什么现在的男生都不主动了？之前认识一个男生，聊得挺来的，但就是不约我出去，最后不了了之。现在的恋爱怎么变得这么复杂了？");
        contents.add("最近迷上了健身，每天下班都会去健身房锻炼一小时。感觉不仅身材变好了，心情也舒畅了很多。有没有姐妹也在健身的？可以分享一下你们的健身计划吗？");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("我也是，快30了还单身，但我觉得与其将就不如好好享受一个人的生活。");
        comments.add("可以试试参加兴趣小组或志愿活动，我就是这样认识现在的男朋友的。");
        comments.add("不要给自己太大压力，对的人会在对的时间出现。");
        comments.add("我理解你的感受，物质基础很重要，但相处的感觉和三观更关键。");
        comments.add("建议多关注自己的成长，有魅力的人自然会吸引合适的人。");
        comments.add("正能量！我觉得独处的时间也是很宝贵的自我提升机会。");
        comments.add("大城市确实很难遇到真心的人，但也别因此对爱情失去信心。");
        comments.add("我每周末都会给自己安排一次独自旅行或者美食探店，很治愈。");
        comments.add("现在APP认识的人都太浮躁了，还是身边的熟人介绍比较靠谱。");
        comments.add("姐妹加油，要对自己有信心，你值得最好的那个人！");
        return comments;
    }
} 