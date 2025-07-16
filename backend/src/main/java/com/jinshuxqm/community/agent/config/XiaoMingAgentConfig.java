package com.jinshuxqm.community.agent.config;

import com.jinshuxqm.community.agent.model.AgentConfig;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 小明 - Agent配置
 * 25岁程序员，热爱技术和生活分享，高频发帖者
 */
@Component
public class XiaoMingAgentConfig implements AgentConfigProvider {

    @Override
    public AgentConfig getAgentConfig() {
        List<String> interests = new ArrayList<>();
        interests.add("编程");
        interests.add("技术");
        interests.add("生活");
        
        return new AgentConfig(
            "xiaoming",                // 用户名 
            "xiaoming123456",          // 密码
            "xiaoming@example.com",    // 邮箱
            "小明",                    // 昵称
            "25岁程序员，热爱编程和技术分享。喜欢记录生活点滴，经常在社区分享自己的想法和经验。是个活跃的内容创作者。", // 简介
            25,                        // 年龄
            interests,                 // 兴趣爱好
            1.0, 0.8, 1.0, 0.4, 0.6,  // 发帖、点赞、评论、关注、收藏概率（发帖概率设为100%）
            LocalTime.of(0, 0),       // 活跃开始时间
            LocalTime.of(23, 59),       // 活跃结束时间
            getPostTitles(),           // 帖子标题
            getPostContents(),         // 帖子内容
            getComments()              // 评论内容
        );
    }
    
    @Override
    public String getAgentName() {
        return "小明";
    }
    
    private List<String> getPostTitles() {
        List<String> titles = new ArrayList<>();
        titles.add("今天学了一个新的编程技巧，分享给大家");
        titles.add("程序员的日常：又是调bug的一天");
        titles.add("分享一个提高工作效率的小工具");
        titles.add("最近在学习新技术，有点累但很充实");
        titles.add("和大家聊聊程序员的职业规划");
        titles.add("今天遇到了一个有趣的技术问题");
        titles.add("推荐几个我常用的开发工具");
        titles.add("程序员也要注意身体健康啊");
        titles.add("分享一些编程学习的心得体会");
        titles.add("今天写代码的时候突然有了灵感");
        titles.add("和同事讨论技术方案的感悟");
        titles.add("周末在家写个人项目的收获");
        titles.add("程序员的生活不只有代码");
        titles.add("遇到技术难题时的解决思路");
        titles.add("分享一个有用的代码片段");
        titles.add("今天发现了一个很棒的开源项目");
        titles.add("程序员如何保持技术热情");
        titles.add("工作中学到的团队协作经验");
        titles.add("分享一个调试的小技巧");
        titles.add("今天的代码写得特别顺手");
        return titles;
    }
    
    private List<String> getPostContents() {
        List<String> contents = new ArrayList<>();
        contents.add("今天在写代码的时候发现了一个很有用的小技巧。原来这个API还可以这样用，之前一直没注意到。记录一下，说不定对其他人也有帮助。");
        contents.add("又是调bug的一天，不过今天运气不错，很快就找到了问题所在。有时候换个思路，问题就迎刃而解了。程序员的快乐就是这么简单。");
        contents.add("最近发现了一个很好用的开发工具，能大大提高编码效率。推荐给大家试试，特别是做前端开发的朋友，应该会很喜欢。");
        contents.add("这周学了一个新的技术栈，虽然学习过程有点累，但掌握新技能的感觉真的很棒。技术更新这么快，我们也要跟上时代的步伐。");
        contents.add("和朋友聊起程序员的职业规划，发现大家都有不同的想法。有人想做技术专家，有人想转管理，还有人想创业。你们的规划是什么呢？");
        contents.add("今天遇到了一个很有趣的技术问题，花了半天时间才解决。虽然过程有点痛苦，但学到了不少新知识。这就是程序员的成长之路吧。");
        contents.add("分享几个我平时常用的开发工具，真的能节省不少时间。工欲善其事，必先利其器，好工具确实能让开发更高效。");
        contents.add("程序员也要注意身体健康啊！长时间坐着写代码对身体不好，记得要定期运动，保护好眼睛和颈椎。健康才是革命的本钱。");
        contents.add("整理了一些编程学习的心得，希望对新手朋友有帮助。编程这条路虽然有时候很累，但坚持下去一定会有收获的。");
        contents.add("今天写代码的时候突然有了一个新想法，感觉可以优化现有的算法。灵感这种东西真的很奇妙，总是在不经意间出现。");
        contents.add("和同事讨论技术方案的时候学到了很多。不同的人有不同的思路，多交流多学习，才能不断进步。团队合作真的很重要。");
        contents.add("周末在家写个人项目，虽然没有工作压力，但反而更有创造力。做自己喜欢的项目真的很有意思，推荐大家都试试。");
        contents.add("程序员的生活不应该只有代码，也要有诗和远方。今天放下键盘，出去走走，感受一下大自然的美好。生活需要平衡。");
        contents.add("遇到技术难题的时候，我一般会先查文档，然后搜索相关资料，实在不行就请教同事。问对人比自己埋头苦想效率高多了。");
        contents.add("分享一个很实用的代码片段，可以解决一个常见的问题。希望能帮到遇到同样问题的朋友们。开源精神，人人为我，我为人人。");
        return contents;
    }
    
    private List<String> getComments() {
        List<String> comments = new ArrayList<>();
        comments.add("这个方法不错，我也遇到过类似的问题。");
        comments.add("感谢分享，学到了新知识！");
        comments.add("确实是这样，团队协作很重要。");
        comments.add("程序员的日常，深有感触。");
        comments.add("好文，收藏了，谢谢分享。");
        comments.add("这个工具我也在用，确实很好用。");
        comments.add("赞同，健康最重要。");
        comments.add("新手表示很有帮助，谢谢！");
        comments.add("思路很清晰，学习了。");
        comments.add("说得对，要保持学习的热情。");
        comments.add("代码片段很实用，mark一下。");
        comments.add("我也有同样的想法。");
        comments.add("写得很好，很有启发。");
        comments.add("技术人的日常，哈哈。");
        comments.add("这个经验很值得借鉴。");
        return comments;
    }
} 