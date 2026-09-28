package kr.ac.jbnu.se.tetris.story;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 일반·엘리트·보스 순서로 진행하는 한 장의 불변 데이터 */
public final class Stage {
    private final String id;
    private final String name;
    private final List<MonsterSpec> encounters;

    public Stage(String id, String name, List<MonsterSpec> encounters) {
        if (id == null || id.trim().isEmpty() || name == null || name.trim().isEmpty()
                || encounters == null || encounters.size() != MonsterTier.values().length) {
            throw new IllegalArgumentException("Stage needs an ID, name, and three encounters");
        }
        Set<String> ids = new HashSet<String>();
        for (int index = 0; index < encounters.size(); index++) {
            MonsterSpec monster = encounters.get(index);
            if (monster == null || monster.getTier() != MonsterTier.values()[index]
                    || !ids.add(monster.getId())) {
                throw new IllegalArgumentException("Stage encounters must be unique NORMAL, ELITE, BOSS");
            }
        }
        this.id = id;
        this.name = name;
        this.encounters = Collections.unmodifiableList(new ArrayList<MonsterSpec>(encounters));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<MonsterSpec> getEncounters() { return encounters; }
}
