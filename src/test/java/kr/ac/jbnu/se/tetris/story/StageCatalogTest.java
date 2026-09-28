package kr.ac.jbnu.se.tetris.story;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 기본 캠페인 순서·AI 프로필 참조·설정 오류 검증 */
public final class StageCatalogTest {
    public static void main(String[] args) throws Exception {
        defaultCampaignIsCompleteAndImmutable();
        malformedConfigurationIsRejected();
    }

    private static void defaultCampaignIsCompleteAndImmutable() {
        StageCatalog catalog = StageCatalog.loadDefault();
        List<Stage> stages = catalog.getStages();
        check(stages.size() == 5, "기본 캠페인은 다섯 장이다");
        Set<String> monsterIds = new HashSet<String>();
        int previousNormalHp = 0;
        for (Stage stage : stages) {
            check(stage.getEncounters().size() == 3, "장마다 세 전투가 있다");
            for (int index = 0; index < 3; index++) {
                MonsterSpec monster = stage.getEncounters().get(index);
                check(monster.getTier() == MonsterTier.values()[index], "일반, 엘리트, 보스 순서");
                check(monsterIds.add(monster.getId()), "몬스터 ID는 중복되지 않는다");
                check(kr.ac.jbnu.se.tetris.ai.AIProfileCatalog.loadDefault()
                        .get(monster.getAiProfileId()).getMaxSearchStates() > 0, "프로필의 탐색 상한 확인");
                check(monster.getAiProfileId().equals(index == 0 ? "normal_default"
                        : index == 1 ? "elite_default" : "boss_default"),
                        "몬스터가 AI 프로필 ID를 참조한다");
            }
            int normalHp = stage.getEncounters().get(0).getHp();
            check(normalHp > previousNormalHp, "다음 장의 일반 몬스터 HP가 증가한다");
            previousNormalHp = normalHp;
        }
        check(monsterIds.size() == 15, "15개 대전이 있다");
        check(catalog.getStage("research_lab") == stages.get(2), "ID로 장을 찾는다");
        check(catalog.getStage("missing") == null, "없는 ID는 null이다");
        expectUnsupported(() -> stages.clear());
        expectUnsupported(() -> stages.get(0).getEncounters().clear());
    }

    private static void malformedConfigurationIsRejected() throws Exception {
        String valid = defaultSettings();
        expectInvalid(() -> load(valid.replace("stage.count=5", "stage.count=0")));
        expectInvalid(() -> load(valid.replace("stage.1.boss.aiProfile=boss_default",
                "stage.1.boss.aiProfile=")));
        expectInvalid(() -> load(valid.replace("stage.1.normal.hp=65", "stage.1.normal.hp=-1")));
        expectInvalid(() -> load(valid.replace("stage.1.normal.id=calculus_textbook",
                "stage.1.normal.id=graduate_admission_examiner")));
        expectInvalid(() -> load(valid + "\nstage.1.normal.hpTypo=65\n"));
        expectInvalid(() -> load(valid + "\nai.boss.strategy=MIRROR\n"));
        expectInvalid(() -> load(valid + "\nstage.count=5\n"));
        expectInvalid(() -> load(valid + "\nstage.1.normal.delayMillis=1000\n"));
        expectInvalid(() -> load(valid.replace("stage.1.boss.name=학사 학위 심사위원", "")));
    }

    private static StageCatalog load(String contents) {
        try {
            return StageCatalog.load(new StringReader(contents));
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static String defaultSettings() throws IOException {
        InputStream stream = StageCatalogTest.class.getResourceAsStream("/story/stages.properties");
        if (stream == null) throw new AssertionError("기본 설정 파일이 없다");
        try (InputStream input = stream; ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            return new String(bytes.toByteArray(), "UTF-8");
        }
    }

    private static void expectInvalid(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("설정 오류를 거부해야 한다");
    }

    private static void expectUnsupported(Runnable action) {
        try { action.run(); }
        catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError("불변 목록을 수정할 수 없어야 한다");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
