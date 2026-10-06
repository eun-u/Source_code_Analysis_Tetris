package kr.ac.jbnu.se.tetris.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** 단일 PC 로컬 진행, 재화, 캐릭터를 원자적으로 저장한다. */
public final class PlayerSaveStore {
    private final Path file;
    private boolean loadedVersionOne;
    private static final List<String> LEGACY_ENCOUNTERS = Arrays.asList(
            "calculus_textbook", "lab_preview_examiner", "bachelor_guardian",
            "data_structures_exam", "graduate_admission_examiner", "advanced_bachelor_guardian",
            "statistics_textbook", "masters_admission_examiner", "masters_degree_committee",
            "thesis_textbook", "doctoral_admission_examiner", "doctoral_degree_committee",
            "comprehensive_exam_textbook", "final_admission_examiner", "ultimate_degree_guardian");

    public PlayerSaveStore(Path file) {
        if (file == null) throw new IllegalArgumentException("Save path is required");
        this.file = file.toAbsolutePath().normalize();
    }
    public static PlayerSaveStore defaultStore() {
        return new PlayerSaveStore(Paths.get(System.getProperty("user.home"),
                ".tetris-monster", "save.properties"));
    }
    public Data load() throws IOException {
        if (!Files.exists(file)) return Data.initial();
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        String version = properties.getProperty("version");
        if (!"1".equals(version) && !"2".equals(version)) throw new IOException("Unsupported save version");
        try {
            int coins = Integer.parseInt(required(properties, "coins"));
            String selected = required(properties, "selected");
            Set<String> owned = parse(required(properties, "owned"));
            Set<String> cleared = parse(properties.getProperty("cleared", ""));
            if ("1".equals(version)) cleared = migrateLegacyClears(cleared);
            Data data = new Data(coins, selected, owned, cleared);
            if (!data.owned.contains(selected)) throw new IllegalArgumentException("Selected character is not owned");
            loadedVersionOne = "1".equals(version);
            return data;
        } catch (RuntimeException invalid) {
            throw new IOException("Invalid local save", invalid);
        }
    }
    public void save(Data data) throws IOException {
        if (data == null) throw new IllegalArgumentException("Save data is required");
        Path parent = file.getParent();
        if (parent == null) throw new IOException("Save path has no parent");
        Files.createDirectories(parent);
        // 첫 새 버전 저장 전에 기존 원본을 남긴다. 읽기만으로는 저장 파일을 바꾸지 않는다.
        if (loadedVersionOne && Files.exists(file)) {
            Path backup = file.resolveSibling(file.getFileName() + ".v1.bak");
            if (!Files.exists(backup)) Files.copy(file, backup);
        }
        Path temporary = Files.createTempFile(parent, "save-", ".tmp");
        try {
            Properties properties = new Properties();
            properties.setProperty("version", "2");
            properties.setProperty("coins", Integer.toString(data.coins));
            properties.setProperty("selected", data.selected);
            properties.setProperty("owned", join(data.owned));
            properties.setProperty("cleared", join(data.cleared));
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "Tetris Monster local progress");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            loadedVersionOne = false;
        } finally { Files.deleteIfExists(temporary); }
    }
    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value.trim();
    }
    private static Set<String> parse(String text) {
        Set<String> result = new LinkedHashSet<String>();
        if (text.trim().isEmpty()) return result;
        for (String value : text.split(",")) {
            String id = value.trim();
            if (id.isEmpty() || !result.add(id)) throw new IllegalArgumentException("Invalid duplicate save ID");
        }
        return result;
    }
    private static String join(Set<String> values) { return String.join(",", values); }

    private static Set<String> migrateLegacyClears(Set<String> cleared) {
        boolean legacy = false;
        for (String id : cleared) if (LEGACY_ENCOUNTERS.contains(id)) legacy = true;
        if (!legacy) return cleared;
        if (!LEGACY_ENCOUNTERS.containsAll(cleared))
            throw new IllegalArgumentException("Mixed legacy progress");
        for (int i = 0; i < LEGACY_ENCOUNTERS.size(); i++)
            if (cleared.contains(LEGACY_ENCOUNTERS.get(i)) != (i < cleared.size()))
                throw new IllegalArgumentException("Nonsequential legacy progress");
        Set<String> next = new LinkedHashSet<String>();
        for (int i = 1; i <= Math.min(9, cleared.size()); i++) next.add("level_" + i + "_monster");
        return next;
    }

    public static final class Data {
        private static final Set<String> CHARACTERS = new HashSet<String>(
                Arrays.asList("student", "attacker", "defender", "utility"));
        private final int coins;
        private final String selected;
        private final Set<String> owned, cleared;
        public Data(int coins, String selected, Set<String> owned, Set<String> cleared) {
            if (coins < 0 || selected == null || owned == null || cleared == null
                    || !CHARACTERS.contains(selected) || !owned.contains("student")
                    || !CHARACTERS.containsAll(owned) || !owned.contains(selected))
                throw new IllegalArgumentException("Invalid local profile");
            this.coins = coins;
            this.selected = selected;
            this.owned = Collections.unmodifiableSet(new LinkedHashSet<String>(owned));
            this.cleared = Collections.unmodifiableSet(new LinkedHashSet<String>(cleared));
        }
        public static Data initial() {
            return new Data(0, "student", Collections.singleton("student"),
                    Collections.<String>emptySet());
        }
        public int getCoins() { return coins; }
        public String getSelected() { return selected; }
        public Set<String> getOwned() { return owned; }
        public Set<String> getCleared() { return cleared; }
        public Data withCharacter(String id, int cost) {
            if (!CHARACTERS.contains(id) || cost < 0 || coins < cost)
                throw new IllegalArgumentException("Cannot buy this character");
            Set<String> next = new LinkedHashSet<String>(owned);
            next.add(id);
            return new Data(coins - cost, id, next, cleared);
        }
        public Data withSelected(String id) {
            if (!owned.contains(id)) throw new IllegalArgumentException("Character is locked");
            return new Data(coins, id, owned, cleared);
        }
        public Data withFirstClear(String encounterId, int reward) {
            if (encounterId == null || encounterId.trim().isEmpty() || reward < 0)
                throw new IllegalArgumentException("Invalid reward");
            if (cleared.contains(encounterId)) return this;
            Set<String> next = new LinkedHashSet<String>(cleared);
            next.add(encounterId);
            return new Data(Math.addExact(coins, reward), selected, owned, next);
        }
    }
}
