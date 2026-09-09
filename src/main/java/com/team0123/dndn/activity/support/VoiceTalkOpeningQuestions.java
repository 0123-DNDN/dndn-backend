package com.team0123.dndn.activity.support;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class VoiceTalkOpeningQuestions {

    private static final List<String> QUESTIONS = List.of(
            "오늘 기분은 어떠셨어요?",
            "오늘 가장 기억에 남는 일이 있었나요?",
            "오늘 맛있게 드신 음식이 있었나요?",
            "오늘 누구와 이야기하셨나요?",
            "오늘 밖에 다녀오셨다면 어디에 다녀오셨나요?",
            "오늘 아침에는 어떻게 시간을 보내셨어요?",
            "오늘 웃음이 났던 일이 있었나요?",
            "오늘 마음이 편안했던 순간이 있었나요?",
            "오늘 새롭게 알게 된 것이 있었나요?",
            "오늘 가장 고마웠던 일은 무엇인가요?",
            "어제 가장 기억에 남는 일은 무엇인가요?",
            "최근 가족과 이야기한 일이 있었나요?",
            "이번 주에 가장 즐거웠던 일은 무엇인가요?",
            "내일은 무엇을 하고 싶으세요?",
            "요즘 자주 생각나는 사람이 있나요?",
            "최근에 재미있게 본 방송이나 영상이 있나요?",
            "최근에 자주 듣는 노래가 있나요?",
            "요즘 즐겨 드시는 간식이 있나요?",
            "최근에 날씨가 좋다고 느낀 날이 있었나요?",
            "이번 주에 기다리고 있는 일이 있나요?",
            "요즘 집에서 가장 자주 하는 일은 무엇인가요?",
            "최근에 누군가에게 들은 반가운 소식이 있나요?",
            "요즘 즐겁게 하고 있는 취미가 있나요?",
            "최근에 산책하면서 본 것 중 기억나는 게 있나요?",
            "예전에 좋아했던 음식 중 요즘도 즐겨 드시는 게 있나요?",
            "가족과 함께했던 기억 중 최근에 떠오른 일이 있나요?",
            "어릴 때 좋아했던 놀이나 활동이 있었나요?",
            "예전에 자주 가셨던 곳 중 다시 가보고 싶은 곳이 있나요?",
            "오래전 친구 중 가끔 생각나는 분이 있나요?",
            "젊었을 때 즐겨 들었던 노래가 있나요?",
            "계절이 바뀌면 떠오르는 기억이 있나요?",
            "예전에 가족과 자주 해 드셨던 음식이 있나요?",
            "요즘 하루 중 가장 편안한 시간은 언제인가요?",
            "오늘 몸을 움직이거나 산책하신 일이 있었나요?",
            "오늘 주변에서 예쁘다고 느낀 것이 있었나요?",
            "이번 주에 가족이나 친구에게 전하고 싶은 말이 있나요?"
    );

    public String randomQuestion() {
        return QUESTIONS.get(
                ThreadLocalRandom.current().nextInt(QUESTIONS.size())
        );
    }

    public boolean contains(String question) {
        return QUESTIONS.contains(question);
    }

    public List<String> allQuestions() {
        return QUESTIONS;
    }
}
