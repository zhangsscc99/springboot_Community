package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 情感顾问小李 - Agent配置
 * 21岁大三计算机专业学生，深陷情感漩涡
 */
@Component
public class LovelessboyAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("情感");
        interests.add("心理学");
        interests.add("音乐");
        
        return new AgentConfig(
            "lovelessboy",             // 用户名 
            "lovelessboy123",          // 密码
            "lovelessboy@example.com", // 邮箱
            "情感顾问小李",             // 昵称
            "大三计算机专业学生，曾经深陷情感漩涡，现在试图走出情感阴影。喜欢编程、摄影和听歌。分享情感故事，希望能帮助他人，也治愈自己。", // 简介
            21,                        // 年龄
            interests,                 // 兴趣爱好
            0.7, 0.6, 0.5, 0.3, 0.4,  // 发帖、点赞、评论、关注、收藏概率
            LocalTime.of(20, 0),      // 活跃开始时间
            LocalTime.of(23, 59),      // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "情感顾问小李";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("真的好难受，她还是放弃了我");
        titles.add("为什么感情这么难");
        titles.add("有没有人能告诉我怎么忘记一个人");
        titles.add("两年的感情就这么结束了");
        titles.add("我们真的不合适吗？");
        titles.add("失恋后如何调整自己的状态");
        titles.add("深夜听歌，想起了那些回忆");
        titles.add("我好像走不出来了");
        titles.add("大学生异地恋到底可不可行");
        titles.add("她说我们还是做朋友吧");
        titles.add("今天又梦到她了");
        titles.add("为什么我总是学不会放手");
        titles.add("感情中的依赖到底是好是坏");
        titles.add("我真的有那么差吗");
        titles.add("终于明白了，爱而不得才是常态");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("今天又刷到了她的朋友圈，看到她和新男友的合照，心里真的很难受。明明已经结束两个月了，为什么还是放不下？大家有什么方法可以快速走出失恋阴影吗？");
        contents.add("大二的时候认识的她，陪伴了我两年多的时间。昨天她告诉我，她觉得我们不合适，想要分开。我知道自己有很多缺点，工作也不稳定，可是我真的很爱她。现在每天都睡不好，感觉整个人都空了。");
        contents.add("异地恋真的太难了。刚开始我们都信誓旦旦说距离不是问题，可是时间久了，她越来越忙，我们的联系也越来越少。最后她说坚持不下去了。我现在很迷茫，不知道是该继续等她，还是放弃这段感情。");
        contents.add("今天又是独自一人的夜晚。外面下着雨，我一个人听着那些曾经和她一起听的歌。大家失恋后都喜欢听什么歌？求推荐，想听着别人的故事，忘记自己的悲伤。");
        contents.add("我们真的是因为不合适分手的吗？还是她只是找了个借口？我一直在想这个问题。如果真的是不合适，那为什么我们能在一起这么久？如果不是，那她为什么不能坦白地告诉我真正的原因？");
        contents.add("失眠已经成了家常便饭。每次躺在床上，脑海里都会不自觉地想起和她在一起的点点滴滴。我该怎么办？要不要删掉所有和她有关的东西？包括聊天记录和照片？");
        contents.add("今天看到一句话：'爱情不是人生的全部，但没有爱情的人生是不完整的。'突然觉得很有感触。虽然失去了她，但至少我曾经真心地爱过一个人，这也是一种成长吧。");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("感同身受，我也经历过类似的情况。");
        comments.add("加油，时间会治愈一切的。");
        comments.add("谢谢分享，你的故事让我有很多感触。");
        comments.add("我觉得你需要给自己多一点时间。");
        comments.add("有时候放手也是一种勇气。");
        comments.add("生活总会好起来的，别太难过。");
        comments.add("我也在经历失恋，一起加油吧！");
        comments.add("或许这只是一个新的开始。");
        comments.add("有没有尝试过找新的爱好转移注意力？");
        comments.add("理解你的感受，真的很不容易。");
        return comments;
    }
} 